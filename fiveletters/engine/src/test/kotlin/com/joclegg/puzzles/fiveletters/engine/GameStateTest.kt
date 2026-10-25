package com.joclegg.puzzles.fiveletters.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.assertEquals

class GameStateTest {
    private val words = Words(answers = listOf("crane", "slate", "trace"), guesses = listOf("adieu", "audio", "crate", "react", "stale"))

    private fun GameState.enter(word: String) = word.fold(this) { state, letter -> state.type(letter) }.submit(words)

    @Test
    fun `given a typed word when it is submitted then it becomes a guess and typing clears`() {
        // Act
        val after = GameState("CRANE").enter("adieu")

        // Assert
        assertEquals(listOf("ADIEU") to "", after.guesses to after.typing)
    }

    @ParameterizedTest
    @CsvSource("cra,TOO_SHORT", "zzzzz,NOT_A_WORD")
    fun `given an unacceptable word when it is submitted then it is rejected and kept`(
        word: String,
        rejection: Rejection,
    ) {
        // Act
        val after = GameState("CRANE").enter(word)

        // Assert
        assertEquals(Triple(emptyList<String>(), word.uppercase(), rejection), Triple(after.guesses, after.typing, after.rejection))
    }

    @Test
    fun `given hard mode and a green letter when a guess drops it then it is rejected`() {
        // Arrange
        val game = GameState("CRANE", hardMode = true).enter("crate")

        // Act
        val after = game.enter("adieu")

        // Assert
        assertEquals(Rejection.MUST_USE_HINTS, after.rejection)
    }

    @Test
    fun `given hard mode when a guess keeps every hint then it is accepted`() {
        // Arrange
        val game = GameState("CRANE", hardMode = true).enter("react")

        // Act
        val after = game.enter("trace")

        // Assert
        assertEquals(2, after.guesses.size)
    }

    @Test
    fun `given the answer when it is guessed then the game is won and further typing does nothing`() {
        // Arrange
        val game = GameState("CRANE").enter("adieu").enter("crane")

        // Act
        val after = game.type('a')

        // Assert
        assertEquals(true to "", after.isWon to after.typing)
    }

    @Test
    fun `given six wrong guesses when the last is submitted then the game is lost`() {
        // Act
        val after = (1..6).fold(GameState("CRANE")) { state, _ -> state.enter("adieu") }

        // Assert
        assertEquals(true, after.isLost)
    }

    @Test
    fun `given a won game when it is shared then the grid shows colours and no letters`() {
        // Arrange
        val game = GameState("CRANE").enter("trace").enter("crane")

        // Act
        val text = game.shareText("Five Letters 1")

        // Assert
        assertEquals("Five Letters 1 2/6\n\n⬜🟩🟩🟨🟩\n🟩🟩🟩🟩🟩", text)
    }

    @Test
    fun `given guesses when the keyboard is coloured then each letter shows its best mark`() {
        // Act
        val marks = GameState("CRANE").enter("react").enter("trace").letterMarks

        // Assert
        assertEquals(Mark.CORRECT, marks['R'])
        assertEquals(Mark.ABSENT, marks['T'])
    }

    @Test
    fun `given the same date when the daily word is picked twice then it matches, and the next day differs`() {
        // Arrange
        val big = Words((1..50).map { "w${it.toString().padStart(4, 'a')}".take(5) }, emptyList())
        val day = LocalDate.of(2026, 11, 1)

        // Act
        val words = listOf(big.daily(day), big.daily(day), big.daily(day.plusDays(1)))

        // Assert
        assertEquals(words[0], words[1])
        assertEquals(true, words[0] != words[2])
        assertEquals(8, Words.dayNumber(day))
    }
}
