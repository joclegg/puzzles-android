package com.joclegg.puzzles.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SudokuGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalSudokuColors.current
    Box(modifier.size(56.dp)) {
        Canvas(Modifier.size(56.dp)) {
            val cell = size.width / CELLS
            drawRect(colors.background)
            for (line in 1 until CELLS) {
                val at = line * cell
                drawLine(colors.thinLine, Offset(at, 0f), Offset(at, size.height), strokeWidth = 1.dp.toPx())
                drawLine(colors.thinLine, Offset(0f, at), Offset(size.width, at), strokeWidth = 1.dp.toPx())
            }
            for (edge in listOf(0f, size.width)) {
                drawLine(colors.thickLine, Offset(edge, 0f), Offset(edge, size.height), strokeWidth = 3.dp.toPx())
                drawLine(colors.thickLine, Offset(0f, edge), Offset(size.width, edge), strokeWidth = 3.dp.toPx())
            }
        }
        DIGITS.forEachIndexed { index, digit ->
            if (digit != null) {
                Box(
                    Modifier.offset(x = (index % CELLS * CELL_DP).dp, y = (index / CELLS * CELL_DP).dp).size(CELL_DP.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(digit, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private const val CELLS = 3
private const val CELL_DP = 56f / CELLS
private val DIGITS = listOf("5", null, "3", null, "7", null, "1", null, "9")
