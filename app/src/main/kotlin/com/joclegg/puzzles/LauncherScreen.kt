package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.core.sync.SignInProvider
import com.joclegg.puzzles.core.ui.GameCard
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersGameIcon
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersTheme
import com.joclegg.puzzles.fiveletters.core.ui.LocalLetterColors
import com.joclegg.puzzles.kakuro.core.ui.KakuroGameIcon
import com.joclegg.puzzles.kakuro.core.ui.KakuroTheme
import com.joclegg.puzzles.kakuro.core.ui.LocalKakuroColors
import com.joclegg.puzzles.sightlines.core.ui.SightlinesGameIcon
import com.joclegg.puzzles.sightlines.core.ui.SightlinesTheme
import com.joclegg.puzzles.slideout.core.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.core.ui.SlideOutGameIcon
import com.joclegg.puzzles.slideout.core.ui.SlideOutTheme
import com.joclegg.puzzles.solitaire.core.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.core.ui.SolitaireGameIcon
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTheme
import com.joclegg.puzzles.sudoku.core.ui.LocalBoardColors
import com.joclegg.puzzles.sudoku.core.ui.SudokuGameIcon
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.twotone.core.ui.TwoToneGameIcon
import com.joclegg.puzzles.twotone.core.ui.TwoToneTheme
import com.joclegg.puzzles.wordwheel.core.ui.LocalWheelColors
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelGameIcon
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelTheme
import com.joclegg.puzzles.sightlines.core.ui.LocalTileColors as SightlinesColors
import com.joclegg.puzzles.twotone.core.ui.LocalTileColors as TwoToneColors

@Composable
fun LauncherScreen(
    darkTheme: Boolean,
    account: String?,
    onSignInClick: (SignInProvider) -> Unit,
    onSendEmailLink: suspend (String) -> Boolean,
    onVerifyEmailCode: suspend (String, String) -> Boolean,
    onSignOutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSudokuClick: () -> Unit,
    onSolitaireClick: () -> Unit,
    onTwoToneClick: () -> Unit,
    onSightlinesClick: () -> Unit,
    onFiveLettersClick: () -> Unit,
    onKakuroClick: () -> Unit,
    onSlideOutClick: () -> Unit,
    onWordWheelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosingSignIn by remember { mutableStateOf(false) }
    if (choosingSignIn) {
        SignInDialog(
            darkTheme = darkTheme,
            onProviderClick = { provider ->
                choosingSignIn = false
                onSignInClick(provider)
            },
            onSendEmailLink = onSendEmailLink,
            onVerifyEmailCode = onVerifyEmailCode,
            onDismiss = { choosingSignIn = false },
        )
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Puzzles", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (account == null) {
                OutlinedButton(onClick = { choosingSignIn = true }, modifier = Modifier.testTag("sign_in")) {
                    Text("Sign in to sync")
                }
            } else {
                OutlinedButton(onClick = onSignOutClick, modifier = Modifier.testTag("sign_out")) {
                    Text("Sign out")
                }
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings")) {
                Icon(SudokuIcons.Settings, contentDescription = "Settings")
            }
        }
        GameCard(
            "Sudoku",
            "Classic and killer, from 6x6 to evil",
            LocalBoardColors.current.accent,
            onSudokuClick,
            Modifier.testTag("game_sudoku"),
        ) {
            SudokuGameIcon()
        }
        SolitaireTheme(darkTheme) {
            GameCard(
                "Solitaire",
                "Klondike, draw 1 or 3, or reverse",
                LocalTableColors.current.accent,
                onSolitaireClick,
                Modifier.testTag("game_solitaire"),
            ) {
                SolitaireGameIcon()
            }
        }
        TwoToneTheme(darkTheme) {
            GameCard(
                "Two Tone",
                "Fill the grid red and blue, three simple rules",
                TwoToneColors.current.accent,
                onTwoToneClick,
                Modifier.testTag("game_two_tone"),
            ) {
                TwoToneGameIcon()
            }
        }
        SightlinesTheme(darkTheme) {
            GameCard(
                "Sightlines",
                "Blue dots count what they can see",
                SightlinesColors.current.accent,
                onSightlinesClick,
                Modifier.testTag("game_sightlines"),
            ) {
                SightlinesGameIcon()
            }
        }
        FiveLettersTheme(darkTheme) {
            GameCard(
                "Five Letters",
                "Guess the word in six tries, a new one every day",
                LocalLetterColors.current.accent,
                onFiveLettersClick,
                Modifier.testTag("game_five_letters"),
            ) {
                FiveLettersGameIcon()
            }
        }
        KakuroTheme(darkTheme) {
            GameCard(
                "Kakuro",
                "Cross sums: runs of different digits that add up",
                LocalKakuroColors.current.accent,
                onKakuroClick,
                Modifier.testTag("game_kakuro"),
            ) {
                KakuroGameIcon()
            }
        }
        SlideOutTheme(darkTheme) {
            GameCard(
                "Slide Out",
                "Slide the blocks to free the red one",
                LocalSlideColors.current.accent,
                onSlideOutClick,
                Modifier.testTag("game_slide_out"),
            ) {
                SlideOutGameIcon()
            }
        }
        WordWheelTheme(darkTheme) {
            GameCard(
                "Word Wheel",
                "Swipe the letters to fill the crossword",
                LocalWheelColors.current.accent,
                onWordWheelClick,
                Modifier.testTag("game_word_wheel"),
            ) {
                WordWheelGameIcon()
            }
        }
    }
}
