package com.joclegg.puzzles.sightlines.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class SolverTest {
    @ParameterizedTest
    @MethodSource("deductions")
    fun `given a grid when the next deduction is found then it names the dots and the rule`(
        text: String,
        numbers: Map<Int, Int>,
        expected: Deduction,
    ) {
        // Act
        val deduction = Solver.next(Grid.parse(4, text), numbers)

        // Assert
        assertEquals(expected, deduction)
    }

    companion object {
        @JvmStatic
        fun deductions() =
            listOf(
                Arguments.of("BB.. .... .... ....", mapOf(0 to 1), Deduction(listOf(4, 2), Dot.RED, Reason.SATISFIED, 0)),
                Arguments.of("B.B. B... .... ....", mapOf(0 to 2), Deduction(listOf(1), Dot.RED, Reason.TOO_MANY, 0)),
                Arguments.of("B... R... .... ....", mapOf(0 to 3), Deduction(listOf(1), Dot.BLUE, Reason.NEEDS, 0)),
                Arguments.of(".R.. R... .... ....", emptyMap<Int, Int>(), Deduction(listOf(0), Dot.RED, Reason.ALONE, 0)),
            )
    }
}
