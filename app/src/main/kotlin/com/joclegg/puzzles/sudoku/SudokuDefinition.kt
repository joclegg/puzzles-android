package com.joclegg.puzzles.sudoku

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.room.Room
import com.joclegg.puzzles.GameDefinition
import com.joclegg.puzzles.PuzzleGame
import com.joclegg.puzzles.banks
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.sudoku.core.data.GameDatabase
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.PuzzleBank
import com.joclegg.puzzles.sudoku.core.data.PuzzleSupply
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.ui.LocalBoardColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sudokuSettingsStore by preferencesDataStore(name = "settings")

class SudokuDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "sudoku.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.sudokuSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = sudokuSyncSource(database.gameDao(), settings, settingsStore)
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

    override val game = PuzzleGame.SUDOKU
    override val homeRoute = SudokuRoutes.HOME
    override val bankFolder: String? = "sudoku"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(SudokuRoutes) { builder.sudokuGraph(this@SudokuDefinition, navController) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(SudokuRoutes) { navController.openSudokuGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color = LocalBoardColors.current.accent

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        SudokuRoutes.SettingsTabContent(this, argument?.let(GameMode::valueOf) ?: GameMode.CLASSIC)
    }
}
