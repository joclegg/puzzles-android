package com.joclegg.puzzles.kakuro

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.kakuro.core.data.GameDatabase
import com.joclegg.puzzles.kakuro.core.data.GameRepository
import com.joclegg.puzzles.kakuro.core.data.PuzzleBank
import com.joclegg.puzzles.kakuro.core.data.SettingsRepository
import java.time.LocalDate
import kotlin.random.Random

private val Context.kakuroSettingsStore by preferencesDataStore(name = "kakuro_settings")

class KakuroContainer(
    context: Context,
) {
    private val database = Room.databaseBuilder(context, GameDatabase::class.java, "kakuro.db").build()
    val games = GameRepository(database.gameDao())
    val settings = SettingsRepository(context.kakuroSettingsStore)
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
