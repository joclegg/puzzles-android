package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.core.sync.SignInProvider
import com.joclegg.puzzles.core.ui.GameCard
import com.joclegg.puzzles.core.ui.Medal
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.medalColour
import com.joclegg.puzzles.fiveletters.ui.FiveLettersGameIcon
import com.joclegg.puzzles.fiveletters.ui.FiveLettersTheme
import com.joclegg.puzzles.fiveletters.ui.LocalFiveLettersColors
import com.joclegg.puzzles.kakuro.ui.KakuroGameIcon
import com.joclegg.puzzles.kakuro.ui.KakuroTheme
import com.joclegg.puzzles.kakuro.ui.LocalKakuroColors
import com.joclegg.puzzles.sightlines.ui.LocalSightlinesColors
import com.joclegg.puzzles.sightlines.ui.SightlinesGameIcon
import com.joclegg.puzzles.sightlines.ui.SightlinesTheme
import com.joclegg.puzzles.slideout.ui.LocalSlideOutColors
import com.joclegg.puzzles.slideout.ui.SlideOutGameIcon
import com.joclegg.puzzles.slideout.ui.SlideOutTheme
import com.joclegg.puzzles.solitaire.ui.LocalSolitaireColors
import com.joclegg.puzzles.solitaire.ui.SolitaireGameIcon
import com.joclegg.puzzles.solitaire.ui.SolitaireTheme
import com.joclegg.puzzles.sudoku.ui.LocalSudokuColors
import com.joclegg.puzzles.sudoku.ui.SudokuGameIcon
import com.joclegg.puzzles.sudoku.ui.SudokuIcons
import com.joclegg.puzzles.twotone.ui.LocalTwoToneColors
import com.joclegg.puzzles.twotone.ui.TwoToneGameIcon
import com.joclegg.puzzles.twotone.ui.TwoToneTheme
import com.joclegg.puzzles.wordwheel.ui.LocalWordWheelColors
import com.joclegg.puzzles.wordwheel.ui.WordWheelGameIcon
import com.joclegg.puzzles.wordwheel.ui.WordWheelTheme

@Composable
fun LauncherScreen(
    darkTheme: Boolean,
    account: String?,
    onSignInClick: (SignInProvider) -> Unit,
    onSendEmailLink: suspend (String) -> Boolean,
    onVerifyEmailCode: suspend (String, String) -> Boolean,
    onSignOutClick: () -> Unit,
    medals: Map<PuzzleGame, Medal>,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
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
        if (holdsEveryGold(medals)) {
            Row(
                Modifier.fillMaxWidth().testTag("all_golds_trophy"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MenuIcons.Trophy, contentDescription = null, tint = medalColour(Medal.GOLD), modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.gold_in_every_puzzle),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.puzzles), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            if (account == null) {
                OutlinedButton(onClick = { choosingSignIn = true }, modifier = Modifier.testTag("sign_in")) {
                    Text(stringResource(R.string.sign_in_to_sync))
                }
            } else {
                OutlinedButton(onClick = onSignOutClick, modifier = Modifier.testTag("sign_out")) {
                    Text(stringResource(R.string.sign_out))
                }
            }
            IconButton(onClick = onHistoryClick, modifier = Modifier.testTag("history")) {
                Icon(MenuIcons.History, contentDescription = stringResource(R.string.history))
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings")) {
                Icon(SudokuIcons.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
        GameCard(
            stringResource(R.string.sudoku),
            stringResource(R.string.classic_and_killer_from_6x6_to_evil),
            LocalSudokuColors.current.accent,
            onSudokuClick,
            Modifier.testTag("game_sudoku"),
            medals[PuzzleGame.SUDOKU] ?: Medal.NONE,
        ) {
            SudokuGameIcon()
        }
        SolitaireTheme(darkTheme) {
            GameCard(
                stringResource(R.string.solitaire),
                stringResource(R.string.klondike_draw_1_or_3_or_reverse),
                LocalSolitaireColors.current.accent,
                onSolitaireClick,
                Modifier.testTag("game_solitaire"),
                medals[PuzzleGame.SOLITAIRE] ?: Medal.NONE,
            ) {
                SolitaireGameIcon()
            }
        }
        TwoToneTheme(darkTheme) {
            GameCard(
                stringResource(R.string.two_tone),
                stringResource(R.string.fill_the_grid_red_and_blue_three),
                LocalTwoToneColors.current.accent,
                onTwoToneClick,
                Modifier.testTag("game_two_tone"),
                medals[PuzzleGame.TWO_TONE] ?: Medal.NONE,
            ) {
                TwoToneGameIcon()
            }
        }
        SightlinesTheme(darkTheme) {
            GameCard(
                stringResource(R.string.sightlines),
                stringResource(R.string.blue_dots_count_what_they_can_see),
                LocalSightlinesColors.current.accent,
                onSightlinesClick,
                Modifier.testTag("game_sightlines"),
                medals[PuzzleGame.SIGHTLINES] ?: Medal.NONE,
            ) {
                SightlinesGameIcon()
            }
        }
        FiveLettersTheme(darkTheme) {
            GameCard(
                stringResource(R.string.five_letters),
                stringResource(R.string.guess_the_word_in_six_tries_a_new_one),
                LocalFiveLettersColors.current.accent,
                onFiveLettersClick,
                Modifier.testTag("game_five_letters"),
                medals[PuzzleGame.FIVE_LETTERS] ?: Medal.NONE,
            ) {
                FiveLettersGameIcon()
            }
        }
        KakuroTheme(darkTheme) {
            GameCard(
                stringResource(R.string.kakuro),
                stringResource(R.string.cross_sums_runs_of_different_digits),
                LocalKakuroColors.current.accent,
                onKakuroClick,
                Modifier.testTag("game_kakuro"),
                medals[PuzzleGame.KAKURO] ?: Medal.NONE,
            ) {
                KakuroGameIcon()
            }
        }
        SlideOutTheme(darkTheme) {
            GameCard(
                stringResource(R.string.slide_out),
                stringResource(R.string.slide_the_blocks_to_free_the_red_one),
                LocalSlideOutColors.current.accent,
                onSlideOutClick,
                Modifier.testTag("game_slide_out"),
                medals[PuzzleGame.SLIDE_OUT] ?: Medal.NONE,
            ) {
                SlideOutGameIcon()
            }
        }
        WordWheelTheme(darkTheme) {
            GameCard(
                stringResource(R.string.word_wheel),
                stringResource(R.string.swipe_the_letters_to_fill_the),
                LocalWordWheelColors.current.accent,
                onWordWheelClick,
                Modifier.testTag("game_word_wheel"),
                medals[PuzzleGame.WORD_WHEEL] ?: Medal.NONE,
            ) {
                WordWheelGameIcon()
            }
        }
    }
}
