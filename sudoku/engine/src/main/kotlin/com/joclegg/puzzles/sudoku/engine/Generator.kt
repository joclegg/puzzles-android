package com.joclegg.puzzles.sudoku.engine

import kotlin.random.Random

data class GeneratedPuzzle(
    val puzzle: Puzzle,
    val grade: Grade,
)

object Generator {
    private val classicFloor =
        mapOf(Difficulty.FAST to 12, Difficulty.EASY to 36, Difficulty.MEDIUM to 30, Difficulty.HARD to 25, Difficulty.EXPERT to 22)
    private val killerFloor =
        mapOf(
            Difficulty.FAST to 4,
            Difficulty.BEGINNER to 32,
            Difficulty.EASY to 24,
            Difficulty.MEDIUM to 16,
            Difficulty.HARD to 8,
            Difficulty.EXPERT to 2,
            Difficulty.EVIL to 0,
        )
    private const val RARE_SINGLES_WEIGHT = 1
    private const val MAX_MERGED_CAGE = 6

    val killerScoreBands =
        mapOf(
            Difficulty.BEGINNER to (0 until 90),
            Difficulty.EASY to (90 until 140),
            Difficulty.MEDIUM to (140 until 300),
            Difficulty.HARD to (300 until 600),
            Difficulty.EXPERT to (600 until 900),
            Difficulty.EVIL to (900 until Int.MAX_VALUE),
        )
    private const val LAYOUT_TRIES = 12

    private class CageProfile(
        val sizeWeights: List<Int>,
        val oneWaySumPreference: Int,
    )

    private val cageProfiles =
        mapOf(
            Difficulty.FAST to CageProfile(sizeWeights = listOf(4, 35, 40, 16, 5), oneWaySumPreference = 0),
            Difficulty.BEGINNER to CageProfile(sizeWeights = listOf(20, 60, 18, 2, 0), oneWaySumPreference = 1),
            Difficulty.EASY to CageProfile(sizeWeights = listOf(12, 58, 25, 5, 0), oneWaySumPreference = 1),
            Difficulty.MEDIUM to CageProfile(sizeWeights = listOf(5, 45, 35, 13, 2), oneWaySumPreference = 0),
            Difficulty.HARD to CageProfile(sizeWeights = listOf(1, 30, 40, 22, 7), oneWaySumPreference = -1),
            Difficulty.EXPERT to CageProfile(sizeWeights = listOf(0, 22, 40, 26, 12), oneWaySumPreference = -1),
            Difficulty.EVIL to CageProfile(sizeWeights = listOf(0, 12, 38, 30, 20), oneWaySumPreference = -1),
        )

    fun classic(
        target: Difficulty,
        random: Random,
        maxAttempts: Int = 50,
    ): GeneratedPuzzle? =
        attempt(maxAttempts) {
            val grid = target.grid
            val solution = Solver.randomGrid(random, grid)
            val pairs = (0 until (grid.cells + 1) / 2).map { listOf(it, grid.cells - 1 - it).distinct() }.shuffled(random)
            dig(solution, emptyList(), pairs, target, classicFloor.getValue(target))
        }

    fun killer(
        target: Difficulty,
        random: Random,
        maxAttempts: Int = 50,
        anyScore: Boolean = false,
    ): GeneratedPuzzle? =
        attempt(maxAttempts) {
            val solution = Solver.randomGrid(random, target.grid)
            val cages = cagesFor(target, solution, random)
            val cells = solution.indices.map { listOf(it) }.shuffled(random)
            dig(solution, cages, cells, target, killerFloor.getValue(target), killerScoreBands[target].takeUnless { anyScore })
        }

    private fun attempt(
        maxAttempts: Int,
        block: () -> GeneratedPuzzle?,
    ): GeneratedPuzzle? {
        repeat(maxAttempts) { block()?.let { return it } }
        return null
    }

