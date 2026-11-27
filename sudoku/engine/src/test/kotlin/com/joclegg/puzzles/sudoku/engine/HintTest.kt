package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class HintTest {
    private val game = GameState(easyPuzzle)

    @Test
    fun `given a fresh puzzle when a hint is asked for then it names a cell and its answer`() {
        // Act
        val hint = game.findHint()!!

        // Assert
        assertEquals(easyPuzzle.solution!![hint.cell], hint.digit)
    }

    @Test
    fun `given no cell can be worked out when the fallback hint is asked for then it names the digits the cell can be`() {
        // Arrange
        val wording = Regex("^This cell can only be \\d(, \\d)*( or \\d)?: everything else is in its row, column or box\\.$")

        // Act
        val hint = game.fewestCandidatesHint()!!

        // Assert
        assertEquals(
            listOf(easyPuzzle.solution!![hint.cell], true, false),
            listOf(
                hint.digit,
                wording.matches(hint.message),
                "first empty" in hint.message,
            ),
        )
    }

    @Test
    fun `given a wrong digit when a hint is asked for then it points at that cell first`() {
        // Arrange
        val cell = easyPuzzle.givens.indexOfFirst { it == 0 }
        val wrongDigit = easyPuzzle.solution!![cell] % 9 + 1
        val wrong = game.copy(values = game.values.toMutableList().apply { this[cell] = wrongDigit })

        // Act
        val hint = wrong.findHint()

        // Assert
        assertEquals(Hint(cell, easyPuzzle.solution!![cell], "This cell is wrong."), hint)
    }

    @Test
    fun `given a hint when it is placed then the cell holds its answer`() {
        // Arrange
        val hint = game.findHint()!!

        // Act
        val placed = game.placeHint(hint)

        // Assert
        assertEquals(hint.digit, placed.values[hint.cell])
    }

    @ParameterizedTest
    @MethodSource("noteCases")
    fun `given other cells noted with their candidates when the fallback hint is asked for then covered cells are skipped`(
        notesOf: (Int) -> Int,
        expectHinted: Boolean,
    ) {
        // Arrange
        val empty = game.values.indices.filter { game.values[it] == 0 }
        val target = empty.first { Integer.bitCount(candidatesOf(it)) >= 2 }
        val notes = game.notes.toMutableList()
        empty.forEach { notes[it] = candidatesOf(it) }
        notes[target] = notesOf(candidatesOf(target))

        // Act
        val hint = game.copy(notes = notes).fewestCandidatesHint()!!

        // Assert
        if (expectHinted) {
            assertEquals(listOf(target, true), listOf(hint.cell, hint.message.startsWith("This cell can only be")))
        } else {
            assertEquals("This one is the ${easyPuzzle.solution!![hint.cell]}.", hint.message)
        }
    }

    private fun candidatesOf(cell: Int): Int =
        easyPuzzle.grid.peers[cell].fold(easyPuzzle.grid.allDigits) { mask, peer -> mask and digitBit(game.values[peer]).inv() }

    companion object {
        @JvmStatic
        fun noteCases(): List<Arguments> =
            listOf(
                Arguments.of({ _: Int -> 0 }, true),
                Arguments.of({ candidates: Int -> candidates or easyPuzzle.grid.allDigits }, true),
                Arguments.of({ candidates: Int -> candidates }, false),
                Arguments.of({ candidates: Int -> Integer.lowestOneBit(candidates) }, false),
            )
    }
}
