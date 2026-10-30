package com.joclegg.puzzles.solitaire.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.joclegg.puzzles.solitaire.engine.Board
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.GameState
import com.joclegg.puzzles.solitaire.engine.Pile
import com.joclegg.puzzles.solitaire.engine.Selection
import com.joclegg.puzzles.solitaire.engine.Suit

@Immutable
data class TableUi(
    val board: Board,
    val selection: Selection? = null,
    val visibleWaste: Int = 1,
    val order: BuildOrder = BuildOrder.DOWN,
) {
    fun isSelected(
        pile: Pile,
        index: Int,
    ): Boolean {
        val selected = selection ?: return false
        return selected.pile == pile && index >= board.cardsIn(pile).size - selected.count
    }
}

fun GameState.toTableUi() = TableUi(board, selection, drawMode.cards, order)

private val Gap = 4.dp
private const val CARD_ASPECT = 1.45f
private const val FACE_DOWN_STEP = 0.14f
private const val FACE_UP_STEP = 0.3f
private const val WASTE_FAN = 0.32f

@Composable
fun SolitaireTable(
    table: TableUi,
    onStockClick: () -> Unit,
    onCardClick: (Pile, Int) -> Unit,
    onEmptyPileClick: (Pile) -> Unit,
    modifier: Modifier = Modifier,
    onCardDrop: (Pile, Int, Pile) -> Unit = { _, _, _ -> },
    celebrate: Boolean = false,
) {
    val colors = LocalTableColors.current
    val currentOnDrop by rememberUpdatedState(onCardDrop)
    val drag = remember { CardDrag { from, index, to -> currentOnDrop(from, index, to) } }
    BoxWithConstraints(modifier.fillMaxSize().background(colors.felt).padding(Gap)) {
        val cardWidth = (maxWidth - Gap * (Board.COLUMNS - 1)) / Board.COLUMNS
        val card = DpSize(cardWidth, cardWidth * CARD_ASPECT)
        val tableauHeight = maxHeight - card.height - Gap * 3
        Column {
            val topLifted = drag.pile == Pile.Waste || drag.pile is Pile.Foundation
            Row(
                Modifier.zIndex(
                    if (topLifted) {
                        1f
                    } else {
                        0f
                    },
                ),
            ) {
                StockPile(table.board.stock.size, card, onStockClick)
                Spacer(Modifier.width(Gap))
                WastePile(table, card, onCardClick, drag, Modifier.width(cardWidth * 2 + Gap))
                Spacer(Modifier.width(Gap))
                table.board.foundations.forEachIndexed { index, cards ->
                    val pile = Pile.Foundation(index)
                    Box(Modifier.size(card).then(drag.target(pile))) {
                        EmptyPile(
                            pile,
                            card,
                            onEmptyPileClick,
                            starter = Card(table.order.foundationStarter, Suit.entries[index]).rankLabel,
                        )
                        cards.lastOrNull()?.let { top ->
                            key(top) {
                                PlayingCard(
                                    top,
                                    card,
                                    table.isSelected(pile, cards.lastIndex),
                                    modifier =
                                        Modifier
                                            .landing(celebrate)
                                            .then(drag.lifted(pile, cards.lastIndex))
                                            .then(drag.source(pile, cards.lastIndex)),
                                    onClick = { onCardClick(pile, cards.lastIndex) },
                                )
                            }
                        }
                    }
                    if (index < table.board.foundations.lastIndex) {
                        Spacer(Modifier.width(Gap))
                    }
                }
            }
            Spacer(Modifier.height(Gap * 3))
            Row {
                table.board.tableau.forEachIndexed { index, _ ->
                    val lifted = drag.pile == Pile.Tableau(index)
                    TableauColumn(
                        table,
                        index,
                        card,
                        tableauHeight,
                        onCardClick,
                        onEmptyPileClick,
                        drag,
                        Modifier.zIndex(
                            if (lifted) {
                                1f
                            } else {
                                0f
                            },
                        ),
                    )
                    if (index < table.board.tableau.lastIndex) {
                        Spacer(Modifier.width(Gap))
                    }
                }
            }
        }
    }
}

