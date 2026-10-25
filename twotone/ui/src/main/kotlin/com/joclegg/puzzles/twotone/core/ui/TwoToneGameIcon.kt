package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TwoToneGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalTileColors.current
    val squares = listOf(colors.red to null, colors.blue to null, colors.blue to null, colors.red to null)
    Column(modifier.size(56.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        squares.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (colour, label) -> Square(colour, label) }
            }
        }
    }
}

@Composable
private fun Square(
    colour: Color,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTileColors.current
    Box(modifier.size(26.dp).background(colour, RoundedCornerShape(5.dp))) {
        if (label != null) {
            Text(
                label,
                color = colors.emptyTile,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
