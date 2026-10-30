package com.joclegg.puzzles

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersTheme
import com.joclegg.puzzles.fiveletters.core.ui.LocalLetterColors
import com.joclegg.puzzles.kakuro.core.ui.KakuroTheme
import com.joclegg.puzzles.kakuro.core.ui.LocalKakuroColors
import com.joclegg.puzzles.sightlines.core.ui.SightlinesTheme
import com.joclegg.puzzles.slideout.core.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.core.ui.SlideOutTheme
import com.joclegg.puzzles.solitaire.core.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTheme
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.twotone.core.ui.TwoToneTheme
import com.joclegg.puzzles.wordwheel.core.ui.LocalWheelColors
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelTheme
import com.joclegg.puzzles.sightlines.core.ui.LocalTileColors as SightlinesColors
import com.joclegg.puzzles.twotone.core.ui.LocalTileColors as TwoToneColors

@Composable
fun settingsAccent(
    tab: SettingsTab,
    darkTheme: Boolean,
): Color {
    var accent = LocalBoardColors.current.accent
    when (tab) {
        SettingsTab.GENERAL, SettingsTab.SUDOKU -> {
            Unit
        }

        SettingsTab.SOLITAIRE -> {
            SolitaireTheme(darkTheme) { accent = LocalTableColors.current.accent }
        }

        SettingsTab.TWO_TONE -> {
            TwoToneTheme(darkTheme) { accent = TwoToneColors.current.accent }
        }

        SettingsTab.SIGHTLINES -> {
            SightlinesTheme(darkTheme) { accent = SightlinesColors.current.accent }
        }

        SettingsTab.FIVE_LETTERS -> {
            FiveLettersTheme(darkTheme) { accent = LocalLetterColors.current.accent }
        }

        SettingsTab.KAKURO -> {
            KakuroTheme(darkTheme) { accent = LocalKakuroColors.current.accent }
        }

        SettingsTab.SLIDE_OUT -> {
            SlideOutTheme(darkTheme) { accent = LocalSlideColors.current.accent }
        }

        SettingsTab.WORD_WHEEL -> {
            WordWheelTheme(darkTheme) { accent = LocalWheelColors.current.accent }
        }
    }
    return accent
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
