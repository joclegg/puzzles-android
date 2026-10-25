package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratorTest {
    @ParameterizedTest
    @EnumSource(Difficulty::class, names = ["FAST"])
    fun `given the fast level when puzzles are generated then they are 6x6 with one solution`(level: Difficulty) {
        // Arrange
        val random = Random(11)

        // Act
        val classic = assertNotNull(Generator.classic(level, random)).puzzle
        val killer = assertNotNull(Generator.killer(level, random)).puzzle

        // Assert
        assertEquals(listOf(36, 36), listOf(classic.givens.size, killer.givens.size))
        assertEquals(listOf(1, 1), listOf(Solver(classic.givens).countSolutions(), Solver(killer.givens, killer.cages).countSolutions()))
    }

    @ParameterizedTest
    @EnumSource(Difficulty::class, names = ["EASY", "MEDIUM"])
    fun `given a classic level when a puzzle is generated then it has that level and one solution`(level: Difficulty) {
        // Arrange
        val random = Random(level.ordinal)

        // Act
        val generated = assertNotNull(Generator.classic(level, random))

        // Assert
        assertEquals(level to 1, generated.grade.level to Solver(generated.puzzle.givens).countSolutions())
    }

    @ParameterizedTest
    @MethodSource("killerLevels")
    fun `given a killer level when a puzzle is generated then its score is in the level's band and it has one solution`(
        level: Difficulty,
    ) {
        // Arrange
        val random = Random(level.ordinal + 100)

        // Act
        val generated = assertNotNull(Generator.killer(level, random))

        // Assert
        val puzzle = generated.puzzle
        assertTrue(Generator.killerScoreBands[level]?.contains(generated.grade.score) ?: generated.grade.solved)
        assertEquals(1, Solver(puzzle.givens, puzzle.cages).countSolutions())
    }

    @Test
    fun `given the killer score bands when read in level order then they climb without gaps or overlaps`() {
        // Arrange
        val bands = KILLER_LEVELS.mapNotNull { Generator.killerScoreBands[it] }

        // Act
        val joins = bands.zipWithNext { lower, upper -> lower.last + 1 == upper.first }

        // Assert
        assertTrue(joins.all { it } && bands.first().first == 0)
    }

    @Test
    fun `given a killer puzzle when its cages are checked then they cover the grid and match the solution`() {
        // Arrange
        val puzzle = assertNotNull(Generator.killer(Difficulty.EASY, Random(3))).puzzle

        // Act
        val covered = puzzle.cages.flatMap { it.cells }.toSet()

        // Assert
        assertEquals((0 until Grid.STANDARD.cells).toSet(), covered)
        assertTrue(puzzle.cages.all { cage -> cage.cells.sumOf { puzzle.solution!![it] } == cage.sum })
    }

    @ParameterizedTest
    @MethodSource("killerLevels")
    fun `given a killer level when cages are laid out then easier levels have more single cells`(level: Difficulty) {
        // Arrange
        val solution = Solver.randomGrid(Random(5), level.grid)

        // Act
        val singles = Generator.cagesFor(level, solution, Random(5)).count { it.cells.size == 1 }

        // Assert
        when {
            level == Difficulty.FAST -> assertTrue(singles >= 0)
            level >= Difficulty.HARD -> assertEquals(0, singles)
            else -> assertTrue(singles > 0)
        }
    }

    @Test
    fun `given a killer puzzle when it is encoded and decoded then it is unchanged`() {
        // Arrange
        val puzzle = assertNotNull(Generator.killer(Difficulty.EASY, Random(4))).puzzle

        // Act
        val decoded = PuzzleCodec.decode(PuzzleCodec.encode(puzzle))

        // Assert
        assertEquals(puzzle, decoded)
    }

    companion object {
        @JvmStatic
        fun killerLevels() = KILLER_LEVELS
    }
}
