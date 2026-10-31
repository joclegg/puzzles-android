package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BoardColors(
    val background: Color,
    val givenCell: Color,
    val doneDigit: Color,
    val enteredDigit: Color,
    val noteDigit: Color,
    val highlightedNote: Color,
    val selected: Color,
    val sameDigit: Color,
    val peer: Color,
    val wrong: Color,
    val trialMark: Color,
    val thinLine: Color,
    val thickLine: Color,
    val cageLine: Color,
    val cageSum: Color,
    val accent: Color,
)

private val LightBoard =
    BoardColors(
        background = Color(0xFFFFFFFF),
        givenCell = Color(0xFFD5D8DD),
        doneDigit = Color(0x667A808A),
        enteredDigit = Color(0xFF14181F),
        noteDigit = Color(0xFF4A515B),
        highlightedNote = Color(0xFF1565D8),
        selected = Color(0xFF8EC0FA),
        sameDigit = Color(0xFFBCD9FC),
        peer = Color(0xFFE5F0FE),
        wrong = Color(0xFFD32F2F),
        trialMark = Color(0xFFE53935),
        thinLine = Color(0xFFA7AFBB),
        thickLine = Color(0xFF263040),
        cageLine = Color(0xFF5F6670),
        cageSum = Color(0xFF262C35),
        accent = Color(0xFF1565D8),
    )

private val DarkBoard =
    BoardColors(
        background = Color(0xFF121418),
        givenCell = Color(0xFF353A42),
        doneDigit = Color(0x668C939F),
        enteredDigit = Color(0xFFE9ECF1),
        noteDigit = Color(0xFFB8BEC8),
        highlightedNote = Color(0xFF7DB5FF),
        selected = Color(0xFF2F5E99),
        sameDigit = Color(0xFF244A78),
        peer = Color(0xFF1A2B40),
        wrong = Color(0xFFFF6B6B),
        trialMark = Color(0xFFFF5252),
        thinLine = Color(0xFF555E6C),
        thickLine = Color(0xFFB4BFCF),
        cageLine = Color(0xFFA2A9B4),
        cageSum = Color(0xFFDDE1E7),
        accent = Color(0xFF7DB5FF),
    )

private val Blue = Color(0xFF1565D8)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme =
        if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF7DB5FF),
                onPrimary = Color(0xFF0B2545),
                secondaryContainer = DarkBoard.peer,
                surfaceVariant = DarkBoard.peer,
                surfaceContainerHighest = DarkBoard.peer,
                onSecondaryContainer = DarkBoard.enteredDigit,
                background = DarkBoard.background,
                surface = DarkBoard.background,
            )
        } else {
            lightColorScheme(
                primary = Blue,
                onPrimary = Color.White,
                secondaryContainer = LightBoard.peer,
                surfaceVariant = LightBoard.peer,
                surfaceContainerHighest = LightBoard.peer,
                onSecondaryContainer = LightBoard.enteredDigit,
                background = LightBoard.background,
                surface = LightBoard.background,
            )
        }
    CompositionLocalProvider(LocalBoardColors provides if (darkTheme) {
DarkBoard
} else {
LightBoard
}) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
