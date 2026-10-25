package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TileColors(
    val emptyTile: Color,
    val red: Color,
    val redEdge: Color,
    val blue: Color,
    val blueEdge: Color,
    val broken: Color,
    val hintLine: Color,
    val hintTarget: Color,
)

private val LightTiles =
    TileColors(
        emptyTile = Color(0xFFE6E9EE),
        red = Color(0xFFE0603F),
        redEdge = Color(0xFFB54A30),
        blue = Color(0xFF34A7C8),
        blueEdge = Color(0xFF2785A0),
        broken = Color(0xFF14181F),
        hintLine = Color(0xFFE5F0FE),
        hintTarget = Color(0xFF1565D8),
    )

private val DarkTiles =
    TileColors(
        emptyTile = Color(0xFF2A2E35),
        red = Color(0xFFD5583A),
        redEdge = Color(0xFFA6432C),
        blue = Color(0xFF2E9EBE),
        blueEdge = Color(0xFF227A93),
        broken = Color(0xFFF1F3F6),
        hintLine = Color(0xFF1A2B40),
        hintTarget = Color(0xFF7DB5FF),
    )

val LocalTileColors = staticCompositionLocalOf { LightTiles }

@Composable
fun TwoToneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tiles =
        if (darkTheme) {
            DarkTiles
        } else {
            LightTiles
        }
    CompositionLocalProvider(LocalTileColors provides tiles, content = content)
}
