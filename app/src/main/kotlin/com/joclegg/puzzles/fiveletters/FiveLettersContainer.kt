package com.joclegg.puzzles.fiveletters

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.fiveletters.core.data.GameDatabase
import com.joclegg.puzzles.fiveletters.core.data.GameRepository
import com.joclegg.puzzles.fiveletters.core.data.NytWordSource
import com.joclegg.puzzles.fiveletters.core.data.SettingsRepository
import com.joclegg.puzzles.fiveletters.engine.Words
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

private val Context.fiveLettersSettingsStore by preferencesDataStore(name = "five_letters_settings")

class FiveLettersContainer(
    private val context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "five_letters.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.fiveLettersSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = fiveLettersSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
    val nytWords = NytWordSource()

    private val loading = Mutex()
    private var loaded: Words? = null

    suspend fun words(): Words =
        loading.withLock {
            loaded ?: withContext(Dispatchers.IO) { Words(read("words/answers.txt"), read("words/guesses.txt")) }.also { loaded = it }
        }

    private fun read(path: String): List<String> =
        context.assets
            .open(path)
            .bufferedReader()
            .use { it.readLines() }
}
