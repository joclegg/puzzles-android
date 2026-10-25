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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joclegg.puzzles.sightlines.SightlinesRoutes
import com.joclegg.puzzles.sightlines.SightlinesRoutes.openSightlinesGame
import com.joclegg.puzzles.sightlines.SightlinesRoutes.sightlinesGraph
import com.joclegg.puzzles.solitaire.SolitaireRoutes
import com.joclegg.puzzles.solitaire.SolitaireRoutes.openSolitaireGame
import com.joclegg.puzzles.solitaire.SolitaireRoutes.solitaireGraph
import com.joclegg.puzzles.sudoku.SudokuRoutes
import com.joclegg.puzzles.sudoku.SudokuRoutes.openSudokuGame
import com.joclegg.puzzles.sudoku.SudokuRoutes.sudokuGraph
import com.joclegg.puzzles.twotone.TwoToneRoutes
import com.joclegg.puzzles.twotone.TwoToneRoutes.openTwoToneGame
import com.joclegg.puzzles.twotone.TwoToneRoutes.twoToneGraph

enum class PuzzleGame { SUDOKU, SOLITAIRE, TWO_TONE, SIGHTLINES }

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
            }
            currentOnConsume()
        }
    }
    Scaffold(modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) { padding ->
        NavHost(navController, startDestination = LAUNCHER, modifier = Modifier.padding(padding)) {
            composable(LAUNCHER) {
                LauncherScreen(
                    onSudokuClick = { navController.navigate(SudokuRoutes.HOME) },
                    onSolitaireClick = { navController.navigate(SolitaireRoutes.HOME) },
                    onTwoToneClick = { navController.navigate(TwoToneRoutes.HOME) },
                    onSightlinesClick = { navController.navigate(SightlinesRoutes.HOME) },
                )
            }
            sudokuGraph(app.sudoku, navController)
            solitaireGraph(app.solitaire, navController, darkTheme)
            twoToneGraph(app.twoTone, navController, darkTheme)
            sightlinesGraph(app.sightlines, navController, darkTheme)
        }
    }
}
