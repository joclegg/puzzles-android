package com.joclegg.puzzles.slideout.engine

const val SIZE = 6
const val EXIT_ROW = 2

data class Block(
    val row: Int,
    val col: Int,
    val length: Int,
    val horizontal: Boolean,
) {
    val cells: List<Int>
        get() =
            (0 until length).map { step ->
                if (horizontal) {
                    row * SIZE + col + step
                } else {
                    (row + step) * SIZE + col
                }
            }

    val position: Int
        get() =
            if (horizontal) {
                col
            } else {
                row
            }

    fun movedTo(position: Int) =
        if (horizontal) {
            copy(col = position)
        } else {
            copy(row = position)
        }

    fun encode() =
        "$row$col$length" +
            if (horizontal) {
                "H"
            } else {
                "V"
            }

    companion object {
        fun decode(code: String) = Block(code[0].digitToInt(), code[1].digitToInt(), code[2].digitToInt(), code[3] == 'H')
    }
}

data class Board(
    val blocks: List<Block>,
) {
    val target: Block get() = blocks.first()

    val isSolved: Boolean get() = target.col + target.length == SIZE

    fun occupied(except: Int? = null): Set<Int> = blocks.filterIndexed { index, _ -> index != except }.flatMap { it.cells }.toSet()

    fun range(index: Int): IntRange {
        val block = blocks[index]
        val taken = occupied(except = index)

        fun free(position: Int) = position in 0..SIZE - block.length && block.movedTo(position).cells.none { it in taken }
        var low = block.position
        while (free(low - 1)) {
            low--
        }
        var high = block.position
        while (free(high + 1)) {
            high++
        }
        return low..high
    }

    fun move(
        index: Int,
        position: Int,
    ): Board =
        copy(
            blocks =
                blocks.mapIndexed { i, block ->
                    if (i == index) {
                        block.movedTo(position)
                    } else {
                        block
                    }
                },
        )

    fun neighbours(): List<Pair<Move, Board>> =
        blocks.indices.flatMap { index ->
            range(index).filter { it != blocks[index].position }.map { position -> Move(index, position) to move(index, position) }
        }

    fun key(): String = blocks.joinToString("") { it.position.toString() }

    fun encode() = blocks.joinToString(",") { it.encode() }

    fun identity(): String = (listOf(blocks.first().encode()) + blocks.drop(1).map { it.encode() }.sorted()).joinToString(",")

    companion object {
        fun decode(text: String) = Board(text.split(',').map(Block::decode))
    }
}

data class Move(
    val block: Int,
    val position: Int,
)
