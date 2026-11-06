package com.joclegg.puzzles.slideout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.slideout.engine.Block
import com.joclegg.puzzles.slideout.engine.Board
import com.joclegg.puzzles.slideout.engine.EXIT_ROW
import com.joclegg.puzzles.slideout.engine.Move
import com.joclegg.puzzles.slideout.engine.SIZE
import kotlin.math.roundToInt

@Immutable
data class BoardUi(
    val board: Board,
    val hint: Move?,
)

private val BlockShape = RoundedCornerShape(8.dp)
private val BoardShape = RoundedCornerShape(12.dp)
private val InnerBoardShape = RoundedCornerShape(9.dp)
private val Inset = 3.dp
private val FrameWidth = 3.dp
private val BoardPadding = 6.dp
private val TipLength = 4.dp
private val ArrowSize = 16.dp

@Composable
fun SlideBoard(
    board: BoardUi,
    onSlide: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSlideOutColors.current
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .background(colors.block, BoardShape)
            .padding(FrameWidth)
            .background(colors.board, InnerBoardShape)
            .padding(BoardPadding - FrameWidth),
    ) {
        val cell = maxWidth / SIZE
        ExitGap(Modifier.offset(x = maxWidth, y = cell * EXIT_ROW - TipLength).size(BoardPadding, cell + TipLength * 2))
        for (row in 0 until SIZE) {
            for (col in 0 until SIZE) {
                Box(
                    Modifier
                        .offset(x = cell * col, y = cell * row)
                        .size(cell)
                        .padding(Inset)
                        .background(colors.slot, BlockShape),
                )
            }
        }
        board.board.blocks.forEachIndexed { index, block ->
            key(index) {
                BlockView(index, block, board.board.range(index), board.hint?.takeIf { it.block == index }, cell, onSlide)
            }
        }
    }
}

@Composable
private fun ExitGap(modifier: Modifier = Modifier) {
    val colors = LocalSlideOutColors.current
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.align(Alignment.TopEnd).size(FrameWidth, TipLength).background(colors.exit, TipTopShape))
        Box(Modifier.align(Alignment.BottomEnd).size(FrameWidth, TipLength).background(colors.exit, TipBottomShape))
        Canvas(Modifier.align(Alignment.CenterStart).offset(x = BoardPadding).size(ArrowSize)) {
            val stroke = 3.dp.toPx()
            val arrow =
                Path().apply {
                    moveTo(size.width * 0.375f, size.height * 0.25f)
                    lineTo(size.width * 0.625f, size.height * 0.5f)
                    lineTo(size.width * 0.375f, size.height * 0.75f)
                }
            drawPath(arrow, colors.exit, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private val TipTopShape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
private val TipBottomShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)

@Composable
private fun BlockView(
    index: Int,
    block: Block,
    range: IntRange,
    hint: Move?,
    cell: Dp,
    onSlide: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSlideOutColors.current
    val cellPx = with(LocalDensity.current) { cell.toPx() }
    var drag by remember(block) { mutableFloatStateOf(0f) }
    val currentOnSlide by rememberUpdatedState(onSlide)
    val low = (range.first - block.position) * cellPx
    val high = (range.last - block.position) * cellPx
    val currentLow by rememberUpdatedState(low)
    val currentHigh by rememberUpdatedState(high)
    val currentCellPx by rememberUpdatedState(cellPx)
    val shift = drag.coerceIn(low, high).roundToInt()
    val (face, edge) =
        if (index == 0) {
            colors.target to colors.targetEdge
        } else {
            colors.block to colors.blockEdge
        }
    val width =
        if (block.horizontal) {
            cell * block.length
        } else {
            cell
        }
    val height =
        if (block.horizontal) {
            cell
        } else {
            cell * block.length
        }
    Box(
        modifier
            .offset(x = cell * block.col, y = cell * block.row)
            .offset {
                if (block.horizontal) {
                    IntOffset(shift, 0)
                } else {
                    IntOffset(0, shift)
                }
            }.size(width, height)
            .padding(Inset)
            .then(
                if (hint != null) {
                    Modifier.border(3.dp, colors.hint, BlockShape)
                } else {
                    Modifier
                },
            ).background(edge, BlockShape)
            .pointerInput(block) {
                detectDragGestures(
                    onDragEnd = {
                        val steps = (drag.coerceIn(currentLow, currentHigh) / currentCellPx).roundToInt()
                        drag = 0f
                        currentOnSlide(index, block.position + steps)
                    },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag +=
                        if (block.horizontal) {
                            amount.x
                        } else {
                            amount.y
                        }
                }
            }.testTag(blockTag(index)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(FACE_FRACTION)
                .align(Alignment.TopCenter)
                .background(face, BlockShape),
        )
    }
}

private const val FACE_FRACTION = 0.88f
