package com.joclegg.puzzles.slideout

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.BankStore
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.slideout.core.data.GameDatabase
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.PuzzleBank
import com.joclegg.puzzles.slideout.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.slideOutSettingsStore by preferencesDataStore(name = "slide_out_settings")

class SlideOutContainer(
    context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "slide_out.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.slideOutSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = slideOutSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            BankStore(context).read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
