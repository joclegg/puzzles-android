package com.joclegg.puzzles.kakuro

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
import com.joclegg.puzzles.kakuro.core.ui.KakuroTheme
import com.joclegg.puzzles.kakuro.core.ui.LocalKakuroColors
import com.joclegg.puzzles.kakuro.feature.game.GameRoute
import com.joclegg.puzzles.kakuro.feature.game.GameViewModel
import com.joclegg.puzzles.kakuro.feature.home.HomeRoute
import com.joclegg.puzzles.kakuro.feature.home.HomeViewModel
import com.joclegg.puzzles.kakuro.feature.settings.SettingsRoute
import com.joclegg.puzzles.kakuro.feature.settings.SettingsViewModel
import com.joclegg.puzzles.kakuro.feature.stats.HistoryRoute
import com.joclegg.puzzles.kakuro.feature.stats.StatsRoute
import com.joclegg.puzzles.kakuro.feature.stats.StatsViewModel

object KakuroRoutes {
    const val HOME = "kakuro/home"
    private const val GAME = "kakuro/game/{id}"
    private const val STATS = "kakuro/stats"
    private const val HISTORY = "kakuro/history"

    fun NavGraphBuilder.kakuroGraph(
        container: KakuroContainer,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.bank, container.newSeed, container.today) },
                onOpenGame = { navController.openKakuroGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.KAKURO)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "kakuro_game_$id") {
                        GameViewModel(
                            id,
                            container.games,
                            container.settings,
                            container.bank,
                            SystemClock::elapsedRealtime,
                            container.newSeed,
                        )
                    },
                onOpenGame = { navController.openKakuroGame(it) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.KAKURO)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(viewModel { StatsViewModel(container.games) }, onBack = navController::popBackStack)
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(container.games) },
                onOpenGame = { navController.openKakuroGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: KakuroContainer,
        darkTheme: Boolean,
    ) {
        KakuroTheme(darkTheme) {
            AccentColors(LocalKakuroColors.current.accent) {
                SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
            }
        }
    }

    fun NavHostController.openKakuroGame(id: Long) {
        navigate("kakuro/game/$id") {
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
        composable(route, arguments = arguments) { entry -> KakuroTheme(darkTheme) { content(entry) } }
    }
}
