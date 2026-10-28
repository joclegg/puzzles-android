package com.joclegg.puzzles.twotone

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.twotone.core.data.GameDatabase
import com.joclegg.puzzles.twotone.core.data.GameRepository
import com.joclegg.puzzles.twotone.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.twotoneSettingsStore by preferencesDataStore(name = "two_tone_settings")

class TwoToneContainer(
    context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "two_tone.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.twotoneSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = twoToneSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
