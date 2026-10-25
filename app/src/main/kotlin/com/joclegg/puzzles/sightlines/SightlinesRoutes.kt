package com.joclegg.puzzles.sightlines

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
import com.joclegg.puzzles.sightlines.core.ui.SightlinesTheme
import com.joclegg.puzzles.sightlines.feature.game.GameRoute
import com.joclegg.puzzles.sightlines.feature.game.GameViewModel
import com.joclegg.puzzles.sightlines.feature.home.HomeRoute
import com.joclegg.puzzles.sightlines.feature.home.HomeViewModel
import com.joclegg.puzzles.sightlines.feature.settings.SettingsRoute
import com.joclegg.puzzles.sightlines.feature.settings.SettingsViewModel
import com.joclegg.puzzles.sightlines.feature.stats.HistoryRoute
import com.joclegg.puzzles.sightlines.feature.stats.StatsRoute
import com.joclegg.puzzles.sightlines.feature.stats.StatsViewModel

object SightlinesRoutes {
    const val HOME = "sightlines/home"
    private const val GAME = "sightlines/game/{id}"
    private const val STATS = "sightlines/stats"
    private const val HISTORY = "sightlines/history"
    private const val SETTINGS = "sightlines/settings"

    fun NavGraphBuilder.sightlinesGraph(
        container: SightlinesContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.newSeed, container.today) },
                onOpenGame = { navController.openSightlinesGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SETTINGS) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "sightlines_game_$id") {
                        GameViewModel(id, container.games, container.settings, SystemClock::elapsedRealtime, container.newSeed)
                    },
                onOpenGame = { navController.openSightlinesGame(it) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(SETTINGS, darkTheme) {
            SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = navController::popBackStack)
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openSightlinesGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    fun NavHostController.openSightlinesGame(id: Long) {
        navigate("sightlines/game/$id") {
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
        composable(route, arguments = arguments) { entry -> SightlinesTheme(darkTheme) { content(entry) } }
    }
}
