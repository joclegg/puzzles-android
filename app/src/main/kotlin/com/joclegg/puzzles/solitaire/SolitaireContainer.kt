package com.joclegg.puzzles.solitaire

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.solitaire.core.data.GameDatabase
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.solitaireSettingsStore by preferencesDataStore(name = "solitaire_settings")

class SolitaireContainer(
    context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "solitaire.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.solitaireSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = solitaireSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
