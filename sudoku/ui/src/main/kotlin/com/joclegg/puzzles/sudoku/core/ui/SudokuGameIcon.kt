package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun SudokuGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalBoardColors.current
    Canvas(modifier.size(56.dp)) {
        val cell = size.width / CELLS
        drawRoundRect(colors.background, cornerRadius = CornerRadius(cell / 2))
        drawRect(colors.givenCell, topLeft = Offset(0f, 0f), size = Size(cell, cell))
        drawRect(colors.givenCell, topLeft = Offset(cell * 2, cell * 2), size = Size(cell, cell))
        drawRect(colors.selected, topLeft = Offset(cell, cell), size = Size(cell, cell))
        for (line in 0..CELLS) {
            val at = line * cell
            drawLine(colors.thickLine, Offset(at, 0f), Offset(at, size.height), strokeWidth = 2.dp.toPx())
            drawLine(colors.thickLine, Offset(0f, at), Offset(size.width, at), strokeWidth = 2.dp.toPx())
        }
    }
}

private const val CELLS = 3
