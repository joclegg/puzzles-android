package com.joclegg.puzzles.solitaire.core.ui

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
data class TableColors(
    val felt: Color,
    val emptyPile: Color,
    val cardFace: Color,
    val cardEdge: Color,
    val cardBack: Color,
    val cardBackPattern: Color,
    val blackSuit: Color,
    val redSuit: Color,
    val selected: Color,
    val onFelt: Color,
)

private val LightTable =
    TableColors(
        felt = Color(0xFF2E6B4A),
        emptyPile = Color(0x33FFFFFF),
        cardFace = Color(0xFFFFFFFF),
        cardEdge = Color(0xFFA7AFBB),
        cardBack = Color(0xFF1565D8),
        cardBackPattern = Color(0xFF8EC0FA),
        blackSuit = Color(0xFF14181F),
        redSuit = Color(0xFFC62828),
        selected = Color(0xFF8EC0FA),
        onFelt = Color(0xFFF1F5F2),
    )

private val DarkTable =
    TableColors(
        felt = Color(0xFF15301F),
        emptyPile = Color(0x26FFFFFF),
        cardFace = Color(0xFFE3E6EA),
        cardEdge = Color(0xFF555E6C),
        cardBack = Color(0xFF2F5E99),
        cardBackPattern = Color(0xFF7DB5FF),
        blackSuit = Color(0xFF121418),
        redSuit = Color(0xFFB3261E),
        selected = Color(0xFF7DB5FF),
        onFelt = Color(0xFFDDE1E7),
    )

private val Blue = Color(0xFF1565D8)

val LocalTableColors = staticCompositionLocalOf { LightTable }

@Composable
fun SolitaireTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme =
        if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF7DB5FF),
                onPrimary = Color(0xFF0B2545),
                secondaryContainer = Color(0xFF1A2B40),
                onSecondaryContainer = Color(0xFFE9ECF1),
                background = Color(0xFF121418),
                surface = Color(0xFF121418),
            )
        } else {
            lightColorScheme(
                primary = Blue,
                onPrimary = Color.White,
                secondaryContainer = Color(0xFFE5F0FE),
                onSecondaryContainer = Color(0xFF14181F),
                background = Color.White,
                surface = Color.White,
            )
        }
    val table =
        if (darkTheme) {
            DarkTable
        } else {
            LightTable
        }
    CompositionLocalProvider(LocalTableColors provides table) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
