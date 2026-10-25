package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MedalColour { GOLD, SILVER }

@Composable
fun MedalBadge(
    colour: MedalColour,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val colors = LocalBoardColors.current
    val fill =
        if (colour == MedalColour.GOLD) {
            colors.medalGold
        } else {
            colors.medalSilver
        }
    Box(
        modifier
            .size(size)
            .background(fill, CircleShape)
            .border(size / 8, Color.White.copy(alpha = 0.6f), CircleShape),
    )
}
