package com.joclegg.puzzles.sudoku.feature.game

import com.joclegg.puzzles.core.testing.TestCompletionSource
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.GameRepository
import com.joclegg.puzzles.sudoku.core.data.GameStatus
import com.joclegg.puzzles.sudoku.core.data.PuzzleRef
import com.joclegg.puzzles.sudoku.core.data.SavedGame
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Puzzle
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal const val GAME_ID = 1L
internal const val LAST_EMPTY_CELL = 2
internal const val LAST_EMPTY_CELL_ANSWER = 4

internal class GameViewModelHarness {
    val settings = MutableStateFlow(Settings())
    val saves = mutableListOf<SavedGame>()
    val completed = TestCompletionSource<SavedGame>()
    var clockMs = 0L

    val games =
        mockk<GameRepository> {
            coEvery { get(GAME_ID) } answers { saves.lastOrNull() ?: oneLeftGame() }
            coEvery { save(any()) } answers { saves += firstArg<SavedGame>() }
            coEvery { reveal(any()) } just Runs
            coEvery { complete(any()) } answers {
                completed.complete(firstArg())
                true
            }
        }

    private val settingsRepository = mockk<SettingsRepository> { every { settings } returns this@GameViewModelHarness.settings }

    suspend fun openGame(): GameViewModel =
        GameViewModel(GAME_ID, games, settingsRepository) { clockMs }.also { it.uiState.filterNotNull().first() }

    private fun oneLeftGame(): SavedGame {
        val solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179".map { it.digitToInt() }
        val givens =
            solution.mapIndexed { cell, digit ->
                if (cell == LAST_EMPTY_CELL) {
                    0
                } else {
                    digit
                }
            }
        return SavedGame(
            id = GAME_ID,
            ref = PuzzleRef("t", GameMode.CLASSIC, Difficulty.EASY, Puzzle(givens, solution)),
            values = givens,
            notes = List(givens.size) { 0 },
            elapsedMs = 0,
            mistakes = 0,
            mistakeCheck = MistakeCheck.SHOW_WRONG,
            status = GameStatus.IN_PROGRESS,
            startedAt = 0,
            finishedAt = null,
        )
    }
}
