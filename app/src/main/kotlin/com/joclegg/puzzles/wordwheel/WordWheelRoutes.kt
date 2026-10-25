package com.joclegg.puzzles.wordwheel

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.joclegg.puzzles.SettingsHub
import com.joclegg.puzzles.SettingsTab
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelTheme
import com.joclegg.puzzles.wordwheel.feature.game.GameRoute
import com.joclegg.puzzles.wordwheel.feature.game.GameViewModel
import com.joclegg.puzzles.wordwheel.feature.home.HomeRoute
import com.joclegg.puzzles.wordwheel.feature.home.HomeViewModel
import com.joclegg.puzzles.wordwheel.feature.settings.SettingsRoute
import com.joclegg.puzzles.wordwheel.feature.settings.SettingsViewModel
import com.joclegg.puzzles.wordwheel.feature.stats.HistoryRoute
import com.joclegg.puzzles.wordwheel.feature.stats.StatsRoute
import com.joclegg.puzzles.wordwheel.feature.stats.StatsViewModel

object WordWheelRoutes {
    const val HOME = "word_wheel/home"
    private const val GAME = "word_wheel/game/{id}"
    private const val STATS = "word_wheel/stats"
    private const val HISTORY = "word_wheel/history"

    fun NavGraphBuilder.wordWheelGraph(
        container: WordWheelContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.bank, container.newSeed, container.today) },
                onOpenGame = { navController.openWordWheelGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.WORD_WHEEL)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "wordwheel_game_$id") {
                        GameViewModel(
                            id,
                            container.games,
                            container.settings,
                            container.bank,
                            SystemClock::elapsedRealtime,
                            container.newSeed,
                        )
                    },
                onOpenGame = { navController.openWordWheelGame(it) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openWordWheelGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: WordWheelContainer,
        darkTheme: Boolean,
    ) {
        WordWheelTheme(darkTheme) {
            SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
        }
    }

    fun NavHostController.openWordWheelGame(id: Long) {
        navigate("word_wheel/game/$id") {
            popUpTo(HOME)
            launchSingleTop = true
        }
    }

    private fun NavGraphBuilder.screen(
        route: String,
        darkTheme: Boolean,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit,
    ) {
        composable(route, arguments = arguments) { entry -> WordWheelTheme(darkTheme) { content(entry) } }
    }
}
