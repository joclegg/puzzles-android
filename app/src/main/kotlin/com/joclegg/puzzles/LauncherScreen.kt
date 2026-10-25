package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LauncherScreen(
    onSudokuClick: () -> Unit,
    onSolitaireClick: () -> Unit,
    onTwoToneClick: () -> Unit,
    onSightlinesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Puzzles", style = MaterialTheme.typography.headlineLarge)
        GameCard("Sudoku", "Classic and killer, from 6x6 to evil", onSudokuClick, Modifier.testTag("game_sudoku"))
        GameCard("Solitaire", "Klondike, draw 1 or 3, or reverse", onSolitaireClick, Modifier.testTag("game_solitaire"))
        GameCard("Two Tone", "Fill the grid red and blue, three simple rules", onTwoToneClick, Modifier.testTag("game_two_tone"))
        GameCard("Sightlines", "Blue dots count what they can see", onSightlinesClick, Modifier.testTag("game_sightlines"))
    }
}

@Composable
private fun GameCard(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
