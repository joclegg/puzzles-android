package com.joclegg.puzzles.slideout

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
import com.joclegg.puzzles.slideout.core.ui.SlideOutTheme
import com.joclegg.puzzles.slideout.feature.game.GameRoute
import com.joclegg.puzzles.slideout.feature.game.GameViewModel
import com.joclegg.puzzles.slideout.feature.home.HomeRoute
import com.joclegg.puzzles.slideout.feature.home.HomeViewModel
import com.joclegg.puzzles.slideout.feature.settings.SettingsRoute
import com.joclegg.puzzles.slideout.feature.settings.SettingsViewModel
import com.joclegg.puzzles.slideout.feature.stats.HistoryRoute
import com.joclegg.puzzles.slideout.feature.stats.StatsRoute
import com.joclegg.puzzles.slideout.feature.stats.StatsViewModel

object SlideOutRoutes {
    const val HOME = "slide_out/home"
    private const val GAME = "slide_out/game/{id}"
    private const val STATS = "slide_out/stats"
    private const val HISTORY = "slide_out/history"

    fun NavGraphBuilder.slideOutGraph(
        container: SlideOutContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.bank, container.newSeed, container.today) },
                onOpenGame = { navController.openSlideOutGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.SLIDE_OUT)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "slideout_game_$id") {
                        GameViewModel(
                            id,
                            container.games,
                            container.settings,
                            container.bank,
                            SystemClock::elapsedRealtime,
                            container.newSeed,
                        )
                    },
                onOpenGame = { navController.openSlideOutGame(it) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openSlideOutGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: SlideOutContainer,
        darkTheme: Boolean,
    ) {
        SlideOutTheme(darkTheme) {
            SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
        }
    }

    fun NavHostController.openSlideOutGame(id: Long) {
        navigate("slide_out/game/$id") {
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
        composable(route, arguments = arguments) { entry -> SlideOutTheme(darkTheme) { content(entry) } }
    }
}