@Composable
private fun StockPile(
    count: Int,
    card: DpSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTableColors.current
    Box(
        modifier
            .size(card)
            .clip(CardShape)
            .clickable(onClick = onClick)
            .testTag("stock"),
    ) {
        if (count > 0) {
            CardBack(card)
            Text(
                count.toString(),
                color = colors.onFelt,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).testTag("stock_count"),
            )
        } else {
            Box(Modifier.fillMaxSize().background(colors.emptyPile, CardShape))
            Text("↻", color = colors.onFelt, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun WastePile(
    table: TableUi,
    card: DpSize,
    onCardClick: (Pile, Int) -> Unit,
    drag: CardDrag,
    modifier: Modifier = Modifier,
) {
    val waste = table.board.waste
    val shown = waste.takeLast(table.visibleWaste)
    Box(modifier.height(card.height).testTag("waste")) {
        shown.forEachIndexed { position, top ->
            val index = waste.size - shown.size + position
            val isTop = index == waste.lastIndex
            PlayingCard(
                top,
                card,
                table.isSelected(Pile.Waste, index),
                modifier =
                    Modifier
                        .offset(x = card.width * WASTE_FAN * position)
                        .then(
                            if (isTop) {
                                drag.lifted(Pile.Waste, index).then(drag.source(Pile.Waste, index))
                            } else {
                                Modifier
                            },
                        ),
                onClick =
                    if (isTop) {
                        { onCardClick(Pile.Waste, index) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun TableauColumn(
    table: TableUi,
    column: Int,
    card: DpSize,
    maxHeight: Dp,
    onCardClick: (Pile, Int) -> Unit,
    onEmptyPileClick: (Pile) -> Unit,
    drag: CardDrag,
    modifier: Modifier = Modifier,
) {
    val pile = Pile.Tableau(column)
    val cards = table.board.tableau[column]
    val faceDown = cards.faceUpFrom.coerceAtMost(cards.cards.size)
    val faceUp = cards.cards.size - faceDown
    val wanted = card.height * FACE_DOWN_STEP * faceDown + card.height * FACE_UP_STEP * (faceUp - 1).coerceAtLeast(0) + card.height
    val squeeze =
        if (wanted > maxHeight && cards.cards.size > 1) {
            ((maxHeight - card.height) / (wanted - card.height)).coerceIn(0f, 1f)
        } else {
            1f
        }
    Box(modifier.width(card.width).height(maxHeight).then(drag.target(pile))) {
        EmptyPile(pile, card, onEmptyPileClick)
        var top = 0.dp
        cards.cards.forEachIndexed { index, playing ->
            val isFaceUp = cards.isFaceUp(index)
            val cardModifier = Modifier.offset(y = top)
            if (isFaceUp) {
                PlayingCard(
                    playing,
                    card,
                    table.isSelected(pile, index),
                    cardModifier.then(drag.lifted(pile, index)).then(drag.source(pile, index)),
                    onClick = { onCardClick(pile, index) },
                )
            } else {
                CardBack(card, cardModifier.testTag(faceDownTag(column, index)).semantics { cardFaceUp = false })
            }
            top +=
                card.height * squeeze *
                if (isFaceUp) {
                    FACE_UP_STEP
                } else {
                    FACE_DOWN_STEP
                }
        }
    }
}

@Composable
private fun EmptyPile(
    pile: Pile,
    card: DpSize,
    onClick: (Pile) -> Unit,
    modifier: Modifier = Modifier,
    starter: String? = null,
) {
    val colors = LocalTableColors.current
    Box(
        modifier
            .size(card)
            .background(colors.emptyPile, CardShape)
            .clickable { onClick(pile) }
            .testTag(pileTag(pile)),
    ) {
        if (starter != null) {
            Text(
                starter,
                color = colors.onFelt,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private val CardShape = RoundedCornerShape(5.dp)

@Composable
fun PlayingCard(
    card: Card,
    size: DpSize,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalTableColors.current
    val ink =
        if (card.suit.isRed) {
            colors.redSuit
        } else {
            colors.blackSuit
        }
    val face =
        if (selected) {
            colors.selected
        } else {
            colors.cardFace
        }
    Box(
        modifier
            .size(size)
            .clip(CardShape)
            .background(face)
            .border(1.dp, colors.cardEdge, CardShape)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
            .testTag(cardTag(card))
            .semantics {
                cardFaceUp = true
                cardSelected = selected
            },
    ) {
        val label = (size.width.value * 0.36f).sp
        Text(
            card.rankLabel,
            color = ink,
            fontSize = label,
            fontWeight = FontWeight.Bold,
            lineHeight = label,
            modifier = Modifier.padding(start = 4.dp, top = 1.dp),
        )
        Text(
            card.suit.symbol.toString(),
            color = ink,
            fontSize = (size.width.value * 0.55f).sp,
            modifier = Modifier.align(Alignment.Center).padding(top = size.height * 0.15f),
        )
    }
}

@Composable
private fun CardBack(
    size: DpSize,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTableColors.current
    Box(
        modifier
            .size(size)
            .clip(CardShape)
            .background(colors.cardBack)
            .border(1.dp, colors.cardEdge, CardShape)
            .padding(4.dp)
            .border(1.dp, colors.cardBackPattern, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun Modifier.landing(celebrate: Boolean): Modifier {
    val pop = remember { Animatable(if (celebrate) 0f else 1f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    return graphicsLayer {
        val lift = 1f - pop.value
        scaleX = 1f + lift * LANDING_SCALE
        scaleY = 1f + lift * LANDING_SCALE
        rotationZ = lift * LANDING_TILT
        translationY = -lift * size.height * LANDING_RISE
    }.zIndex(
        if (pop.value < 1f) {
            2f
        } else {
            0f
        },
    )
}

private const val LANDING_SCALE = 0.5f
private const val LANDING_TILT = -18f
private const val LANDING_RISE = 0.6f
