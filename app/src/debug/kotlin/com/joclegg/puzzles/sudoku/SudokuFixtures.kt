package com.joclegg.puzzles.sudoku

import com.joclegg.puzzles.sudoku.core.data.DailyIds
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.PuzzleRef
import com.joclegg.puzzles.sudoku.engine.Cage
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.Puzzle
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import java.time.LocalDate

object SudokuFixtures {
    const val DRAFT_EXTRA = "draft"

    fun draft(name: String?): String? =
        when (name) {
            "classic_easy_missing_r1c1" -> "CLASSIC#" + PuzzleCodec.encode(Puzzle(GIVENS.map { it.digitToInt() }.blank(0), null))
            else -> null
        }

    private const val GIVENS = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private const val FAST_SOLUTION = "123456456123231564564231312645645312"
    private const val SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    private val solution = SOLUTION.map { it.digitToInt() }
    private val fastSolution = FAST_SOLUTION.map { it.digitToInt() }

    private val rowPairCages =
        (0 until 9)
            .flatMap { row ->
                listOf(0, 2, 4, 6).map { col -> listOf(row * 9 + col, row * 9 + col + 1) } + listOf(listOf(row * 9 + 8))
            }.map { cells -> Cage(cells, cells.sumOf { solution[it] }) }

    fun puzzle(name: String?): PuzzleRef? =
        when (name) {
            "classic_one_left" -> {
                ref(name, GameMode.CLASSIC, solution.blank(2))
            }

            "classic_easy" -> {
                ref(name, GameMode.CLASSIC, GIVENS.map { it.digitToInt() })
            }

            "killer_one_cage" -> {
                ref(name, GameMode.KILLER, solution.blank(0, 1), rowPairCages)
            }

            "fast_one_left" -> {
                PuzzleRef("fixture:$name", GameMode.CLASSIC, Difficulty.FAST, Puzzle(fastSolution.blank(2), fastSolution))
            }

            "daily_classic_one_left" -> {
                ref(
                    name,
                    GameMode.CLASSIC,
                    solution.blank(2),
                ).copy(id = DailyIds.of(GameMode.CLASSIC, Difficulty.EASY, LocalDate.now()))
            }

            else -> {
                null
            }
        }

    private fun ref(
        name: String,
        mode: GameMode,
        givens: List<Int>,
        cages: List<Cage> = emptyList(),
    ) = PuzzleRef("fixture:$name", mode, Difficulty.EASY, Puzzle(givens, solution, cages))

    private fun List<Int>.blank(vararg cells: Int) =
        mapIndexed { i, digit ->
            if (i in cells) {
                0
            } else {
                digit
            }
        }
}
