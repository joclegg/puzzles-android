package com.joclegg.puzzles.wordwheel

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.BankStore
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.wordwheel.core.data.GameDatabase
import com.joclegg.puzzles.wordwheel.core.data.GameRepository
import com.joclegg.puzzles.wordwheel.core.data.PuzzleBank
import com.joclegg.puzzles.wordwheel.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.wordwheelSettingsStore by preferencesDataStore(name = "word_wheel_settings")

class WordWheelContainer(
    context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "word_wheel.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.wordwheelSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = wordWheelSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            BankStore(context).read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
