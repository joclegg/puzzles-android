package com.joclegg.puzzles.sudoku

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.joclegg.puzzles.AccentColors
import com.joclegg.puzzles.SettingsHub
import com.joclegg.puzzles.SettingsTab
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.openLevel
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.feature.game.EntryRoute
import com.joclegg.puzzles.sudoku.feature.game.EntryViewModel
import com.joclegg.puzzles.sudoku.feature.game.GameRoute
import com.joclegg.puzzles.sudoku.feature.game.GameViewModel
import com.joclegg.puzzles.sudoku.feature.home.HomeRoute
import com.joclegg.puzzles.sudoku.feature.home.HomeViewModel
import com.joclegg.puzzles.sudoku.feature.settings.SettingsRoute
import com.joclegg.puzzles.sudoku.feature.settings.SettingsViewModel
import com.joclegg.puzzles.sudoku.feature.stats.HistoryRoute
import com.joclegg.puzzles.sudoku.feature.stats.StatsRoute
import com.joclegg.puzzles.sudoku.feature.stats.StatsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SudokuRoutes {
    const val HOME = "sudoku/home"
    private const val GAME = "sudoku/game/{id}"
    private const val ENTRY = "sudoku/entry/{mode}"
    private const val STATS = "sudoku/stats"
    private const val HISTORY = "sudoku/history"

    fun NavGraphBuilder.sudokuGraph(
        container: SudokuContainer,
        navController: NavHostController,
    ) {
        composable(HOME) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.settings, container.bank, container.supply) },
                onOpenGame = { navController.openSudokuGame(it) },
                onEnterPuzzle = { mode -> navController.navigate(entry(mode)) },
                onSettings = { mode -> navController.navigate(SettingsHub.route(SettingsTab.SUDOKU, mode.name)) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onBack = navController::popBackStack,
            )
        }
        composable(GAME, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            val scope = rememberCoroutineScope()
            GameRoute(
                viewModel = viewModel { GameViewModel(id, container.games, container.settings, SystemClock::elapsedRealtime) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.SUDOKU, it.name)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
                onNewPuzzle = { mode, level ->
                    scope.launch {
                        val mistakeCheck =
                            container.settings.settings
                                .first()
                                .mistakeCheckFor(mode)
                        navController.openSudokuGame(container.games.openLevel(container.supply, mode, level, mistakeCheck))
                    }
                },
            )
        }
        composable(ENTRY) { entry ->
            val mode = GameMode.valueOf(entry.arguments!!.getString("mode")!!)
            EntryRoute(
                viewModel = viewModel { EntryViewModel(mode, container.games, container.settings) },
                onPlay = { navController.openSudokuGame(it) },
                onBack = navController::popBackStack,
            )
        }
        composable(STATS) {
            StatsRoute(viewModel { StatsViewModel(container.games, container.settings) }, onBack = navController::popBackStack)
        }
        composable(HISTORY) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games, container.settings) },
                onOpenGame = { navController.openSudokuGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: SudokuContainer,
        mode: GameMode,
    ) {
        AccentColors(LocalBoardColors.current.accent) {
            SettingsRoute(viewModel { SettingsViewModel(container.settings) }, initialMode = mode, onBack = null)
        }
    }

    fun NavHostController.openSudokuGame(id: Long) {
        navigate("sudoku/game/$id") {
            popUpTo(HOME)
            launchSingleTop = true
        }
    }

    private fun entry(mode: GameMode) = "sudoku/entry/${mode.name}"
}
