package com.joclegg.puzzles.solitaire.engine

sealed interface Hint {
    data class Transfer(
        val selection: Selection,
        val to: Pile,
    ) : Hint

    data object Draw : Hint
}

fun GameState.findHint(): Hint? {
    if (isWon) {
        return null
    }
    val waste =
        if (board.waste.isNotEmpty()) {
            listOf(Selection(Pile.Waste, 1) to 1)
        } else {
            emptyList()
        }
    val tableau =
        board.tableau.flatMapIndexed { index, column ->
            (column.faceUpFrom until column.cards.size).map { start ->
                val rank =
                    if (start == column.faceUpFrom && start > 0) {
                        2
                    } else if (start == column.cards.size - 1) {
                        0
                    } else {
                        -1
                    }
                Selection(Pile.Tableau(index), column.cards.size - start) to rank
            }
        }
    val best =
        (waste + tableau)
            .mapNotNull { (selection, rank) ->
                destinationFor(selection)?.let { to ->
                    val score =
                        if (to is Pile.Foundation) {
                            3
                        } else {
                            rank
                        }
                    Triple(selection, to, score)
                }
            }.filter { it.third >= 1 }
            .maxByOrNull { it.third }
    if (best != null) {
        return Hint.Transfer(best.first, best.second)
    }
    return if (board.canDraw()) {
        Hint.Draw
    } else {
        null
    }
}
