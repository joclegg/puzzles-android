package com.joclegg.puzzles.wordwheel

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
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
    private val database = Room.databaseBuilder(context, GameDatabase::class.java, "word_wheel.db").build()
    val games = GameRepository(database.gameDao())
    val settings = SettingsRepository(context.wordwheelSettingsStore)
    val bank =
        PuzzleBank { path ->
            context.assets
                .open(path)
                .bufferedReader()
                .use { it.readText() }
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
