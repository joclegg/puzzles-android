package com.joclegg.puzzles.sudoku.engine

const val NO_CAGE = -1

fun digitBit(digit: Int) = 1 shl digit

fun singleDigit(mask: Int) = Integer.numberOfTrailingZeros(mask)

fun digitsOf(mask: Int): List<Int> = (1..MAX_DIGIT).filter { mask and digitBit(it) != 0 }

private const val MAX_DIGIT = 9

class Grid private constructor(
    val size: Int,
    val boxRows: Int,
    val boxCols: Int,
) {
    val cells = size * size
    val digits = 1..size
    val allDigits = digits.fold(0) { mask, digit -> mask or digitBit(digit) }
    private val boxesPerRow = size / boxCols

    fun rowOf(cell: Int) = cell / size

    fun colOf(cell: Int) = cell % size

    fun boxOf(cell: Int) = (rowOf(cell) / boxRows) * boxesPerRow + colOf(cell) / boxCols

    fun cellAt(
        row: Int,
        col: Int,
    ) = row * size + col

    val rows: List<IntArray> by lazy { List(size) { r -> IntArray(size) { c -> cellAt(r, c) } } }
    val cols: List<IntArray> by lazy { List(size) { c -> IntArray(size) { r -> cellAt(r, c) } } }
    val boxes: List<IntArray> by lazy {
        List(size) { box -> (0 until cells).filter { boxOf(it) == box }.toIntArray() }
    }
    val houses: List<IntArray> by lazy { rows + cols + boxes }

    val peers: List<IntArray> by lazy {
        List(cells) { cell ->
            (0 until cells)
                .filter { it != cell && (rowOf(it) == rowOf(cell) || colOf(it) == colOf(cell) || boxOf(it) == boxOf(cell)) }
                .toIntArray()
        }
    }

    fun sharesHouse(
        a: Int,
        b: Int,
    ) = rowOf(a) == rowOf(b) || colOf(a) == colOf(b) || boxOf(a) == boxOf(b)

    fun neighbours(cell: Int): List<Int> {
        val last = size - 1
        val (row, col) = rowOf(cell) to colOf(cell)
        return listOfNotNull(
            (cell - size).takeIf { row > 0 },
            (cell + size).takeIf { row < last },
            (cell - 1).takeIf { col > 0 },
            (cell + 1).takeIf { col < last },
        )
    }

    fun emptyValues(): List<Int> = List(cells) { 0 }

    companion object {
        val STANDARD = Grid(size = 9, boxRows = 3, boxCols = 3)
        val FAST = Grid(size = 6, boxRows = 2, boxCols = 3)

        fun forCellCount(cells: Int): Grid = listOf(STANDARD, FAST).first { it.cells == cells }
    }
}

data class Cage(
    val cells: List<Int>,
    val sum: Int,
)

data class Puzzle(
    val givens: List<Int>,
    val solution: List<Int>?,
    val cages: List<Cage> = emptyList(),
) {
    val grid: Grid get() = Grid.forCellCount(givens.size)

    init {
        require(solution == null || solution.size == givens.size)
    }

    val cageIndexByCell: IntArray by lazy {
        IntArray(givens.size) { NO_CAGE }.also { index ->
            cages.forEachIndexed { cage, it -> it.cells.forEach { cell -> index[cell] = cage } }
        }
    }

    val isKiller get() = cages.isNotEmpty()
    val givenCount get() = givens.count { it != 0 }
}

object PuzzleCodec {
    fun encode(puzzle: Puzzle): String =
        buildString {
            puzzle.givens.joinTo(this, "")
            append('|')
            puzzle.solution?.joinTo(this, "")
            append('|')
            puzzle.cages.joinTo(this, ";") { cage -> "${cage.sum}:${cage.cells.joinToString(",")}" }
        }

    fun decode(line: String): Puzzle {
        val (givens, solution, cages) = line.trim().split('|').let { it + List(3 - it.size) { "" } }
        return Puzzle(
            givens = givens.map { it.digitToInt() },
            solution = solution.takeIf { it.isNotEmpty() }?.map { it.digitToInt() },
            cages =
                cages.split(';').filter { it.isNotEmpty() }.map { cage ->
                    val (sum, cells) = cage.split(':')
                    Cage(cells.split(',').map { it.toInt() }, sum.toInt())
                },
        )
    }
}
