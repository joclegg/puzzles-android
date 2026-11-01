package com.joclegg.puzzles.solitaire.feature.game

import com.joclegg.puzzles.solitaire.core.data.Settings
import com.joclegg.puzzles.solitaire.engine.Pile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                game.onHidden()

                // Assert
                assertEquals(7_000, saves.last().elapsedMs)
            }
        }

    @Test
    fun `given one card left when it goes to its foundation then the win is saved with its time`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val game = openGame()
                game.onVisible()
                clockMs = 42_000

                // Act
                game.onCardClick(Pile.Tableau(0), 0)

                // Assert
                assertEquals(42_000, won.await().elapsedMs)
                assertEquals(Triple(true, true, true), game.uiState.value!!.let { Triple(it.won, it.newBest, it.resultSaved) })
            }
        }

    @Test
    fun `given a move made and the game hidden when it is reopened and undone then the move is gone`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val first = openGame()
                first.onCardClick(Pile.Tableau(0), 0)
                first.onUndo()
                first.onStockClick()
                first.onHidden()
                val reopened = openGame()

                // Act
                reopened.onUndo()

                // Assert
                assertEquals(0, reopened.uiState.value!!.moveCount)
            }
        }

    @Test
    fun `given the timer and move counter turned off when the game is shown then both are hidden`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val game = openGame()

                // Act
                settings.value = Settings(showTimer = false, showMoveCount = false)

                // Assert
                assertEquals(false to false, game.uiState.value!!.let { it.showTimer to it.showMoveCount })
            }
        }
}
