package com.joclegg.puzzles.crossword

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
import com.joclegg.puzzles.core.ui.stats.HistoryRoute
import com.joclegg.puzzles.core.ui.stats.StatsRoute
import com.joclegg.puzzles.core.ui.stats.StatsViewModel
import com.joclegg.puzzles.crossword.feature.game.GameRoute
import com.joclegg.puzzles.crossword.feature.game.GameViewModel
import com.joclegg.puzzles.crossword.feature.home.HomeRoute
import com.joclegg.puzzles.crossword.feature.home.HomeViewModel
import com.joclegg.puzzles.crossword.feature.settings.SettingsRoute
import com.joclegg.puzzles.crossword.feature.settings.SettingsViewModel
import com.joclegg.puzzles.crossword.ui.CrosswordTheme
import com.joclegg.puzzles.crossword.ui.LocalCrosswordColors

object CrosswordRoutes {
    const val HOME = "crossword/home"
    private const val GAME = "crossword/game/{id}"
    private const val STATS = "crossword/stats"
    private const val HISTORY = "crossword/history"

    fun NavGraphBuilder.crosswordGraph(
        container: CrosswordDefinition,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(HOME, darkTheme) {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.games, container.bank, container.newSeed, container.today) },
                onOpenGame = { navController.openCrosswordGame(it) },
                onStats = { navController.navigate(STATS) },
                onHistory = { navController.navigate(HISTORY) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.CROSSWORD)) },
                onBack = navController::popBackStack,
            )
        }
        screen(GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(key = "crossword_game_$id") {
                        GameViewModel(
                            id,
                            container.games,
                            container.settings,
                            container.bank,
                            SystemClock::elapsedRealtime,
                            container.newSeed,
                        )
                    },
                onOpenGame = { navController.openCrosswordGame(it) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.CROSSWORD)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(STATS, darkTheme) {
            StatsRoute(
                viewModel {
                    StatsViewModel(crosswordStatsSource(container.games))
                },
                onBack = navController::popBackStack,
                footnote = CROSSWORD_STATS_FOOTNOTE,
            ) { CrosswordStatsTable(it) }
        }
        screen(HISTORY, darkTheme) {
            HistoryRoute(
                viewModel = viewModel { StatsViewModel(crosswordStatsSource(container.games)) },
                accent = LocalCrosswordColors.current.accent,
                filters = crosswordHistoryFilters,
                onOpenGame = { navController.openCrosswordGame(it) },
                onBack = navController::popBackStack,
            )
        }
    }

    @Composable
    fun SettingsTabContent(
        container: CrosswordDefinition,
        darkTheme: Boolean,
    ) {
        CrosswordTheme(darkTheme) {
            AccentColors(LocalCrosswordColors.current.accent) {
                SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
            }
        }
    }

    fun NavHostController.openCrosswordGame(id: Long) {
        navigate("crossword/game/$id") {
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
        composable(route, arguments = arguments) { entry -> CrosswordTheme(darkTheme) { content(entry) } }
    }
}
