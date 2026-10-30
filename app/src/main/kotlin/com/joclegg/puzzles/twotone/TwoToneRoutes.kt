package com.joclegg.puzzles.twotone

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
import com.joclegg.puzzles.AccentColors
import com.joclegg.puzzles.SettingsHub
import com.joclegg.puzzles.SettingsTab
import com.joclegg.puzzles.twotone.core.ui.LocalTileColors
import com.joclegg.puzzles.twotone.core.ui.TwoToneTheme
import com.joclegg.puzzles.twotone.feature.game.GameRoute
import com.joclegg.puzzles.twotone.feature.game.GameViewModel
import com.joclegg.puzzles.twotone.feature.home.HomeRoute
import com.joclegg.puzzles.twotone.feature.home.HomeViewModel
import com.joclegg.puzzles.twotone.feature.settings.SettingsRoute
import com.joclegg.puzzles.twotone.feature.settings.SettingsViewModel
import com.joclegg.puzzles.twotone.feature.stats.HistoryRoute
import com.joclegg.puzzles.twotone.feature.stats.StatsRoute
import com.joclegg.puzzles.twotone.feature.stats.StatsViewModel

object TwoToneRoutes {
    const val HOME = "two_tone/home"
    private const val GAME = "two_tone/game/{id}"
    private const val STATS = "two_tone/stats"
    private const val HISTORY = "two_tone/history"

    fun NavGraphBuilder.twoToneGraph(
        container: TwoToneContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.newSeed, container.today) },
                onOpenGame = { navController.openTwoToneGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.TWO_TONE)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "two_tone_game_$id") {
                        GameViewModel(id, container.games, container.settings, SystemClock::elapsedRealtime, container.newSeed)
                    },
                onOpenGame = { navController.openTwoToneGame(it) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.TWO_TONE)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openTwoToneGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: TwoToneContainer,
        darkTheme: Boolean,
    ) {
        TwoToneTheme(darkTheme) {
            AccentColors(LocalTileColors.current.accent) {
                SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
            }
        }
    }

    fun NavHostController.openTwoToneGame(id: Long) {
        navigate("two_tone/game/$id") {
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
        composable(route, arguments = arguments) { entry -> TwoToneTheme(darkTheme) { content(entry) } }
    }
}
