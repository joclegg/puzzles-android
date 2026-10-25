package com.joclegg.puzzles.fiveletters.core.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FiveLettersGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalLetterColors.current
    val tiles = listOf("W" to colors.correct, "O" to colors.present, "R" to colors.absent, "D" to colors.correct)
    Column(modifier.size(56.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (letter, colour) ->
                    Box(Modifier.size(26.dp).background(colour, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text(letter, color = colors.markedText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
