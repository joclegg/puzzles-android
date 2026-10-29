package com.joclegg.puzzles.wordwheel.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WheelColors(
    val cell: Color,
    val cellFound: Color,
    val cellHinted: Color,
    val cellText: Color,
    val wheel: Color,
    val letter: Color,
    val letterText: Color,
    val chosen: Color,
    val chosenText: Color,
    val line: Color,
    val accent: Color,
)

private val Light =
    WheelColors(
        cell = Color(0xFFE6E9EE),
        cellFound = Color(0xFF1565D8),
        cellHinted = Color(0xFFBCD9FC),
        cellText = Color(0xFFFFFFFF),
        wheel = Color(0xFFE5F0FE),
        letter = Color(0xFFFFFFFF),
        letterText = Color(0xFF14181F),
        chosen = Color(0xFF1565D8),
        chosenText = Color(0xFFFFFFFF),
        line = Color(0xFF1565D8),
        accent = Color(0xFF4F5BD5),
    )

private val Dark =
    WheelColors(
        cell = Color(0xFF2A2E35),
        cellFound = Color(0xFF2F5E99),
        cellHinted = Color(0xFF244A78),
        cellText = Color(0xFFE9ECF1),
        wheel = Color(0xFF1A2B40),
        letter = Color(0xFF2A2E35),
        letterText = Color(0xFFE9ECF1),
        chosen = Color(0xFF7DB5FF),
        chosenText = Color(0xFF0B2545),
        line = Color(0xFF7DB5FF),
        accent = Color(0xFF939BFF),
    )

val LocalWheelColors = staticCompositionLocalOf { Light }

@Composable
fun WordWheelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            Dark
        } else {
            Light
        }
    CompositionLocalProvider(LocalWheelColors provides colors, content = content)
}
