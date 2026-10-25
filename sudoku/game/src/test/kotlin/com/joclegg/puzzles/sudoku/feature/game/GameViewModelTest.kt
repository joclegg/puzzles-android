package com.joclegg.puzzles.sudoku.feature.game

import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val harness = GameViewModelHarness()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `given a game shown for 5s, hidden for 55s and shown for 2s when the time is read then it is 7s`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val game = openGame()
                game.onVisible()
                clockMs = 5_000
                game.onHidden()
                clockMs = 60_000
                game.onVisible()
                clockMs = 62_000

                // Act
                game.onCellClick(LAST_EMPTY_CELL)

                // Assert
                assertEquals(7_000, game.uiState.value!!.elapsedMs)
                game.onHidden()
            }
        }

    @Test
    fun `given three pencil marks saved when the game is reopened and undone three times then they are gone`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val first = openGame()
                first.onToggleNotes()
                first.onCellClick(LAST_EMPTY_CELL)
                listOf(1, 2, 3).forEach(first::onDigitClick)
                first.onHidden()
                val reopened = openGame()

                // Act
                repeat(3) { reopened.onUndo() }

                // Assert
                assertEquals(
                    0,
                    reopened.uiState.value!!
                        .board.cells[LAST_EMPTY_CELL]
                        .notes,
                )
                reopened.onHidden()
            }
        }

    @Test
    fun `given one cell left when it is filled after 3s then the game completes with a new best`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val game = openGame()
                game.onVisible()
                clockMs = 3_000

                // Act
                game.onCellClick(LAST_EMPTY_CELL)
                game.onDigitClick(LAST_EMPTY_CELL_ANSWER)

                // Assert
                val saved = completed.await()
                val state = game.uiState.value!!
                assertEquals(
                    listOf(true, true, 3_000L, 3_000L),
                    listOf(state.completed, state.newBest, state.elapsedMs, saved.elapsedMs),
                )
            }
        }

    @Test
    fun `given count down chosen when the last cell's number is counted then it shows one left, and none once placed`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                settings.value = Settings(classic = ModeSettings(digitCounts = DigitCounts.COUNT_DOWN))
                val game = openGame()
                val before = game.uiState.first { it?.digitCounts?.get(LAST_EMPTY_CELL_ANSWER) == 1 }!!.digitCounts[LAST_EMPTY_CELL_ANSWER]

                // Act
                game.onCellClick(LAST_EMPTY_CELL)
                game.onDigitClick(LAST_EMPTY_CELL_ANSWER)

                // Assert
                completed.await()
                val after = game.uiState.value!!.digitCounts[LAST_EMPTY_CELL_ANSWER]
                assertEquals(1 to 0, before to after)
                game.onHidden()
            }
        }
}
