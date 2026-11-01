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
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes.fiveLettersGraph
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes.openFiveLettersGame
import com.joclegg.puzzles.kakuro.KakuroRoutes
import com.joclegg.puzzles.kakuro.KakuroRoutes.kakuroGraph
import com.joclegg.puzzles.kakuro.KakuroRoutes.openKakuroGame
import com.joclegg.puzzles.sightlines.SightlinesRoutes
import com.joclegg.puzzles.sightlines.SightlinesRoutes.openSightlinesGame
import com.joclegg.puzzles.sightlines.SightlinesRoutes.sightlinesGraph
import com.joclegg.puzzles.slideout.SlideOutRoutes
import com.joclegg.puzzles.slideout.SlideOutRoutes.openSlideOutGame
import com.joclegg.puzzles.slideout.SlideOutRoutes.slideOutGraph
import com.joclegg.puzzles.solitaire.SolitaireRoutes
import com.joclegg.puzzles.solitaire.SolitaireRoutes.openSolitaireGame
import com.joclegg.puzzles.solitaire.SolitaireRoutes.solitaireGraph
import com.joclegg.puzzles.sudoku.SudokuRoutes
import com.joclegg.puzzles.sudoku.SudokuRoutes.openSudokuGame
import com.joclegg.puzzles.sudoku.SudokuRoutes.sudokuGraph
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.twotone.TwoToneRoutes
import com.joclegg.puzzles.twotone.TwoToneRoutes.openTwoToneGame
import com.joclegg.puzzles.twotone.TwoToneRoutes.twoToneGraph
import com.joclegg.puzzles.wordwheel.WordWheelRoutes
import com.joclegg.puzzles.wordwheel.WordWheelRoutes.openWordWheelGame
import com.joclegg.puzzles.wordwheel.WordWheelRoutes.wordWheelGraph
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

val PuzzleGame.home: String
    get() =
        when (this) {
            PuzzleGame.SUDOKU -> SudokuRoutes.HOME
            PuzzleGame.SOLITAIRE -> SolitaireRoutes.HOME
            PuzzleGame.TWO_TONE -> TwoToneRoutes.HOME
            PuzzleGame.SIGHTLINES -> SightlinesRoutes.HOME
            PuzzleGame.FIVE_LETTERS -> FiveLettersRoutes.HOME
            PuzzleGame.KAKURO -> KakuroRoutes.HOME
            PuzzleGame.SLIDE_OUT -> SlideOutRoutes.HOME
            PuzzleGame.WORD_WHEEL -> WordWheelRoutes.HOME
        }

fun todaysMedal(
    app: PuzzlesApplication,
    tab: SettingsTab,
): Flow<Medal> {
    val rules = calendarRules(tab)
    return dailyResults(app, tab).map { results ->
        dailyDays(results, rules.total, rules.firstMedal, ZoneId.systemDefault())[LocalDate.now()]?.medal ?: Medal.NONE
    }
}

fun todaysMedals(app: PuzzlesApplication): Flow<Map<PuzzleGame, Medal>> =
    combine(PuzzleGame.entries.map { game -> todaysMedal(app, SettingsTab.valueOf(game.name)).map { game to it } }) { it.toMap() }

fun holdsEveryGold(medals: Map<PuzzleGame, Medal>): Boolean =
    PuzzleGame.entries.all { medals[it] == Medal.GOLD }

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
            when (openGame.game) {
                PuzzleGame.SUDOKU -> {
                    navController.navigate(SudokuRoutes.HOME)
                    openGame.id?.let { navController.openSudokuGame(it) }
                }

                PuzzleGame.SOLITAIRE -> {
                    navController.navigate(SolitaireRoutes.HOME)
                    openGame.id?.let { navController.openSolitaireGame(it) }
                }

                PuzzleGame.TWO_TONE -> {
                    navController.navigate(TwoToneRoutes.HOME)
                    openGame.id?.let { navController.openTwoToneGame(it) }
                }

                PuzzleGame.SIGHTLINES -> {
                    navController.navigate(SightlinesRoutes.HOME)
                    openGame.id?.let { navController.openSightlinesGame(it) }
                }

                PuzzleGame.FIVE_LETTERS -> {
                    navController.navigate(FiveLettersRoutes.HOME)
                    openGame.id?.let { navController.openFiveLettersGame(it) }
                }

                PuzzleGame.KAKURO -> {
                    navController.navigate(KakuroRoutes.HOME)
                    openGame.id?.let { navController.openKakuroGame(it) }
                }

                PuzzleGame.SLIDE_OUT -> {
                    navController.navigate(SlideOutRoutes.HOME)
                    openGame.id?.let { navController.openSlideOutGame(it) }
                }

                PuzzleGame.WORD_WHEEL -> {
                    navController.navigate(WordWheelRoutes.HOME)
                    openGame.id?.let { navController.openWordWheelGame(it) }
                }
            }
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
            navController.navigate(game.home)
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
                    val mode = entry.arguments?.getString("mode")?.let(GameMode::valueOf) ?: GameMode.CLASSIC
                    SettingsHubRoute(
                        viewModel = viewModel { AppSettingsViewModel(app.sudoku.settings, app.auth.email, app.auth::deleteAccount) },
                        initialTab = SettingsTab.valueOf(entry.arguments!!.getString("tab")!!),
                        onBack = navController::popBackStack,
                        accentOf = { tab -> settingsAccent(tab, darkTheme) },
                        tabContent = { tab ->
                            when (tab) {
                                SettingsTab.GENERAL -> Unit
                                SettingsTab.SUDOKU -> SudokuRoutes.SettingsTabContent(app.sudoku, mode)
                                SettingsTab.SOLITAIRE -> SolitaireRoutes.SettingsTabContent(app.solitaire, darkTheme)
                                SettingsTab.TWO_TONE -> TwoToneRoutes.SettingsTabContent(app.twoTone, darkTheme)
                                SettingsTab.SIGHTLINES -> SightlinesRoutes.SettingsTabContent(app.sightlines, darkTheme)
                                SettingsTab.FIVE_LETTERS -> FiveLettersRoutes.SettingsTabContent(app.fiveLetters, darkTheme)
                                SettingsTab.KAKURO -> KakuroRoutes.SettingsTabContent(app.kakuro, darkTheme)
                                SettingsTab.SLIDE_OUT -> SlideOutRoutes.SettingsTabContent(app.slideOut, darkTheme)
                                SettingsTab.WORD_WHEEL -> WordWheelRoutes.SettingsTabContent(app.wordWheel, darkTheme)
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
                            navController.navigate(game.home) {
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
                sudokuGraph(app.sudoku, navController)
                solitaireGraph(app.solitaire, navController, darkTheme)
                twoToneGraph(app.twoTone, navController, darkTheme)
                sightlinesGraph(app.sightlines, navController, darkTheme)
                fiveLettersGraph(app.fiveLetters, navController, darkTheme)
                kakuroGraph(app.kakuro, navController, darkTheme)
                slideOutGraph(app.slideOut, navController, darkTheme)
                wordWheelGraph(app.wordWheel, navController, darkTheme)
            }
        }
    }
}
