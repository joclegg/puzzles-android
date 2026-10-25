package com.joclegg.puzzles.sightlines.engine

import kotlin.random.Random

object Generator {
    private const val RED_CHANCE = 0.3

    fun generate(
        size: Int,
        seed: Long,
    ): Puzzle {
        val random = Random(seed)
        val solution = solution(size, random)
        var numbers =
            solution.dots.indices
                .filter { solution.dots[it] == Dot.BLUE }
                .associateWith { solution.seen(it) }
        var walls =
            solution.dots.indices
                .filter { solution.dots[it] == Dot.RED }
                .toSet()
        for (cell in solution.dots.indices.shuffled(random)) {
            val fewerNumbers = numbers - cell
            val fewerWalls = walls - cell
            val candidate = Puzzle(solution, fewerNumbers, fewerWalls)
            if (Solver.solveLogically(candidate.givens, fewerNumbers) == solution) {
                numbers = fewerNumbers
                walls = fewerWalls
            }
        }
        return Puzzle(solution, numbers, walls)
    }

    private fun solution(
        size: Int,
        random: Random,
    ): Grid {
        var grid =
            Grid(
                size,
                List(size * size) {
                    if (random.nextDouble() < RED_CHANCE) {
                        Dot.RED
                    } else {
                        Dot.BLUE
                    }
                },
            )
        while (true) {
            val lonely = grid.dots.indices.firstOrNull { grid.dots[it] == Dot.BLUE && grid.seen(it) == 0 } ?: return grid
            grid = grid.with(lonely, Dot.RED)
        }
    }
}
