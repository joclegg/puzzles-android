package com.joclegg.puzzles.twotone.engine

enum class Tile(
    val code: Char,
) {
    EMPTY('.'),
    RED('R'),
    BLUE('B'),
    ;

    val other: Tile
        get() =
            when (this) {
                RED -> BLUE
                BLUE -> RED
                EMPTY -> EMPTY
            }

    val next: Tile
        get() =
            when (this) {
                EMPTY -> RED
                RED -> BLUE
                BLUE -> EMPTY
            }

    companion object {
        fun of(code: Char) = entries.first { it.code == code }
    }
}

val SIZES = listOf(4, 6, 8, 10, 12)

data class Line(
    val isRow: Boolean,
    val index: Int,
)

data class Grid(
    val size: Int,
    val tiles: List<Tile>,
) {
    init {
        require(size % 2 == 0 && tiles.size == size * size) { "A grid needs an even size and size x size tiles" }
    }

    val lines: List<Line> get() = (0 until size).flatMap { listOf(Line(true, it), Line(false, it)) }

    val isFull: Boolean get() = Tile.EMPTY !in tiles

    fun cellsOf(line: Line): List<Int> =
        (0 until size).map { position ->
            if (line.isRow) {
                line.index * size + position
            } else {
                position * size + line.index
            }
        }

    fun tilesOf(line: Line) = cellsOf(line).map { tiles[it] }

    fun with(
        cell: Int,
        tile: Tile,
    ) = copy(tiles = tiles.toMutableList().also { it[cell] = tile })

    fun brokenCells(): Set<Int> =
        lines.flatMapTo(mutableSetOf()) { line ->
            val cells = cellsOf(line)
            val lineTiles = tilesOf(line)
            val threes =
                (0..size - RUN).filter { start ->
                    val run = lineTiles.subList(start, start + RUN)
                    run[0] != Tile.EMPTY && run.all { it == run[0] }
                }
            val runs = threes.flatMap { start -> cells.subList(start, start + RUN) }
            val overfull =
                listOf(Tile.RED, Tile.BLUE).flatMap { tile ->
                    if (lineTiles.count { it == tile } > size / 2) {
                        cells.filter { tiles[it] == tile }
                    } else {
                        emptyList()
                    }
                }
            val duplicate =
                if (Tile.EMPTY !in lineTiles && lines.any { it != line && it.isRow == line.isRow && tilesOf(it) == lineTiles }) {
                    cells
                } else {
                    emptyList()
                }
            runs + overfull + duplicate
        }

    companion object {
        const val RUN = 3

        fun empty(size: Int) = Grid(size, List(size * size) { Tile.EMPTY })

        fun parse(
            size: Int,
            text: String,
        ) = Grid(size, text.filterNot { it.isWhitespace() }.map(Tile::of))
    }

    fun encode() = tiles.joinToString("") { it.code.toString() }
}
