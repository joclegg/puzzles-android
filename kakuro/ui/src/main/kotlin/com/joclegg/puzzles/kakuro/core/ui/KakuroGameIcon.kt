package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KakuroGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalKakuroColors.current
    Box(modifier.size(56.dp)) {
        Canvas(Modifier.size(56.dp)) {
            val cell = size.width / 2
            drawRect(colors.block, Offset.Zero, Size(cell, cell))
            drawRect(colors.background, Offset(cell, 0f), Size(cell, cell))
            drawRect(colors.background, Offset(0f, cell), Size(cell, cell))
            drawRect(colors.block, Offset(cell, cell), Size(cell, cell))
            drawLine(colors.line, Offset.Zero, Offset(cell, cell), strokeWidth = 1.dp.toPx())
            for (line in 0..2) {
                drawLine(colors.line, Offset(line * cell, 0f), Offset(line * cell, size.height), strokeWidth = 1.dp.toPx())
                drawLine(colors.line, Offset(0f, line * cell), Offset(size.width, line * cell), strokeWidth = 1.dp.toPx())
            }
        }
        Box(Modifier.align(Alignment.TopEnd).size(28.dp), contentAlignment = Alignment.Center) {
            Text("1", color = colors.digit, fontSize = 18.sp)
        }
        Box(Modifier.align(Alignment.BottomStart).size(28.dp), contentAlignment = Alignment.Center) {
            Text("2", color = colors.digit, fontSize = 18.sp)
        }
    }
}
