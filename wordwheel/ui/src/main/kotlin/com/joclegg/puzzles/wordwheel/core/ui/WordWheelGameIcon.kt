package com.joclegg.puzzles.wordwheel.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WordWheelGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalWheelColors.current
    Box(modifier.size(56.dp).background(colors.wheel, CircleShape)) {
        listOf("W" to (19 to 2), "O" to (35 to 19), "R" to (19 to 36), "D" to (2 to 19)).forEach { (letter, at) ->
            Box(
                Modifier.offset(x = at.first.dp, y = at.second.dp).size(18.dp).background(colors.chosen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, color = colors.chosenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
