package com.joclegg.puzzles.solitaire.engine

sealed interface Move {
    data object Draw : Move

    data class Transfer(
        val from: Pile,
        val count: Int,
        val to: Pile,
    ) : Move
}

data class Selection(
    val pile: Pile,
    val count: Int,
)

data class GameState(
    val start: Board,
    val drawMode: DrawMode,
    val moves: List<Move> = emptyList(),
    val selection: Selection? = null,
    val oneTapMove: Boolean = true,
    val order: BuildOrder = BuildOrder.DOWN,
) {
    val board: Board = moves.fold(start) { board, move -> board.apply(move) }

    val isWon: Boolean get() = board.isWon

    val moveCount: Int get() = moves.size

    fun tapStock(): GameState {
        if (isWon || !board.canDraw()) {
            return copy(selection = null)
        }
        return play(Move.Draw)
    }

    fun tapCard(
        pile: Pile,
        index: Int,
    ): GameState {
        if (isWon) {
            return this
        }
        val count = board.cardsIn(pile).size - index
        val tapped = Selection(pile, count)
        val current = selection
        return when {
            current == tapped -> {
                autoMove(tapped) ?: copy(selection = null)
            }

            current != null && current.pile != pile && board.canMove(current.pile, current.count, pile, order) -> {
                play(Move.Transfer(current.pile, current.count, pile))
            }

            !board.canPickUp(pile, count, order) -> {
                copy(selection = null)
            }

            oneTapMove -> {
                autoMove(tapped) ?: copy(selection = tapped)
            }

            else -> {
                copy(selection = tapped)
            }
        }
    }

    fun drop(
        from: Pile,
        index: Int,
        to: Pile,
    ): GameState {
        val count = board.cardsIn(from).size - index
        if (isWon || from == to || !board.canMove(from, count, to, order)) {
            return copy(selection = null)
        }
        return play(Move.Transfer(from, count, to))
    }

    fun tapEmptyPile(pile: Pile): GameState {
        val current = selection ?: return this
        return if (board.canMove(current.pile, current.count, pile, order)) {
            play(Move.Transfer(current.pile, current.count, pile))
        } else {
            copy(selection = null)
        }
    }

    fun undo(): GameState =
        if (moves.isEmpty() || isWon) {
            copy(selection = null)
        } else {
            copy(moves = moves.dropLast(1), selection = null)
        }

    fun restart(): GameState = copy(moves = emptyList(), selection = null)

    fun destinationFor(selection: Selection): Pile? {
        val candidates =
            buildList {
                if (selection.count == 1) {
                    add(
                        Pile.Foundation(
                            board
                                .cardsIn(selection.pile)
                                .last()
                                .suit.ordinal,
                        ),
                    )
                }
                val columns = board.tableau.indices.map(Pile::Tableau)
                addAll(columns.filter { board.tableau[it.index].cards.isNotEmpty() })
                addAll(columns.filter { board.tableau[it.index].cards.isEmpty() })
            }
        return candidates.firstOrNull { board.canMove(selection.pile, selection.count, it, order) && !it.isPointlessFor(selection) }
    }

    private fun Pile.isPointlessFor(selection: Selection): Boolean {
        val from = selection.pile
        return this is Pile.Tableau &&
            board.tableau[index].cards.isEmpty() &&
            from is Pile.Tableau &&
            board.tableau[from.index].faceUpFrom == 0 &&
            selection.count == board.tableau[from.index].cards.size
    }

    private fun autoMove(selection: Selection): GameState? =
        destinationFor(selection)?.let { play(Move.Transfer(selection.pile, selection.count, it)) }

    private fun play(move: Move) = copy(moves = moves + move, selection = null)

    private fun Board.apply(move: Move): Board =
        when (move) {
            Move.Draw -> draw(drawMode)
            is Move.Transfer -> move(move.from, move.count, move.to)
        }
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
