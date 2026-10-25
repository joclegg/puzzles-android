package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersGameIcon
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersTheme
import com.joclegg.puzzles.kakuro.core.ui.KakuroGameIcon
import com.joclegg.puzzles.kakuro.core.ui.KakuroTheme
import com.joclegg.puzzles.sightlines.core.ui.SightlinesGameIcon
import com.joclegg.puzzles.sightlines.core.ui.SightlinesTheme
import com.joclegg.puzzles.solitaire.core.ui.SolitaireGameIcon
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTheme
import com.joclegg.puzzles.sudoku.core.ui.SudokuGameIcon
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.twotone.core.ui.TwoToneGameIcon
import com.joclegg.puzzles.twotone.core.ui.TwoToneTheme
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelGameIcon
import com.joclegg.puzzles.wordwheel.core.ui.WordWheelTheme

@Composable
fun LauncherScreen(
    darkTheme: Boolean,
    onSettingsClick: () -> Unit,
    onSudokuClick: () -> Unit,
    onSolitaireClick: () -> Unit,
    onTwoToneClick: () -> Unit,
    onSightlinesClick: () -> Unit,
    onFiveLettersClick: () -> Unit,
    onKakuroClick: () -> Unit,
    onWordWheelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Puzzles", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings")) {
                Icon(SudokuIcons.Settings, contentDescription = "Settings")
            }
        }
        GameCard("Sudoku", "Classic and killer, from 6x6 to evil", onSudokuClick, Modifier.testTag("game_sudoku")) {
            SudokuGameIcon()
        }
        GameCard("Solitaire", "Klondike, draw 1 or 3, or reverse", onSolitaireClick, Modifier.testTag("game_solitaire")) {
            SolitaireTheme(darkTheme) { SolitaireGameIcon() }
        }
        GameCard("Two Tone", "Fill the grid red and blue, three simple rules", onTwoToneClick, Modifier.testTag("game_two_tone")) {
            TwoToneTheme(darkTheme) { TwoToneGameIcon() }
        }
        GameCard("Sightlines", "Blue dots count what they can see", onSightlinesClick, Modifier.testTag("game_sightlines")) {
            SightlinesTheme(darkTheme) { SightlinesGameIcon() }
        }
        GameCard(
            "Five Letters",
            "Guess the word in six tries, a new one every day",
            onFiveLettersClick,
            Modifier.testTag("game_five_letters"),
        ) {
            FiveLettersTheme(darkTheme) { FiveLettersGameIcon() }
        }
        GameCard("Kakuro", "Cross sums: runs of different digits that add up", onKakuroClick, Modifier.testTag("game_kakuro")) {
            KakuroTheme(darkTheme) { KakuroGameIcon() }
        }
        GameCard("Word Wheel", "Swipe the letters to fill the crossword", onWordWheelClick, Modifier.testTag("game_word_wheel")) {
            WordWheelTheme(darkTheme) { WordWheelGameIcon() }
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            icon()
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
