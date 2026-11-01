package com.joclegg.puzzles.kakuro.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class HintTest {
    private val small = Puzzle(size = 3, solution = listOf(0, 0, 0, 0, 1, 2, 0, 3, 4))

    @Test
    fun `given a fresh puzzle when a hint is asked for then it points at the cell where only one digit fits`() {
        // Act
        val hint = GameState(small).findHint()

        // Assert
        assertEquals(Hint(4, 1, "Only 1 fits here: 3 in 2 across and 4 in 2 down leave no other digit."), hint)
    }

    @Test
    fun `given no cell can be worked out when the fallback hint is asked for then it names the runs and the digits they allow`() {
        // Act
        val hint = GameState(small).fewestCandidatesHint()!!

        // Assert
        assertEquals(
            listOf(4, 1, true, true, true, false),
            listOf(
                hint.cell,
                hint.digit,
                "across run (3 in 2)" in hint.message,
                "down run (4 in 2)" in hint.message,
                hint.message.endsWith("only allow 1."),
                "first empty" in hint.message,
            ),
        )
    }

    @Test
    fun `given a wrong digit when a hint is asked for then it points at that cell first`() {
        // Arrange
        val game = GameState(small).copy(values = listOf(0, 0, 0, 0, 0, 0, 0, 0, 9))

        // Act
        val hint = game.findHint()

        // Assert
        assertEquals("This cell is wrong.", hint?.message)
    }

    @Test
    fun `given a hint when it is placed then the cell holds its answer`() {
        // Arrange
        val game = GameState(small)

        // Act
        val placed = game.placeHint(game.findHint()!!)

        // Assert
        assertEquals(1, placed.values[4])
    }

    @Test
    fun `given a puzzle when the solution is revealed then every cell holds its answer`() {
        // Act
        val revealed = GameState(small).revealSolution()

        // Assert
        assertEquals(small.solution, revealed.values)
    }
}
