package com.joclegg.puzzles.solitaire.feature.game

import com.joclegg.puzzles.core.testing.TestCompletionSource
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.GameStatus
import com.joclegg.puzzles.solitaire.core.data.SavedGame
import com.joclegg.puzzles.solitaire.core.data.Settings
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.data.WinnableDeals
import com.joclegg.puzzles.solitaire.engine.BoardCodec
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.Suit
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal const val GAME_ID = 1L

internal class GameViewModelHarness {
    val settings = MutableStateFlow(Settings())
    val saves = mutableListOf<SavedGame>()
    val won = TestCompletionSource<SavedGame>()
    var clockMs = 0L

    val games =
        mockk<GameRepository> {
            coEvery { get(GAME_ID) } answers { saves.lastOrNull() ?: oneMoveToWin() }
            coEvery { save(any()) } answers { saves += firstArg<SavedGame>() }
            coEvery { win(any()) } answers {
                won.complete(firstArg())
                true
            }
        }

    private val settingsRepository = mockk<SettingsRepository> { every { settings } returns this@GameViewModelHarness.settings }

    suspend fun openGame(): GameViewModel =
        GameViewModel(GAME_ID, games, settingsRepository, {
            clockMs
        }, { 7L }, WinnableDeals { "7\n" }, solving = Dispatchers.Main).also { it.uiState.filterNotNull().first() }

    private fun oneMoveToWin(): SavedGame {
        val foundations =
            Suit.entries.joinToString("|") { suit ->
                Card.deck.filter { it.suit == suit && (suit != Suit.SPADES || it.rank < Card.KING) }.joinToString("") { it.code }
            }
        return SavedGame(
            id = GAME_ID,
            deal = DealRef("t", DrawMode.DRAW_ONE, BoardCodec.decode("/KS|/;;;$foundations")),
            moves = emptyList(),
            elapsedMs = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = 0,
            finishedAt = null,
        )
    }
}
