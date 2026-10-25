package com.joclegg.puzzles.solitaire.engine

import kotlin.random.Random

enum class DrawMode(
    val cards: Int,
) {
    DRAW_ONE(1),
    DRAW_THREE(3),
}

enum class BuildOrder(
    val step: Int,
    val columnStarter: Int,
    val foundationStarter: Int,
) {
    DOWN(1, Card.KING, Card.ACE),
    UP(-1, Card.ACE, Card.KING),
    ;

    fun foundationRank(placed: Int) = foundationStarter + step * placed
}

sealed interface Pile {
    data object Waste : Pile

    data class Foundation(
        val index: Int,
    ) : Pile

    data class Tableau(
        val index: Int,
    ) : Pile
}

data class Column(
    val cards: List<Card>,
    val faceUpFrom: Int = (cards.size - 1).coerceAtLeast(0),
) {
    val faceUp: List<Card> get() = cards.drop(faceUpFrom)

    fun isFaceUp(index: Int) = index >= faceUpFrom

    fun withoutTop(count: Int): Column {
        val remaining = cards.dropLast(count)
        return Column(remaining, faceUpFrom.coerceAtMost((remaining.size - 1).coerceAtLeast(0)))
    }

    fun plus(moved: List<Card>) = copy(cards = cards + moved, faceUpFrom = faceUpFrom.coerceAtMost(cards.size))
}

data class Board(
    val tableau: List<Column>,
    val stock: List<Card>,
    val waste: List<Card> = emptyList(),
    val foundations: List<List<Card>> = List(FOUNDATIONS) { emptyList() },
) {
    val isWon: Boolean get() = foundations.all { it.size == Card.KING }

    fun cardsIn(pile: Pile): List<Card> =
        when (pile) {
            Pile.Waste -> waste
            is Pile.Foundation -> foundations[pile.index]
            is Pile.Tableau -> tableau[pile.index].cards
        }

    fun draw(drawMode: DrawMode): Board =
        if (stock.isEmpty()) {
            copy(stock = waste.reversed(), waste = emptyList())
        } else {
            val drawn = stock.takeLast(drawMode.cards).reversed()
            copy(stock = stock.dropLast(drawn.size), waste = waste + drawn)
        }

    fun canDraw() = stock.isNotEmpty() || waste.isNotEmpty()

    fun canPickUp(
        pile: Pile,
        count: Int,
        order: BuildOrder = BuildOrder.DOWN,
    ): Boolean {
        val cards = cardsIn(pile)
        return when (pile) {
            Pile.Waste, is Pile.Foundation -> {
                count == 1 && cards.isNotEmpty()
            }

            is Pile.Tableau -> {
                val column = tableau[pile.index]
                count in 1..column.faceUp.size &&
                    cards.takeLast(count).zipWithNext().all { (below, above) -> above.stacksOnTableau(below, order) }
            }
        }
    }

    fun canMove(
        from: Pile,
        count: Int,
        to: Pile,
        order: BuildOrder = BuildOrder.DOWN,
    ): Boolean {
        if (from == to || !canPickUp(from, count, order)) {
            return false
        }
        val moving = cardsIn(from).takeLast(count)
        val lead = moving.first()
        return when (to) {
            Pile.Waste -> {
                false
            }

            is Pile.Foundation -> {
                count == 1 && lead.suit.ordinal == to.index && lead.rank == order.foundationRank(foundations[to.index].size)
            }

            is Pile.Tableau -> {
                val target = tableau[to.index].cards.lastOrNull()
                if (target == null) {
                    lead.rank == order.columnStarter
                } else {
                    lead.stacksOnTableau(target, order)
                }
            }
        }
    }

    fun move(
        from: Pile,
        count: Int,
        to: Pile,
    ): Board {
        val moving = cardsIn(from).takeLast(count)
        return remove(from, count).add(to, moving)
    }

    private fun remove(
        pile: Pile,
        count: Int,
    ): Board =
        when (pile) {
            Pile.Waste -> copy(waste = waste.dropLast(count))
            is Pile.Foundation -> copy(foundations = foundations.replace(pile.index) { it.dropLast(count) })
            is Pile.Tableau -> copy(tableau = tableau.replace(pile.index) { it.withoutTop(count) })
        }

    private fun add(
        pile: Pile,
        cards: List<Card>,
    ): Board =
        when (pile) {
            Pile.Waste -> copy(waste = waste + cards)
            is Pile.Foundation -> copy(foundations = foundations.replace(pile.index) { it + cards })
            is Pile.Tableau -> copy(tableau = tableau.replace(pile.index) { it.plus(cards) })
        }

    companion object {
        const val COLUMNS = 7
        const val FOUNDATIONS = 4

        fun deal(seed: Long): Board = deal(Card.deck.shuffled(Random(seed)))

        fun deal(deck: List<Card>): Board {
            require(deck.size == Card.deck.size && deck.toSet() == Card.deck.toSet()) { "A deal needs every card exactly once" }
            var next = 0
            val tableau =
                (1..COLUMNS).map { size ->
                    Column(deck.subList(next, next + size)).also { next += size }
                }
            return Board(tableau, stock = deck.drop(next))
        }
    }
}

private fun <T> List<T>.replace(
    index: Int,
    change: (T) -> T,
): List<T> =
    mapIndexed { i, item ->
        if (i == index) {
            change(item)
        } else {
            item
        }
    }
