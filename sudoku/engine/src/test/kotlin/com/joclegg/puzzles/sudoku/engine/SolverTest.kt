package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolverTest {
    @Test
    fun `given a classic puzzle when it is solved then the known solution is found`() {
        // Arrange
        val solver = Solver(digits(EASY_GIVENS))

        // Act
        val solution = solver.solve()

        // Assert
        assertEquals(digits(EASY_SOLUTION), solution)
    }

    @ParameterizedTest
    @CsvSource(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079, 1",
        "500000000000000000000000000000000000000000000000000000000000000000000000000000000, 2",
        "550000000000000000000000000000000000000000000000000000000000000000000000000000000, 0",
    )
    fun `given some givens when solutions are counted up to two then unique, several and clashing grids are told apart`(
        givens: String,
        expectedCount: Int,
    ) {
        // Arrange
        val solver = Solver(digits(givens))

        // Act
        val count = solver.countSolutions(limit = 2)

        // Assert
        assertEquals(expectedCount, count)
    }

    @Test
    fun `given clashing givens when the solver is created then it is inconsistent and has no solution`() {
        // Arrange
        val clashing =
            List(Grid.STANDARD.cells) {
                if (it == 0 || it == 1) {
                    5
                } else {
                    0
                }
            }

        // Act
        val solver = Solver(clashing)

        // Assert
        assertEquals(false to null, solver.consistent to solver.solve())
    }

    @ParameterizedTest
    @CsvSource("1", "2", "3")
    fun `given a seed when a random grid is made then it is a valid full grid`(seed: Int) {
        // Arrange
        val random = Random(seed)

        // Act
        val grid = Solver.randomGrid(random)

        // Assert
        assertEquals(emptySet(), conflicts(grid))
    }

    @Test
    fun `given random killer cages when the empty grid is solved then every cage adds up`() {
        // Arrange
        val cages = Generator.randomCages(digits(EASY_SOLUTION), Random(7))

        // Act
        val solved = assertNotNull(Solver(List(Grid.STANDARD.cells) { 0 }, cages).solve())

        // Assert
        assertTrue(cages.all { cage -> cage.cells.sumOf { solved[it] } == cage.sum } && conflicts(solved, cages).isEmpty())
    }

    @ParameterizedTest
    @CsvSource("2, 3, 1", "2, 17, 1", "2, 10, 4", "2, 2, 0", "9, 45, 1")
    fun `given a cage size and sum when combinations are listed then the count of digit sets is right`(
        size: Int,
        sum: Int,
        expected: Int,
    ) {
        // Act
        val combinations = CageCombos.of(size, sum)

        // Assert
        assertEquals(expected, combinations.size)
    }

    @ParameterizedTest
    @CsvSource("10, true", "1000000, false")
    fun `given a step budget when solutions are counted then the solver gives up only if the budget runs out`(
        maxSteps: Long,
        expectGiveUp: Boolean,
    ) {
        // Arrange
        val solver = Solver(List(Grid.STANDARD.cells) { 0 })

        // Act
        val count = solver.countSolutionsWithin(limit = 2, maxSteps = maxSteps)

        // Assert
        if (expectGiveUp) {
            assertNull(count)
        } else {
            assertEquals(2, count)
        }
    }
}
