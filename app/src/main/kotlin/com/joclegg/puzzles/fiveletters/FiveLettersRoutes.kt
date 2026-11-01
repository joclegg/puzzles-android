package com.joclegg.puzzles.fiveletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.joclegg.puzzles.core.ui.stats.StatsRoute
import com.joclegg.puzzles.core.ui.stats.StatsViewModel
import com.joclegg.puzzles.fiveletters.feature.game.GameRoute
import com.joclegg.puzzles.fiveletters.feature.game.GameViewModel
import com.joclegg.puzzles.fiveletters.feature.home.HomeRoute
import com.joclegg.puzzles.fiveletters.feature.home.HomeViewModel
import com.joclegg.puzzles.fiveletters.feature.settings.SettingsRoute
import com.joclegg.puzzles.fiveletters.feature.settings.SettingsViewModel
import com.joclegg.puzzles.fiveletters.ui.FiveLettersTheme
import com.joclegg.puzzles.fiveletters.ui.LocalFiveLettersColors

object FiveLettersRoutes {
    const val HOME = "five_letters/home"
    private const val GAME = "five_letters/game/{id}"
    private const val STATS = "five_letters/stats"

    fun NavGraphBuilder.fiveLettersGraph(
        container: FiveLettersDefinition,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        screen(container, HOME, darkTheme) {
            HomeRoute(
                viewModel =
                    viewModel {
                        HomeViewModel(
                            container.games,
                            container.settings,
                            container::words,
                            container.newSeed,
                            container.today,
                            container.daily2Words,
                        )
                    },
                onOpenGame = { navController.openFiveLettersGame(it) },
                onStats = { navController.navigate(STATS) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.FIVE_LETTERS)) },
                onBack = navController::popBackStack,
            )
        }
        screen(container, GAME, darkTheme, listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            GameRoute(
                viewModel =
                    viewModel(
                        key = "five_letters_game_$id",
                    ) { GameViewModel(id, container.games, container.settings, container::words, container.newSeed) },
                onOpenGame = { navController.openFiveLettersGame(it) },
                onSettings = { navController.navigate(SettingsHub.route(SettingsTab.FIVE_LETTERS)) },
                onHome = { navController.popBackStack(HOME, inclusive = false) },
            )
        }
        screen(container, STATS, darkTheme) {
            StatsRoute(
                viewModel {
                    StatsViewModel(fiveLettersStatsSource(container.games))
                },
                onBack = navController::popBackStack,
                spacing = 16.dp,
            ) { FiveLettersStatsPanels(it) }
        }
    }

    @Composable
    fun SettingsTabContent(
        container: FiveLettersDefinition,
        darkTheme: Boolean,
    ) {
        Themed(container, darkTheme) {
            AccentColors(LocalFiveLettersColors.current.accent) {
                SettingsRoute(viewModel { SettingsViewModel(container.settings) }, onBack = null)
            }
        }
    }

    fun NavHostController.openFiveLettersGame(id: Long) {
        navigate("five_letters/game/$id") {
            popUpTo(HOME)
            launchSingleTop = true
        }
    }

    @Composable
    private fun Themed(
        container: FiveLettersDefinition,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
        FiveLettersTheme(darkTheme, highContrast = settings?.highContrast == true, content = content)
    }

    private fun NavGraphBuilder.screen(
        container: FiveLettersDefinition,
        route: String,
        darkTheme: Boolean,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit,
    ) {
        composable(route, arguments = arguments) { entry -> Themed(container, darkTheme) { content(entry) } }
    }
}
