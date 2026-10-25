package com.joclegg.puzzles.slideout.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SlideColors(
    val board: Color,
    val slot: Color,
    val block: Color,
    val blockEdge: Color,
    val target: Color,
    val targetEdge: Color,
    val exit: Color,
    val hint: Color,
)

private val Light =
    SlideColors(
        board = Color(0xFF263040),
        slot = Color(0xFF353A42),
        block = Color(0xFFD9B98A),
        blockEdge = Color(0xFFB09068),
        target = Color(0xFFE0603F),
        targetEdge = Color(0xFFB54A30),
        exit = Color(0xFFE0603F),
        hint = Color(0xFF8EC0FA),
    )

private val Dark =
    SlideColors(
        board = Color(0xFF15191F),
        slot = Color(0xFF22272E),
        block = Color(0xFFB59A72),
        blockEdge = Color(0xFF8C7354),
        target = Color(0xFFD5583A),
        targetEdge = Color(0xFFA6432C),
        exit = Color(0xFFD5583A),
        hint = Color(0xFF7DB5FF),
    )

val LocalSlideColors = staticCompositionLocalOf { Light }

@Composable
fun SlideOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            Dark
        } else {
            Light
        }
    CompositionLocalProvider(LocalSlideColors provides colors, content = content)
}
