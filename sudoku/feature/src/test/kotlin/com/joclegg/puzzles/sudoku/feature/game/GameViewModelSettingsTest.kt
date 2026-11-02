package com.joclegg.puzzles.sudoku.feature.game

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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class GameViewModelSettingsTest(
    private val name: String,
    private val turnOff: (ModeSettings) -> ModeSettings,
    private val read: (GameUiState) -> Boolean,
) {
    private val dispatcher = UnconfinedTestDispatcher()
    private val harness = GameViewModelHarness()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `given an open game when a display setting is turned off then the screen state follows straight away`() =
        runTest(dispatcher) {
            with(harness) {
                // Arrange
                val game = openGame()

                // Act
                settings.value = Settings(classic = turnOff(ModeSettings()))

                // Assert
                assertEquals(false, read(game.uiState.first { it != null && !read(it) }!!))
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any>("showTimer", { it: ModeSettings -> it.copy(showTimer = false) }, { state: GameUiState -> state.showTimer }),
                arrayOf<Any>(
                    "showMistakeCount",
                    { it: ModeSettings -> it.copy(showMistakeCount = false) },
                    { state: GameUiState -> state.showMistakeCount },
                ),
                arrayOf<Any>(
                    "boldPlacedDigits",
                    { it: ModeSettings -> it.copy(boldPlacedDigits = false) },
                    { state: GameUiState -> state.board.boldPlacedDigits },
                ),
                arrayOf<Any>(
                    "greyGivenCells",
                    { it: ModeSettings -> it.copy(greyGivenCells = false) },
                    { state: GameUiState -> state.board.greyGivenCells },
                ),
            )
    }
}
