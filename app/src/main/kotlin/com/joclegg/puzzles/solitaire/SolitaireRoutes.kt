package com.joclegg.puzzles.solitaire

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
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTheme
import com.joclegg.puzzles.solitaire.feature.game.GameRoute
import com.joclegg.puzzles.solitaire.feature.game.GameViewModel
import com.joclegg.puzzles.solitaire.feature.home.HomeRoute
import com.joclegg.puzzles.solitaire.feature.home.HomeViewModel
import com.joclegg.puzzles.solitaire.feature.settings.SettingsRoute
import com.joclegg.puzzles.solitaire.feature.settings.SettingsViewModel
import com.joclegg.puzzles.solitaire.feature.stats.HistoryRoute
import com.joclegg.puzzles.solitaire.feature.stats.StatsRoute
import com.joclegg.puzzles.solitaire.feature.stats.StatsViewModel

object SolitaireRoutes {
    const val HOME = "solitaire/home"
    private const val GAME = "solitaire/game/{id}"
    private const val STATS = "solitaire/stats"
    private const val HISTORY = "solitaire/history"

    fun NavGraphBuilder.solitaireGraph(
        container: SolitaireContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.settings, container.newSeed, container.today) },
                onOpenGame = { navController.openSolitaireGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.SOLITAIRE)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "solitaire_game_$id") {
                        GameViewModel(id, container.games, container.settings, SystemClock::elapsedRealtime, container.newSeed)
                    },
                onOpenGame = { navController.openSolitaireGame(it) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.SOLITAIRE)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openSolitaireGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: SolitaireContainer,
        darkTheme: Boolean,
    ) {
        SolitaireTheme(darkTheme) {
            SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
        }
    }

    fun NavHostController.openSolitaireGame(id: Long) {
        navigate("solitaire/game/$id") {
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
        composable(route, arguments = arguments) { entry -> SolitaireTheme(darkTheme) { content(entry) } }
    }
}
