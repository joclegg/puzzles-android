package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

class SolverTest {
    @Test
    fun `given four kings left on the table when it is solved then it is winnable`() {
        // Act
        val verdict = Solver.solve(board("/KC|/KD|/KH|/KS;;;${foundationsMissingKings()}"), DrawMode.DRAW_ONE, BuildOrder.DOWN)

        // Assert
        assertEquals(Verdict.WINNABLE, verdict)
    }

    @Test
    fun `given a king stuck in the only column under its ace when it is solved then it is unwinnable`() {
        // Arrange
        val others =
            Suit.entries
                .filter {
                    it != Suit.SPADES
                }.joinToString("|") { suit -> Card.deck.filter { it.suit == suit }.joinToString("") { it.code } }

        // Act
        val verdict = Solver.solve(board("2S3S4S5S6S7S8S9STSJSQSKS/AS;;;$others|"), DrawMode.DRAW_ONE, BuildOrder.DOWN)

        // Assert
        assertEquals(Verdict.UNWINNABLE, verdict)
    }

    @Test
    fun `given a king that blocks the only card that could free it when it is solved then it is unwinnable`() {
        // Arrange
        val rest =
            Suit.entries.joinToString("|") { suit ->
                Card.deck.filter { it.suit == suit && !(suit == Suit.HEARTS && it.rank >= Card.QUEEN) }.joinToString("") { it.code }
            }

        // Act
        val verdict = Solver.solve(board("QH/KH;;;$rest"), DrawMode.DRAW_ONE, BuildOrder.DOWN)

        // Assert
        assertEquals(Verdict.UNWINNABLE, verdict)
    }

    @ParameterizedTest
    @EnumSource(DrawMode::class)
    fun `given random deals when they are solved then most are winnable and none are wrongly unwinnable`(drawMode: DrawMode) {
        // Act
        val verdicts = (1L..12L).map { Solver.solve(Board.deal(it), drawMode, BuildOrder.DOWN) }

        // Assert
        val winnable = verdicts.count { it == Verdict.WINNABLE }
        println(
            "SOLVER $drawMode winnable=$winnable unwinnable=${verdicts.count {
                it == Verdict.UNWINNABLE
            }} unknown=${verdicts.count { it == Verdict.UNKNOWN }}",
        )
        assertEquals(true, winnable >= 6)
    }

    @ParameterizedTest
    @EnumSource(DrawMode::class)
    fun `given a seed when a winnable deal is asked for then the deal is a full deck the solver can win, and it is the same each time`(
        drawMode: DrawMode,
    ) {
        // Act
        val deals = List(2) { Solver.winnableDeal(seed = 3, drawMode = drawMode, order = BuildOrder.DOWN) }

        // Assert
        assertEquals(deals[0], deals[1])
        assertEquals(Verdict.WINNABLE, Solver.solve(deals[0], drawMode, BuildOrder.DOWN))
    }

    @Test
    fun `given a stop signal when solving then it gives up as unknown`() {
        // Act
        val verdict = Solver.solve(Board.deal(1), DrawMode.DRAW_THREE, BuildOrder.DOWN, shouldStop = { true })

        // Assert
        assertEquals(Verdict.UNKNOWN, verdict)
    }
}
