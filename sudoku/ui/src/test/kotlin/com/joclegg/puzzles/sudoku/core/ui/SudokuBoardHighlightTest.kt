package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.joclegg.puzzles.sudoku.engine.GameState
import com.joclegg.puzzles.sudoku.engine.Highlight
import com.joclegg.puzzles.sudoku.engine.InputMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class SudokuBoardHighlightTest(
    private val description: String,
    private val game: GameState,
    private val cellTag: String,
    private val expected: Highlight,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `given a selection or active digit when the board is shown then each cell carries the expected highlight`() {
        // Act
        compose.setContent { SudokuTheme(darkTheme = false) { SudokuBoard(game.toBoardUi(), onCellClick = {}) } }

        // Assert
        compose.onNodeWithTag(cellTag).assert(SemanticsMatcher.expectValue(CellHighlightKey, expected))
    }

    companion object {
        private val topLeftSelected = GameState(boardPuzzle).tapCell(0)
        private val eightActive = GameState(boardPuzzle).withInputMode(InputMode.DIGIT_FIRST).tapDigit(8)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any>("selected cell", topLeftSelected, "cell_r1c1", Highlight.SELECTED),
                arrayOf<Any>("same row", topLeftSelected, "cell_r1c2", Highlight.PEER),
                arrayOf<Any>("same digit elsewhere", topLeftSelected, "cell_r2c6", Highlight.SAME_DIGIT),
                arrayOf<Any>("unrelated cell", topLeftSelected, "cell_r5c4", Highlight.NONE),
                arrayOf<Any>("active digit in a box", eightActive, "cell_r3c3", Highlight.SAME_DIGIT),
                arrayOf<Any>("active digit in another box", eightActive, "cell_r4c1", Highlight.SAME_DIGIT),
            )
    }
}
