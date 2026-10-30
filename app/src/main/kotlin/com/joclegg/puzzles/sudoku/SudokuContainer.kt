package com.joclegg.puzzles.sudoku

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.joclegg.puzzles.banks
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.sudoku.core.data.GameDatabase
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.PuzzleBank
import com.joclegg.puzzles.sudoku.core.data.PuzzleSupply
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository

private val Context.sudokuSettingsStore by preferencesDataStore(name = "settings")

class SudokuContainer(
    context: Context,
) {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "sudoku.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.sudokuSettingsStore)
    val settings = SettingsRepository(settingsStore)
    val sync = sudokuSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            context.banks.read(path)
        }
    val supply =
        PuzzleSupply(
            bank = bank,
            games = games,
            generated = database.generatedPuzzleDao(),
            onRunningLow = { PuzzleTopUpWorker.enqueue(context) },
        )
}
