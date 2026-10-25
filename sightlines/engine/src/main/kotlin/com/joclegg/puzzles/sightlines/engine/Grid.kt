package com.joclegg.puzzles.sightlines.engine

enum class Dot(
    val code: Char,
) {
    EMPTY('.'),
    BLUE('B'),
    RED('R'),
    ;

    val next: Dot
        get() =
            when (this) {
                EMPTY -> BLUE
                BLUE -> RED
                RED -> EMPTY
            }

    companion object {
        fun of(code: Char) = entries.first { it.code == code }
    }
}

val SIZES = (4..9).toList()

enum class Direction(
    val rowStep: Int,
    val colStep: Int,
) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
}

data class Grid(
    val size: Int,
    val dots: List<Dot>,
) {
    init {
        require(dots.size == size * size) { "A grid needs size x size dots" }
    }

    val isFull: Boolean get() = Dot.EMPTY !in dots

    fun with(
        cell: Int,
        dot: Dot,
    ) = copy(dots = dots.toMutableList().also { it[cell] = dot })

    fun ray(
        cell: Int,
        direction: Direction,
    ): List<Int> {
        val cells = mutableListOf<Int>()
        var row = cell / size + direction.rowStep
        var col = cell % size + direction.colStep
        while (row in 0 until size && col in 0 until size) {
            cells += row * size + col
            row += direction.rowStep
            col += direction.colStep
        }
        return cells
    }

    fun seen(
        cell: Int,
        direction: Direction,
    ) = ray(cell, direction).takeWhile { dots[it] == Dot.BLUE }.size

    fun seen(cell: Int) = Direction.entries.sumOf { seen(cell, it) }

    fun reach(
        cell: Int,
        direction: Direction,
    ) = ray(cell, direction).takeWhile { dots[it] != Dot.RED }.size

    fun encode() = dots.joinToString("") { it.code.toString() }

    companion object {
        fun empty(size: Int) = Grid(size, List(size * size) { Dot.EMPTY })

        fun parse(
            size: Int,
            text: String,
        ) = Grid(size, text.filterNot { it.isWhitespace() }.map(Dot::of))
    }
}

data class Puzzle(
    val solution: Grid,
    val numbers: Map<Int, Int>,
    val walls: Set<Int>,
) {
    val size: Int get() = solution.size

    val givens: Grid
        get() =
            Grid(
                size,
                solution.dots.indices.map { cell ->
                    when (cell) {
                        in numbers -> Dot.BLUE
                        in walls -> Dot.RED
                        else -> Dot.EMPTY
                    }
                },
            )

    fun isGiven(cell: Int) = cell in numbers || cell in walls

    fun brokenCells(grid: Grid): Set<Int> =
        grid.dots.indices
            .filter { cell ->
                val number = numbers[cell]
                val reach = Direction.entries.sumOf { grid.reach(cell, it) }
                when {
                    number != null -> grid.seen(cell) > number || reach < number
                    grid.dots[cell] == Dot.BLUE -> reach == 0
                    else -> false
                }
            }.toSet()

    fun encode(): String {
        val clues =
            solution.dots.indices.joinToString("") { cell ->
                when (cell) {
                    in numbers -> numbers.getValue(cell).toString(RADIX)
                    in walls -> WALL.toString()
                    else -> "."
                }
            }
        return "$size;$clues;${solution.encode()}"
    }

    companion object {
        private const val RADIX = 36
        private const val WALL = 'X'

        fun decode(text: String): Puzzle {
            val (size, clues, solution) = text.split(';')
            val numbers = mutableMapOf<Int, Int>()
            val walls = mutableSetOf<Int>()
            clues.forEachIndexed { cell, code ->
                when (code) {
                    '.' -> Unit
                    WALL -> walls += cell
                    else -> numbers[cell] = code.digitToInt(RADIX)
                }
            }
            return Puzzle(Grid.parse(size.toInt(), solution), numbers, walls)
        }
    }
}