    private fun dig(
        solution: List<Int>,
        cages: List<Cage>,
        groups: List<List<Int>>,
        target: Difficulty,
        floor: Int,
        killerScoreBand: IntRange? = null,
        killer: Boolean = cages.isNotEmpty(),
    ): GeneratedPuzzle? {
        val givens = solution.toIntArray()
        var count = solution.size
        for (group in groups) {
            if (count - group.size < floor) {
                continue
            }
            group.forEach { givens[it] = 0 }
            if (Grader(givens.toList(), cages).grade(maxOf(target, Difficulty.EASY)).solved) {
                count -= group.size
            } else {
                group.forEach { givens[it] = solution[it] }
            }
        }
        val grade = Grader(givens.toList(), cages).grade()

        val accepted =
            when {
                killer -> grade.solved && (killerScoreBand == null || grade.score in killerScoreBand)
                target == Difficulty.FAST -> grade.solved
                else -> grade.level == target
            }
        return if (accepted) {
            GeneratedPuzzle(Puzzle(givens.toList(), solution, cages), grade)
        } else {
            null
        }
    }

    fun cagesFor(
        target: Difficulty,
        solution: List<Int>,
        random: Random,
    ): List<Cage> {
        val profile = cageProfiles.getValue(target)
        return List(LAYOUT_TRIES) { randomCages(solution, random, profile.sizeWeights) }
            .maxBy { layout -> profile.oneWaySumPreference * oneWaySumShare(layout, Grid.forCellCount(solution.size)) }
    }

    fun randomCages(
        solution: List<Int>,
        random: Random,
        sizeWeights: List<Int> = cageProfiles.getValue(Difficulty.MEDIUM).sizeWeights,
    ): List<Cage> {
        val grid = Grid.forCellCount(solution.size)
        val owner = IntArray(solution.size) { NO_CAGE }
        val cages = mutableListOf<MutableList<Int>>()
        for (seed in solution.indices.shuffled(random)) {
            if (owner[seed] != NO_CAGE) {
                continue
            }
            val target = pickSize(sizeWeights, random)
            val cage = mutableListOf(seed)
            owner[seed] = cages.size
            while (cage.size < target) {
                val next =
                    cage
                        .flatMap { grid.neighbours(it) }
                        .filter { owner[it] == NO_CAGE && cage.none { c -> solution[c] == solution[it] } }
                        .distinct()
                        .randomOrNull(random) ?: break
                owner[next] = cages.size
                cage += next
            }
            cages += cage
        }
        if (sizeWeights.first() <= RARE_SINGLES_WEIGHT) {
            mergeOrphans(cages, owner, solution, random)
        }
        return cages.filter { it.isNotEmpty() }.map { cells -> Cage(cells.sorted(), cells.sumOf { solution[it] }) }
    }

    private fun mergeOrphans(
        cages: MutableList<MutableList<Int>>,
        owner: IntArray,
        solution: List<Int>,
        random: Random,
    ) {
        val grid = Grid.forCellCount(solution.size)
        for (orphan in cages.indices) {
            if (cages[orphan].size != 1) {
                continue
            }
            val cell = cages[orphan].single()
            val home =
                grid
                    .neighbours(cell)
                    .map { owner[it] }
                    .distinct()
                    .filter { other -> cages[other].size < MAX_MERGED_CAGE && cages[other].none { solution[it] == solution[cell] } }
                    .randomOrNull(random) ?: continue
            cages[home] += cell
            owner[cell] = home
            cages[orphan].clear()
        }
    }

    fun oneWaySumShare(
        cages: List<Cage>,
        grid: Grid = Grid.STANDARD,
    ): Double = cages.count { it.cells.size > 1 && CageCombos.of(it.cells.size, it.sum, grid.allDigits).size == 1 }.toDouble() / cages.size

    private fun pickSize(
        sizeWeights: List<Int>,
        random: Random,
    ): Int {
        var roll = random.nextInt(sizeWeights.sum())
        sizeWeights.forEachIndexed { index, weight ->
            if (roll < weight) {
                return index + 1
            }
            roll -= weight
        }
        return 1
    }
}
