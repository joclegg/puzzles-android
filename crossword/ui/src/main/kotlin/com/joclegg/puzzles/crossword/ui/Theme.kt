package com.joclegg.puzzles.crossword.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CrosswordColors(
    val background: Color,
    val block: Color,
    val line: Color,
    val letter: Color,
    val number: Color,
    val selected: Color,
    val word: Color,
    val wrong: Color,
    val accent: Color,
)

private val Light =
    CrosswordColors(
        background = Color(0xFFFFFFFF),
        block = Color(0xFF1B232C),
        line = Color(0xFF8C96A3),
        letter = Color(0xFF14181F),
        number = Color(0xFF4A515B),
        selected = Color(0xFF8CCBE0),
        word = Color(0xFFDDF0F6),
        wrong = Color(0xFFD32F2F),
        accent = Color(0xFF2E7D9A),
    )

private val Dark =
    CrosswordColors(
        background = Color(0xFF1C2026),
        block = Color(0xFF050608),
        line = Color(0xFF555E6C),
        letter = Color(0xFFE9ECF1),
        number = Color(0xFFB8BEC8),
        selected = Color(0xFF2F6F87),
        word = Color(0xFF1D3640),
        wrong = Color(0xFFFF6B6B),
        accent = Color(0xFF6FB8D2),
    )

val LocalCrosswordColors = staticCompositionLocalOf { Light }

@Composable
fun CrosswordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            Dark
        } else {
            Light
        }
    CompositionLocalProvider(LocalCrosswordColors provides colors, content = content)
}
