package com.joclegg.puzzles.solitaire.core.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import com.joclegg.puzzles.solitaire.engine.Pile
import kotlin.math.roundToInt

@Stable
class CardDrag(
    private val onDrop: (Pile, Int, Pile) -> Unit,
) {
    var pile by mutableStateOf<Pile?>(null)
        private set
    var index by mutableIntStateOf(0)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var finger = Offset.Zero
    private val targets = mutableStateMapOf<Pile, Rect>()

    fun lifts(
        pile: Pile,
        index: Int,
    ) = this.pile == pile && index >= this.index

    fun lifted(
        pile: Pile,
        index: Int,
    ): Modifier =
        Modifier.offset {
            if (lifts(pile, index)) {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            } else {
                IntOffset.Zero
            }
        }

    fun target(pile: Pile): Modifier = Modifier.onGloballyPositioned { targets[pile] = it.boundsInRoot() }

    fun source(
        pile: Pile,
        index: Int,
    ): Modifier {
        var origin = Offset.Zero
        return Modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(pile, index) {
                detectDragGestures(
                    onDragStart = { start ->
                        this@CardDrag.pile = pile
                        this@CardDrag.index = index
                        offset = Offset.Zero
                        finger = origin + start
                    },
                    onDragEnd = {
                        val target = targets.entries.firstOrNull { it.key != pile && finger in it.value }?.key
                        reset()
                        if (target != null) {
                            onDrop(pile, index, target)
                        }
                    },
                    onDragCancel = { reset() },
                ) { change, amount ->
                    change.consume()
                    offset += amount
                    finger += amount
                }
            }
    }

    private fun reset() {
        pile = null
        offset = Offset.Zero
    }
}
