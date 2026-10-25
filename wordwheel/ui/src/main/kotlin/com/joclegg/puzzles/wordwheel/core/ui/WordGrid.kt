package com.joclegg.puzzles.wordwheel.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.joclegg.puzzles.wordwheel.engine.Crossword

@Immutable
data class GridUi(
    val crossword: Crossword,
    val found: Set<Pair<Int, Int>>,
    val hinted: Set<Pair<Int, Int>>,
)

@Composable
fun WordGrid(
    grid: GridUi,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWheelColors.current
    val letters = grid.crossword.letters
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cell = min(maxWidth / grid.crossword.cols, maxHeight / grid.crossword.rows).coerceAtMost(MAX_CELL.dp)
        Box(Modifier.size(cell * grid.crossword.cols, cell * grid.crossword.rows)) {
            letters.forEach { (position, letter) ->
                val (row, col) = position
                val shown = position in grid.found || position in grid.hinted
                val background =
                    when (position) {
                        in grid.found -> colors.cellFound
                        in grid.hinted -> colors.cellHinted
                        else -> colors.cell
                    }
                Box(
                    Modifier
                        .offset(x = cell * col, y = cell * row)
                        .size(cell)
                        .padding(2.dp)
                        .background(background, RoundedCornerShape(4.dp))
                        .testTag(cellTag(row, col)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (shown) {
                        Text(
                            letter.toString(),
                            color =
                                if (position in grid.found) {
                                    colors.cellText
                                } else {
                                    colors.letterText
                                },
                            fontWeight = FontWeight.Bold,
                            fontSize = with(LocalDensity.current) { (cell * LETTER_FRACTION).toSp() },
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_CELL = 40
private const val LETTER_FRACTION = 0.55f
