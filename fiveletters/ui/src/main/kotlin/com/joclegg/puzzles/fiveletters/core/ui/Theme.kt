package com.joclegg.puzzles.fiveletters.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class LetterColors(
    val correct: Color,
    val present: Color,
    val absent: Color,
    val markedText: Color,
    val emptyBorder: Color,
    val typedBorder: Color,
    val text: Color,
    val key: Color,
    val keyText: Color,
    val accent: Color,
)

private val Light =
    LetterColors(
        correct = Color(0xFF5FA463),
        present = Color(0xFFD7B23E),
        absent = Color(0xFF7A808A),
        markedText = Color.White,
        emptyBorder = Color(0xFFD5D8DD),
        typedBorder = Color(0xFF7A808A),
        text = Color(0xFF14181F),
        key = Color(0xFFE2E5EA),
        keyText = Color(0xFF14181F),
        accent = Color(0xFF4F9A53),
    )

private val Dark =
    LetterColors(
        correct = Color(0xFF538D4E),
        present = Color(0xFFB59F3B),
        absent = Color(0xFF3A3F47),
        markedText = Color.White,
        emptyBorder = Color(0xFF3A3F47),
        typedBorder = Color(0xFF7A808A),
        text = Color(0xFFE9ECF1),
        key = Color(0xFF555E6C),
        keyText = Color(0xFFE9ECF1),
        accent = Color(0xFF72BF6D),
    )

private val LightContrast = Light.copy(correct = Color(0xFFE5793A), present = Color(0xFF6CB7E8))

private val DarkContrast = Dark.copy(correct = Color(0xFFE5793A), present = Color(0xFF6CB7E8))

val LocalLetterColors = staticCompositionLocalOf { Light }

@Composable
fun FiveLettersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors =
        when {
            darkTheme && highContrast -> DarkContrast
            darkTheme -> Dark
            highContrast -> LightContrast
            else -> Light
        }
    CompositionLocalProvider(LocalLetterColors provides colors, content = content)
}
