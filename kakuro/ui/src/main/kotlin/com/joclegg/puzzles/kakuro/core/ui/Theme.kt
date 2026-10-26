package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class KakuroColors(
    val background: Color,
    val block: Color,
    val clue: Color,
    val line: Color,
    val givenCell: Color,
    val digit: Color,
    val note: Color,
    val selected: Color,
    val run: Color,
    val sameDigit: Color,
    val wrong: Color,
    val trialMark: Color,
)

private val Light =
    KakuroColors(
        background = Color(0xFFFFFFFF),
        block = Color(0xFF263040),
        clue = Color(0xFFF1F3F6),
        line = Color(0xFFA7AFBB),
        givenCell = Color(0xFFD5D8DD),
        digit = Color(0xFF14181F),
        note = Color(0xFF4A515B),
        selected = Color(0xFF8EC0FA),
        run = Color(0xFFE5F0FE),
        sameDigit = Color(0xFFBCD9FC),
        wrong = Color(0xFFD32F2F),
        trialMark = Color(0xFFE53935),
    )

private val Dark =
    KakuroColors(
        background = Color(0xFF121418),
        block = Color(0xFF2A2E35),
        clue = Color(0xFFDDE1E7),
        line = Color(0xFF555E6C),
        givenCell = Color(0xFF353A42),
        digit = Color(0xFFE9ECF1),
        note = Color(0xFFB8BEC8),
        selected = Color(0xFF2F5E99),
        run = Color(0xFF1A2B40),
        sameDigit = Color(0xFF244A78),
        wrong = Color(0xFFFF6B6B),
        trialMark = Color(0xFFFF5252),
    )

val LocalKakuroColors = staticCompositionLocalOf { Light }

@Composable
fun KakuroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            Dark
        } else {
            Light
        }
    CompositionLocalProvider(LocalKakuroColors provides colors, content = content)
}
