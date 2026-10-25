package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.twotone.engine.GameState
import com.joclegg.puzzles.twotone.engine.Grid
import com.joclegg.puzzles.twotone.engine.Hint
import com.joclegg.puzzles.twotone.engine.Tile

@Immutable
data class BoardUi(
    val grid: Grid,
    val givens: Set<Int>,
    val broken: Set<Int> = emptySet(),
    val hintLine: Set<Int> = emptySet(),
    val hintTargets: Set<Int> = emptySet(),
)

fun GameState.toBoardUi(markBrokenRules: Boolean): BoardUi {
    val currentHint = hint
    return BoardUi(
        grid = grid,
        givens =
            grid.tiles.indices
                .filter(puzzle::isGiven)
                .toSet(),
        broken =
            if (markBrokenRules) {
                brokenCells
            } else {
                emptySet()
            },
        hintLine =
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
    )
}

private val TileGap = 5.dp
private val TileShape = RoundedCornerShape(6.dp)

@Composable
fun TwoToneBoard(
    board: BoardUi,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = board.grid.size
    Column(modifier.aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(TileGap)) {
        for (row in 0 until size) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                for (col in 0 until size) {
                    val cell = row * size + col
                    TileView(board, cell, onTileClick, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun TileView(
    board: BoardUi,
    cell: Int,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTileColors.current
    val tile = board.grid.tiles[cell]
    val given = cell in board.givens
    val broken = cell in board.broken
    val hinted = cell in board.hintTargets
    val (face, edge) =
        when (tile) {
            Tile.RED -> {
                colors.red to colors.redEdge
            }

            Tile.BLUE -> {
                colors.blue to colors.blueEdge
            }

            Tile.EMPTY -> {
                if (cell in board.hintLine) {
                    colors.hintLine to colors.hintLine
                } else {
                    colors.emptyTile to colors.emptyTile
                }
            }
        }
    Box(
        modifier
            .clip(TileShape)
            .background(edge)
            .then(
                when {
                    hinted -> Modifier.border(3.dp, colors.hintTarget, TileShape)
                    broken -> Modifier.border(3.dp, colors.broken, TileShape)
                    else -> Modifier
                },
            ).clickable { onTileClick(cell) }
            .testTag(tileTag(cell, board.grid.size))
            .semantics {
                this.tile = tile
                tileGiven = given
                tileBroken = broken
                tileHint = hinted
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(EDGE_FACE_FRACTION)
                .align(Alignment.TopCenter)
                .clip(TileShape)
                .background(face),
        ) {
        }
    }
}

private const val EDGE_FACE_FRACTION = 0.9f
