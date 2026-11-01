package com.joclegg.puzzles

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.joclegg.puzzles.sudoku.ui.LocalBoardColors

@Composable
fun settingsAccent(
    app: PuzzlesApplication,
    tab: SettingsTab,
    darkTheme: Boolean,
): Color =
    if (tab == SettingsTab.GENERAL) {
        LocalBoardColors.current.accent
    } else {
        app.definition(PuzzleGame.valueOf(tab.name)).accent(darkTheme)
    }

@Composable
fun AccentColors(
    accent: Color,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme =
            colors.copy(
                primary = accent,
                onPrimary = colors.surface,
                secondaryContainer = accent.copy(alpha = 0.25f),
                onSecondaryContainer = colors.onSurface,
            ),
        content = content,
    )
}
