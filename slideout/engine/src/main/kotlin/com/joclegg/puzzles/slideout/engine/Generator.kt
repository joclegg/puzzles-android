package com.joclegg.puzzles.slideout.engine

import kotlin.random.Random

enum class Level(
    val moves: IntRange,
) {
    BEGINNER(4..9),
    EASY(10..16),
    MEDIUM(17..24),
    HARD(25..32),
    EXPERT(33..60),
}

data class Puzzle(
    val board: Board,
    val par: Int,
) {
    fun encode() = "$par;${board.encode()}"

    companion object {
        fun decode(text: String): Puzzle {
            val (par, board) = text.split(';')
            return Puzzle(Board.decode(board), par.toInt())
        }
    }
}

object Generator {
    private const val STEPS = 6_000
    private const val PATIENCE = 400
    private const val MAX_BLOCKS = 13
    private const val START_BLOCKS = 10
    private const val START_TRIES = 200

    fun generate(
        level: Level,
        seed: Long,
        shouldStop: () -> Boolean = { false },
    ): Puzzle? {
        val random = Random(seed)
        var board = start(random)
        var depth = Solver.hardest(board, level.moves)?.second ?: 0
        var sinceBetter = 0
        repeat(STEPS) {
            if (shouldStop()) {
                return null
            }
            if (depth >= level.moves.first) {
                val (hardest, moves) = Solver.hardest(board, level.moves) ?: return null
                return Puzzle(hardest, moves)
            }
            if (sinceBetter > PATIENCE) {
                board = start(random)
                depth = Solver.hardest(board, level.moves)?.second ?: 0
                sinceBetter = 0
            }
            sinceBetter++
            val candidate = mutate(board, random) ?: return@repeat
            val candidateDepth = Solver.hardest(candidate, level.moves)?.second ?: return@repeat
            if (candidateDepth > depth) {
                sinceBetter = 0
            }
            if (candidateDepth >= depth) {
                board = candidate
                depth = candidateDepth
            }
        }
        return null
    }

    private fun start(random: Random): Board {
        var board = Board(listOf(Block(EXIT_ROW, random.nextInt(2), 2, horizontal = true)))
        repeat(START_TRIES) {
            if (board.blocks.size < START_BLOCKS) {
                board = add(board, random) ?: board
            }
        }
        return board
    }

    private fun mutate(
        board: Board,
        random: Random,
    ): Board? =
        when (random.nextInt(3)) {
            0 -> add(board, random)
            1 -> remove(board, random)
            else -> shift(board, random)
        }

    private fun add(
        board: Board,
        random: Random,
    ): Board? {
        if (board.blocks.size >= MAX_BLOCKS) {
            return null
        }
        val horizontal = random.nextBoolean()
        val length =
            if (random.nextInt(4) == 0) {
                3
            } else {
                2
            }
        val block =
            if (horizontal) {
                Block(random.nextInt(SIZE), random.nextInt(SIZE - length + 1), length, true)
            } else {
                Block(random.nextInt(SIZE - length + 1), random.nextInt(SIZE), length, false)
            }
        if (horizontal && block.row == EXIT_ROW) {
            return null
        }
        return board.takeIf { block.cells.none { it in board.occupied() } }?.copy(blocks = board.blocks + block)
    }

    private fun remove(
        board: Board,
        random: Random,
    ): Board? {
        if (board.blocks.size <= 1) {
            return null
        }
        val index = 1 + random.nextInt(board.blocks.size - 1)
        return board.copy(blocks = board.blocks.filterIndexed { i, _ -> i != index })
    }

    private fun shift(
        board: Board,
        random: Random,
    ): Board? {
        if (board.blocks.size <= 1) {
            return null
        }
        val index = 1 + random.nextInt(board.blocks.size - 1)
        val block = board.blocks[index]
        val position = random.nextInt(SIZE - block.length + 1)
        val moved = block.movedTo(position)
        val taken = board.occupied(except = index)
        return board.takeIf { moved.cells.none { it in taken } }?.move(index, position)
    }
}
