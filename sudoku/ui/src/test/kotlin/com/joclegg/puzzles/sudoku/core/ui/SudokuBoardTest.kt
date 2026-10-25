package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.sudoku.engine.Cage
import com.joclegg.puzzles.sudoku.engine.GameState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SudokuBoardTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: GameState) {
        compose.setContent { SudokuTheme(darkTheme = false) { SudokuBoard(state.toBoardUi(), onCellClick = {}) } }
    }

    @Test
    fun `given a new game when the board is shown then givens are marked as givens and empty cells are not`() {
        // Arrange
        val game = GameState(boardPuzzle)

        // Act
        show(game)

        // Assert
        compose.onNodeWithTag("cell_r1c1").assert(SemanticsMatcher.expectValue(CellGivenKey, true))
        compose.onNodeWithTag("cell_r1c3").assert(SemanticsMatcher.expectValue(CellGivenKey, false))
    }

    @Test
    fun `given a wrong digit in a full cage when the board is shown then the cell and cage are flagged`() {
        // Arrange
        val cage = Cage(listOf(2, 3), sum = 10)
        val game =
            GameState(boardPuzzle.copy(cages = listOf(cage)))
                .tapCell(2)
                .tapDigit(3)
                .tapCell(3)
                .tapDigit(6)

        // Act
        show(game)

        // Assert
        compose.onNodeWithTag("cell_r1c3").assert(SemanticsMatcher.expectValue(CellWrongKey, true))
        compose.onNodeWithTag("cell_r1c3").assert(SemanticsMatcher.expectValue(CageWrongKey, true))
        compose.onNodeWithTag("cell_r1c4").assert(SemanticsMatcher.expectValue(CellWrongKey, false))
    }
}
