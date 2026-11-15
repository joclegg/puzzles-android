package com.joclegg.puzzles.crossword.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun CrosswordGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalCrosswordColors.current
    Canvas(modifier.size(56.dp)) {
        val cell = size.width / ICON_CELLS
        for (row in 0 until ICON_CELLS) {
            for (col in 0 until ICON_CELLS) {
                val color =
                    if ((row * ICON_CELLS + col) in ICON_BLOCKS) {
                        colors.block
                    } else {
                        colors.background
                    }
                drawRect(color, Offset(col * cell, row * cell), Size(cell, cell))
            }
        }
        for (line in 0..ICON_CELLS) {
            drawLine(colors.line, Offset(line * cell, 0f), Offset(line * cell, size.height), strokeWidth = 1.dp.toPx())
            drawLine(colors.line, Offset(0f, line * cell), Offset(size.width, line * cell), strokeWidth = 1.dp.toPx())
        }
    }
}

private const val ICON_CELLS = 4
private val ICON_BLOCKS = setOf(1, 7, 8, 14)
