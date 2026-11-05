package com.joclegg.puzzles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joclegg.puzzles.core.sync.SignInProvider
import com.joclegg.puzzles.core.ui.DailyCalendars
import com.joclegg.puzzles.core.ui.LocalDailiesLeft
import com.joclegg.puzzles.core.ui.LocalDailyCalendars
import com.joclegg.puzzles.core.ui.Medal
import com.joclegg.puzzles.core.ui.dailyDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

enum class PuzzleGame { SUDOKU, SOLITAIRE, TWO_TONE, SIGHTLINES, FIVE_LETTERS, KAKURO, SLIDE_OUT, WORD_WHEEL }

data class OpenGame(
    val game: PuzzleGame,
    val id: Long?,
)

const val LAUNCHER = "launcher"

fun todaysMedal(
    app: PuzzlesApplication,
    tab: SettingsTab,
): Flow<Medal> {
    val rules = calendarRules(tab)
    return dailyResults(app, tab).map { results ->
        dailyDays(results, rules.total, rules.firstMedal, ZoneId.systemDefault())[LocalDate.now()]?.medal ?: Medal.NONE
    }
}

fun todaysMedals(app: PuzzlesApplication): Flow<Map<PuzzleGame, Medal>> {
    val perGame = PuzzleGame.entries.map { game -> todaysMedal(app, SettingsTab.valueOf(game.name)).map { game to it } }
    return combine(perGame) { it.toMap() }
}

fun holdsEveryGold(medals: Map<PuzzleGame, Medal>): Boolean = PuzzleGame.entries.all { medals[it] == Medal.GOLD }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PuzzlesApp(
    app: PuzzlesApplication,
    darkTheme: Boolean,
    openGame: OpenGame?,
    onConsumeOpenGame: () -> Unit,
    account: String?,
    onSignInClick: (SignInProvider) -> Unit,
    onSendEmailLink: suspend (String) -> Boolean,
    onVerifyEmailCode: suspend (String, String) -> Boolean,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentOnConsume by rememberUpdatedState(onConsumeOpenGame)
    LaunchedEffect(openGame) {
        if (openGame != null) {
            val definition = app.definition(openGame.game)
            navController.navigate(definition.homeRoute)
            openGame.id?.let { definition.openGame(navController, it) }
            currentOnConsume()
        }
    }
    val medals = remember(app) { todaysMedals(app) }
    val calendars =
        remember(app, navController) {
            DailyCalendars(
                todaysMedal = { game -> todaysMedal(app, SettingsTab.valueOf(game)) },
                onOpen = { game -> navController.navigate(DailyCalendarRoutes.route(SettingsTab.valueOf(game))) },
                allGold = app.celebrateAllGolds(medals),
                onAllGoldShown = app::markAllGoldsCelebrated,
            )
        }
    val todays by medals.collectAsState(emptyMap())
    val openHome: (PuzzleGame) -> Unit = { game ->
        if (app.hasBanksFor(game)) {
            navController.navigate(app.definition(game).homeRoute)
        } else {
            navController.navigate(BankGate.route(game))
        }
    }
    CompositionLocalProvider(
        LocalDailiesLeft provides rememberTodaysDailies(app, navController),
        LocalDailyCalendars provides calendars,
    ) {
        Scaffold(modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) { padding ->
            NavHost(navController, startDestination = LAUNCHER, modifier = Modifier.padding(padding)) {
                composable(
                    SettingsHub.ROUTE,
                    arguments =
                        listOf(
                            navArgument("tab") { type = NavType.StringType },
                            navArgument("mode") {
                                type = NavType.StringType
                                nullable = true
                            },
                        ),
                ) { entry ->
                    val argument = entry.arguments?.getString("mode")
                    SettingsHubRoute(
                        viewModel = viewModel { AppSettingsViewModel(app.sudoku.settings, app.auth.email, app.auth::deleteAccount) },
                        initialTab = SettingsTab.valueOf(entry.arguments!!.getString("tab")!!),
                        onBack = navController::popBackStack,
                        accentOf = { tab -> settingsAccent(app, tab, darkTheme) },
                        tabContent = { tab ->
                            if (tab != SettingsTab.GENERAL) {
                                app.definition(PuzzleGame.valueOf(tab.name)).SettingsContent(darkTheme, argument)
                            }
                        },
                    )
                }
                composable(DailyCalendarRoutes.ROUTE, arguments = listOf(navArgument("tab") { type = NavType.StringType })) { entry ->
                    DailyCalendarRoute(app, SettingsTab.valueOf(entry.arguments!!.getString("tab")!!), navController)
                }
                composable(BankGate.ROUTE, arguments = listOf(navArgument("game") { type = NavType.StringType })) { entry ->
                    val game = PuzzleGame.valueOf(entry.arguments!!.getString("game")!!)
                    BankGateRoute(
                        app,
                        game,
                        onReady = {
                            navController.navigate(app.definition(game).homeRoute) {
                                popUpTo(BankGate.ROUTE) { inclusive = true }
                            }
                        },
                        onBack = navController::popBackStack,
                    )
                }
                composable(COMBINED_HISTORY) {
                    CombinedHistoryRoute(app, onBack = navController::popBackStack)
                }
                composable(LAUNCHER) {
                    LauncherScreen(
                        darkTheme = darkTheme,
                        account = account,
                        onSignInClick = onSignInClick,
                        onSendEmailLink = onSendEmailLink,
                        onVerifyEmailCode = onVerifyEmailCode,
                        onSignOutClick = onSignOutClick,
                        medals = todays,
                        onSettingsClick = { navController.navigate(SettingsHub.route(SettingsTab.GENERAL)) },
                        onHistoryClick = { navController.navigate(COMBINED_HISTORY) },
                        onSudokuClick = { openHome(PuzzleGame.SUDOKU) },
                        onSolitaireClick = { openHome(PuzzleGame.SOLITAIRE) },
                        onTwoToneClick = { openHome(PuzzleGame.TWO_TONE) },
                        onSightlinesClick = { openHome(PuzzleGame.SIGHTLINES) },
                        onFiveLettersClick = { openHome(PuzzleGame.FIVE_LETTERS) },
                        onKakuroClick = { openHome(PuzzleGame.KAKURO) },
                        onSlideOutClick = { openHome(PuzzleGame.SLIDE_OUT) },
                        onWordWheelClick = { openHome(PuzzleGame.WORD_WHEEL) },
                    )
                }
                app.games.forEach { it.registerGraph(this, navController, darkTheme) }
            }
        }
    }
}
