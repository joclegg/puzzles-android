package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class CageBuilderTest {
    private val topLeft = Grid.STANDARD.cellAt(0, 0)
    private val rightOfTopLeft = Grid.STANDARD.cellAt(0, 1)
    private val belowTopLeft = Grid.STANDARD.cellAt(1, 0)
    private val farAway = Grid.STANDARD.cellAt(5, 5)
    private val pair = CageBuilder().tapCell(topLeft).tapCell(rightOfTopLeft)

    @Test
    fun `given a selection when neighbouring cells are tapped then the selection grows`() {
        // Arrange
        val started = CageBuilder().tapCell(topLeft)

        // Act
        val grown = started.tapCell(rightOfTopLeft).tapCell(belowTopLeft)

        // Assert
        assertEquals(listOf(topLeft, rightOfTopLeft, belowTopLeft), grown.selection)
    }

    @Test
    fun `given a selection when a cell that doesn't touch it is tapped then a new selection starts`() {
        // Act
        val next = pair.tapCell(farAway)

        // Assert
        assertEquals(listOf(farAway), next.selection)
    }

    @Test
    fun `given two selected cells when 17 is typed and confirmed then a cage is saved`() {
        // Act
        val built = pair.typeDigit(1).typeDigit(7).confirm()

        // Assert
        assertEquals(listOf(Cage(listOf(topLeft, rightOfTopLeft), 17)) to emptyList(), built.cages to built.selection)
    }

    @ParameterizedTest
    @MethodSource("typedSums")
    fun `given digits typed for a sum when the sum is read then it keeps at most two digits`(
        typed: List<Int>,
        expected: String,
    ) {
        // Act
        val sum = typed.fold(CageBuilder()) { builder, digit -> builder.typeDigit(digit) }.sumInput

        // Assert
        assertEquals(expected, sum)
    }

    @ParameterizedTest
    @MethodSource("problems")
    fun `given a selection and sum when it is checked then impossible cages are refused`(
        builder: CageBuilder,
        expected: CageProblem?,
    ) {
        // Act
        val problem = builder.problem()

        // Assert
        assertEquals(expected, problem)
    }

    @Test
    fun `given a refused sum when new digits are typed then the sum starts again`() {
        // Arrange
        val refused = pair.typeDigit(1).confirm()

        // Act
        val retyped = refused.typeDigit(1).typeDigit(7)

        // Assert
        assertEquals("17", retyped.sumInput)
    }

    @Test
    fun `given a saved cage when it is tapped and a new sum typed then the cage is updated`() {
        // Arrange
        val editing = pair.typeDigit(9).confirm().tapCell(rightOfTopLeft)

        // Act
        val updated = editing.typeDigit(1).typeDigit(1).confirm()

        // Assert
        assertEquals(listOf(Cage(listOf(topLeft, rightOfTopLeft), 11)), updated.cages)
    }

    @Test
    fun `given a saved cage when it is tapped and deleted then it is gone`() {
        // Arrange
        val editing = pair.typeDigit(9).confirm().tapCell(rightOfTopLeft)

        // Act
        val deleted = editing.deleteSelection()

        // Assert
        assertEquals(emptyList(), deleted.cages)
    }

    companion object {
        @JvmStatic
        fun typedSums() =
            listOf(
                Arguments.of(listOf(1), "1"),
                Arguments.of(listOf(1, 7), "17"),
                Arguments.of(listOf(1, 2, 4), "4"),
            )

        @JvmStatic
        fun problems(): List<Arguments> {
            val topLeft = Grid.STANDARD.cellAt(0, 0)
            val next = Grid.STANDARD.cellAt(0, 1)
            return listOf(
                Arguments.of(CageBuilder(selection = listOf(topLeft, next), sumInput = "3"), null),
                Arguments.of(CageBuilder(selection = listOf(topLeft, next), sumInput = "2"), CageProblem.IMPOSSIBLE_SUM),
                Arguments.of(CageBuilder(selection = listOf(topLeft, next), sumInput = "18"), CageProblem.IMPOSSIBLE_SUM),
                Arguments.of(
                    CageBuilder(selection = listOf(topLeft, Grid.STANDARD.cellAt(5, 5)), sumInput = "9"),
                    CageProblem.NOT_CONNECTED,
                ),
                Arguments.of(CageBuilder(selection = listOf(topLeft, next)), CageProblem.EMPTY),
            )
        }
    }
}
