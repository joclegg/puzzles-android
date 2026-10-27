package com.joclegg.puzzles.slideout.engine

object MoveCodec {
    fun encode(moves: List<Move>) = moves.joinToString(",") { "${it.block}:${it.position}" }

    fun decode(text: String): List<Move> =
        text.split(',').filter { it.isNotEmpty() }.map {
            val (block, position) = it.split(':').map(String::toInt)
            Move(block, position)
        }
}

data class GameState(
    val puzzle: Puzzle,
    val moves: List<Move> = emptyList(),
    val hint: Move? = null,
) {
    val board: Board = moves.fold(puzzle.board) { board, move -> board.move(move.block, move.position) }

    val isSolved: Boolean get() = board.isSolved

    fun slide(
        block: Int,
        position: Int,
    ): GameState {
        if (isSolved || position == board.blocks[block].position || position !in board.range(block)) {
            return copy(hint = null)
        }
        val last = moves.lastOrNull()
        val merged =
            if (last?.block == block) {
                moves.dropLast(1) + Move(block, position)
            } else {
                moves + Move(block, position)
            }
        val back = merged.lastOrNull()?.let { it.block == block && it.position == previousPosition(block, merged) } == true
        return copy(
            moves =
                if (back) {
                    merged.dropLast(1)
                } else {
                    merged
                },
            hint = null,
        )
    }

    fun undo(): GameState =
        if (isSolved || moves.isEmpty()) {
            copy(hint = null)
        } else {
            copy(moves = moves.dropLast(1), hint = null)
        }

    fun restart(): GameState = GameState(puzzle)

    fun revealSolution(): GameState = copy(moves = moves + Solver.shortest(board).orEmpty(), hint = null)

    fun withHint(): GameState {
        if (isSolved) {
            return this
        }
        return copy(hint = Solver.shortest(board)?.firstOrNull())
    }

    private fun previousPosition(
        block: Int,
        moves: List<Move>,
    ): Int = moves.dropLast(1).lastOrNull { it.block == block }?.position ?: puzzle.board.blocks[block].position
}

data class GameTimer(
    val accumulatedMs: Long = 0,
    val runningSinceMs: Long? = null,
) {
    fun start(nowMs: Long) =
        if (runningSinceMs != null) {
            this
        } else {
            copy(runningSinceMs = nowMs)
        }

    fun pause(nowMs: Long) = runningSinceMs?.let { GameTimer(accumulatedMs + (nowMs - it), null) } ?: this

    fun elapsedMs(nowMs: Long) = accumulatedMs + (runningSinceMs?.let { nowMs - it } ?: 0)
}
