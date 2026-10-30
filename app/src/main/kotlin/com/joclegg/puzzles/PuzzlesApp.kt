package com.joclegg.puzzles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

enum class PuzzleGame { SUDOKU, SOLITAIRE, TWO_TONE, SIGHTLINES, FIVE_LETTERS, KAKURO, SLIDE_OUT, WORD_WHEEL }

data class OpenGame(
    val game: PuzzleGame,
    val id: Long?,
)

private const val LAUNCHER = "launcher"

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
                    viewModel = viewModel { AppSettingsViewModel(app.sudoku.settings) },
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
            composable(LAUNCHER) {
                LauncherScreen(
                    darkTheme = darkTheme,
                    account = account,
                    onSignInClick = onSignInClick,
                    onSendEmailLink = onSendEmailLink,
                    onVerifyEmailCode = onVerifyEmailCode,
                    onSignOutClick = onSignOutClick,
                    onSettingsClick = { navController.navigate(SettingsHub.route(SettingsTab.GENERAL)) },
                    onSudokuClick = { navController.navigate(SudokuRoutes.HOME) },
                    onSolitaireClick = { navController.navigate(SolitaireRoutes.HOME) },
                    onTwoToneClick = { navController.navigate(TwoToneRoutes.HOME) },
                    onSightlinesClick = { navController.navigate(SightlinesRoutes.HOME) },
                    onFiveLettersClick = { navController.navigate(FiveLettersRoutes.HOME) },
                    onKakuroClick = { navController.navigate(KakuroRoutes.HOME) },
                    onSlideOutClick = { navController.navigate(SlideOutRoutes.HOME) },
                    onWordWheelClick = { navController.navigate(WordWheelRoutes.HOME) },
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
