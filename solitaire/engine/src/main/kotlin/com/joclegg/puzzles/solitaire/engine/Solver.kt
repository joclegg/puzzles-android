package com.joclegg.puzzles.solitaire.engine

enum class Verdict { WINNABLE, UNWINNABLE, UNKNOWN }

object Solver {
    const val DEFAULT_BUDGET = 200_000
    private const val DEAL_BUDGET = 20_000

    fun solve(
        start: Board,
        drawMode: DrawMode,
        order: BuildOrder,
        budget: Int = DEFAULT_BUDGET,
        quickOnly: Boolean = false,
        shouldStop: () -> Boolean = { false },
    ): Verdict {
        val quick = search(start, drawMode, order, budget, thorough = false, shouldStop)
        if (quick == Verdict.WINNABLE || quickOnly) {
            return quick.takeIf { it == Verdict.WINNABLE } ?: Verdict.UNKNOWN
        }
        return search(start, drawMode, order, budget, thorough = true, shouldStop)
    }

    fun winnableDeal(
        seed: Long,
        drawMode: DrawMode,
        order: BuildOrder,
    ): Board {
        var next = seed
        while (true) {
            val board = Board.deal(next)
            if (solve(board, drawMode, order, DEAL_BUDGET, quickOnly = true) == Verdict.WINNABLE) {
                return board
            }
            next++
        }
    }

    private fun search(
        start: Board,
        drawMode: DrawMode,
        order: BuildOrder,
        budget: Int,
        thorough: Boolean,
        shouldStop: () -> Boolean,
    ): Verdict {
        val seen = HashSet<String>()
        val stack = ArrayDeque(listOf(settle(start, drawMode, order)))
        var explored = 0
        while (stack.isNotEmpty()) {
            val board = stack.removeLast()
            if (board.isWon) {
                return Verdict.WINNABLE
            }
            if (!seen.add(key(board))) {
                continue
            }
            explored++
            if (explored > budget || shouldStop()) {
                return Verdict.UNKNOWN
            }
            moves(board, drawMode, order, thorough).asReversed().forEach { next -> stack.addLast(settle(next, drawMode, order)) }
        }
        return Verdict.UNWINNABLE
    }

    private fun moves(
        board: Board,
        drawMode: DrawMode,
        order: BuildOrder,
        thorough: Boolean,
    ): List<Board> {
        val foundation = mutableListOf<Board>()
        val revealing = mutableListOf<Board>()
        val other = mutableListOf<Board>()
        val foundations = (0 until Board.FOUNDATIONS).map(Pile::Foundation)
        val columns = board.tableau.indices.map(Pile::Tableau)
        val tops = listOf<Pile>(Pile.Waste) + columns
        for (from in tops) {
            for (to in foundations) {
                if (board.canMove(from, 1, to, order)) {
                    foundation += board.move(from, 1, to)
                }
            }
        }
        for (from in columns) {
            val column = board.tableau[from.index]
            for (count in 1..column.faceUp.size) {
                val reveals = count == column.faceUp.size && column.faceUpFrom > 0
                val wholeColumn = count == column.cards.size
                for (to in columns) {
                    if (to == from || !board.canMove(from, count, to, order)) {
                        continue
                    }
                    if (wholeColumn && board.tableau[to.index].cards.isEmpty()) {
                        continue
                    }
                    val moved = board.move(from, count, to)
                    when {
                        reveals || wholeColumn -> revealing += moved
                        thorough || freesFoundationCard(moved, from, order) -> other += moved
                    }
                }
            }
        }
        for (to in columns) {
            if (board.canMove(Pile.Waste, 1, to, order)) {
                other += board.move(Pile.Waste, 1, to)
            }
            for (from in foundations.takeIf { thorough }.orEmpty()) {
                if (board.canMove(from, 1, to, order)) {
                    other += board.move(from, 1, to)
                }
            }
        }
        if (board.canDraw()) {
            other += board.draw(drawMode)
        }
        return foundation + revealing + other
    }

    private fun freesFoundationCard(
        board: Board,
        column: Pile,
        order: BuildOrder,
    ): Boolean = (0 until Board.FOUNDATIONS).any { board.canMove(column, 1, Pile.Foundation(it), order) }

    private fun settle(
        start: Board,
        drawMode: DrawMode,
        order: BuildOrder,
    ): Board {
        var board = start
        while (true) {
            val safe = safeFoundationMove(board, drawMode, order) ?: return board
            board = safe
        }
    }

    private fun safeFoundationMove(
        board: Board,
        drawMode: DrawMode,
        order: BuildOrder,
    ): Board? {
        val columns = board.tableau.indices.map(Pile::Tableau)
        val tops =
            if (drawMode == DrawMode.DRAW_ONE) {
                listOf<Pile>(Pile.Waste) + columns
            } else {
                columns
            }
        for (from in tops) {
            val card = board.cardsIn(from).lastOrNull() ?: continue
            val to = Pile.Foundation(card.suit.ordinal)
            if (!board.canMove(from, 1, to, order)) {
                continue
            }
            val placedBefore = board.foundations[card.suit.ordinal].size
            val opposite = Suit.entries.filter { it.isRed != card.suit.isRed }.map { board.foundations[it.ordinal].size }
            if (placedBefore <= 1 || opposite.all { it >= placedBefore }) {
                return board.move(from, 1, to)
            }
        }
        return null
    }

    private fun key(board: Board): String =
        buildString {
            board.tableau.forEach { column ->
                append(column.faceUpFrom)
                column.cards.forEach { append(it.code) }
                append('|')
            }
            board.stock.forEach { append(it.code) }
            append('|')
            board.waste.forEach { append(it.code) }
            append('|')
            board.foundations.forEach { append(it.size) }
        }
}
