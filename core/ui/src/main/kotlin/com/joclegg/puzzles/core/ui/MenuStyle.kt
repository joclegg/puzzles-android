package com.joclegg.puzzles.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun tileColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

@Composable
internal fun tileBorder(
    accent: Color,
    highlighted: Boolean,
): BorderStroke =
    if (highlighted) {
        BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    }

@Composable
internal fun pressScale(interactionSource: InteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue =
            if (pressed) {
                0.97f
            } else {
                1f
            },
        label = "press",
    )
    return scale
}
