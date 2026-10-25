package com.joclegg.puzzles.sudoku

import android.os.SystemClock
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.feature.game.EntryRoute
import com.joclegg.puzzles.sudoku.feature.game.EntryViewModel
import com.joclegg.puzzles.sudoku.feature.game.GameRoute
import com.joclegg.puzzles.sudoku.feature.game.GameViewModel
import com.joclegg.puzzles.sudoku.feature.home.DailyRoute
import com.joclegg.puzzles.sudoku.feature.home.DailyViewModel
import com.joclegg.puzzles.sudoku.feature.home.HomeRoute
import com.joclegg.puzzles.sudoku.feature.home.HomeViewModel
import com.joclegg.puzzles.sudoku.feature.settings.SettingsRoute
import com.joclegg.puzzles.sudoku.feature.settings.SettingsViewModel
import com.joclegg.puzzles.sudoku.feature.stats.HistoryRoute
import com.joclegg.puzzles.sudoku.feature.stats.StatsRoute
import com.joclegg.puzzles.sudoku.feature.stats.StatsViewModel

object SudokuRoutes {
    const val HOME = "sudoku/home"
    private const val GAME = "sudoku/game/{id}"
    private const val ENTRY = "sudoku/entry/{mode}"
    private const val STATS = "sudoku/stats"
    private const val HISTORY = "sudoku/history"
    private const val DAILY = "sudoku/daily"
    private const val SETTINGS = "sudoku/settings/{mode}"

    fun NavGraphBuilder.sudokuGraph(
        container: SudokuContainer,
        navController: NavHostController,
    ) {
        composable(HOME) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.settings, container.bank, container.supply) },
                onOpenGame = { navController.openSudokuGame(it) },
                onEnterPuzzle = { mode -> navController.navigate(entry(mode)) },
                onDailyCalendar = { navController.navigate(DAILY) },
                onSettings = { mode -> navController.navigate(settings(mode)) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onBack = navController::popBackStack,
            )
        }
        composable(GAME, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel = viewModel { GameViewModel(id, container.games, container.settings, SystemClock::elapsedRealtime) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
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
        composable(DAILY) {
            DailyRoute(
                viewModel = viewModel { DailyViewModel(container.games, container.settings, container.bank) },
                onOpenGame = { navController.openSudokuGame(it) },
                onBack = navController::popBackStack,
            )
        }
        composable(SETTINGS) { entry ->
            SettingsRoute(
                viewModel = viewModel { SettingsViewModel(container.settings) },
                initialMode = GameMode.valueOf(entry.arguments!!.getString("mode")!!),
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

    fun NavHostController.openSudokuGame(id: Long) {
        navigate("sudoku/game/$id") {
            popUpTo(HOME)
            launchSingleTop = true
        }
    }

    private fun entry(mode: GameMode) = "sudoku/entry/${mode.name}"

    private fun settings(mode: GameMode) = "sudoku/settings/${mode.name}"
}
