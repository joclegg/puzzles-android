package com.joclegg.puzzles.sightlines

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.sightlines.core.data.GameDatabase
import com.joclegg.puzzles.sightlines.core.data.GameRepository
import com.joclegg.puzzles.sightlines.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.sightlinesSettingsStore by preferencesDataStore(name = "sightlines_settings")

class SightlinesContainer(
    context: Context,
) {
    private val database = Room.databaseBuilder(context, GameDatabase::class.java, "sightlines.db").build()
    val games = GameRepository(database.gameDao())
    val settings = SettingsRepository(context.sightlinesSettingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
}
