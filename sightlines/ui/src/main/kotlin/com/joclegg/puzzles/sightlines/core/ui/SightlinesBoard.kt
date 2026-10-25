package com.joclegg.puzzles.sightlines.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.sightlines.engine.Dot
import com.joclegg.puzzles.sightlines.engine.GameState
import com.joclegg.puzzles.sightlines.engine.Grid
import com.joclegg.puzzles.sightlines.engine.Hint

@Immutable
data class BoardUi(
    val grid: Grid,
    val numbers: Map<Int, Int>,
    val walls: Set<Int>,
    val broken: Set<Int> = emptySet(),
    val hintSight: Set<Int> = emptySet(),
    val hintTargets: Set<Int> = emptySet(),
    val seenNow: Map<Int, Int> = emptyMap(),
)

fun GameState.toBoardUi(
    markBrokenRules: Boolean,
    showSeenCount: Boolean = false,
): BoardUi {
    val currentHint = hint
    return BoardUi(
        grid = grid,
        numbers = puzzle.numbers,
        walls = puzzle.walls,
        broken =
            if (markBrokenRules) {
                brokenCells
            } else {
                emptySet()
            },
        hintSight =
            if (currentHint is Hint.Next) {
                currentHint.cells.toSet()
            } else {
                emptySet()
            },
        hintTargets =
            when (currentHint) {
                is Hint.Next -> currentHint.deduction.cells.toSet()
                is Hint.Mistake -> currentHint.cells.toSet()
                null -> emptySet()
            },
        seenNow =
            if (showSeenCount) {
                puzzle.numbers.keys.associateWith(grid::seen)
            } else {
                emptyMap()
            },
    )
}

private val DotGap = 5.dp
private val DotShape = RoundedCornerShape(8.dp)

@Composable
fun SightlinesBoard(
    board: BoardUi,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = board.grid.size
    Column(modifier.aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(DotGap)) {
        for (row in 0 until size) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(DotGap)) {
                for (col in 0 until size) {
                    DotView(board, row * size + col, onDotClick, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun DotView(
    board: BoardUi,
    cell: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTileColors.current
    val dot = board.grid.dots[cell]
    val number = board.numbers[cell]
    val given = number != null || cell in board.walls
    val broken = cell in board.broken
    val hinted = cell in board.hintTargets
    val (face, edge) =
        when (dot) {
            Dot.RED -> {
                colors.red to colors.redEdge
            }

            Dot.BLUE -> {
                colors.blue to colors.blueEdge
            }

            Dot.EMPTY -> {
                if (cell in board.hintSight) {
                    colors.hintLine to colors.hintLine
                } else {
                    colors.emptyTile to colors.emptyTile
                }
            }
        }
    BoxWithConstraints(
        modifier
            .clip(DotShape)
            .background(edge)
            .then(
                if (hinted) {
                    Modifier.border(3.dp, colors.hintTarget, DotShape)
                } else {
                    Modifier
                },
            ).clickable { onDotClick(cell) }
            .testTag(dotTag(cell, board.grid.size))
            .semantics {
                this.dot = dot
                dotGiven = given
                dotBroken = broken
                dotHint = hinted
            },
    ) {
        val textSize = with(LocalDensity.current) { (maxWidth * NUMBER_FRACTION).toSp() }
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(EDGE_FACE_FRACTION)
                .align(Alignment.TopCenter)
                .clip(DotShape)
                .background(face),
        ) {
            if (number != null) {
                Text(
                    number.toString(),
                    color = colors.number,
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            board.seenNow[cell]?.let { seen ->
                Text(
                    seen.toString(),
                    color = colors.number,
                    fontSize = textSize * SEEN_SCALE,
                    modifier =
                        Modifier
                            .align(
                                Alignment.TopEnd,
                            ).padding(end = 5.dp, top = 2.dp)
                            .testTag("${dotTag(cell, board.grid.size)}_seen"),
                )
            }
            if (broken) {
                Box(Modifier.fillMaxSize(BROKEN_FRACTION).align(Alignment.Center).border(2.dp, colors.broken, CircleShape))
            }
        }
    }
}

private const val EDGE_FACE_FRACTION = 0.9f
private const val BROKEN_FRACTION = 0.75f
private const val NUMBER_FRACTION = 0.42f
private const val SEEN_SCALE = 0.42f
