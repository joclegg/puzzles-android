package com.joclegg.puzzles.fiveletters.feature.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.GameHero
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.todayLabel
import com.joclegg.puzzles.fiveletters.core.data.GameStatus
import com.joclegg.puzzles.fiveletters.core.ui.BackButton
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersGameIcon
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersIcons
import com.joclegg.puzzles.fiveletters.core.ui.LocalLetterColors
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenGame: (Long) -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }
    var nytUnavailable by remember { mutableStateOf(false) }

    fun launchGame(start: suspend () -> Long?) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            try {
                val id = start()
                nytUnavailable = id == null
                if (id != null) {
                    onOpenGame(id)
                }
            } finally {
                starting = false
            }
        }
    }
    HomeScreen(
        state = state,
        onDaily = { launchGame { viewModel.openDaily() } },
        onNytDaily = { launchGame { viewModel.openNytDaily() } },
        nytUnavailable = nytUnavailable,
        onPractice = {
            val practice = state.practice
            if (practice != null) {
                onOpenGame(practice.id)
            } else {
                launchGame { viewModel.startPractice() }
            }
        },
        onNewPractice = { launchGame { viewModel.startPractice() } },
        onStats = onStats,
        onSettings = onSettings,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onDaily: () -> Unit,
    onNytDaily: () -> Unit,
    nytUnavailable: Boolean,
    onPractice: () -> Unit,
    onNewPractice: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalLetterColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Five Letters", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onStats, modifier = Modifier.testTag("stats")) {
                Icon(MenuIcons.Stats, contentDescription = "Stats")
            }
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(FiveLettersIcons.Settings, contentDescription = "Settings")
            }
        }
        GameHero("Guess the word in six tries, a new one every day", accent) { FiveLettersGameIcon() }
        MenuSection("Today", aside = todayLabel()) {
            MenuTile(
                label = "Today's word · #${state.dailyNumber}",
                accent = accent,
                onClick = onDaily,
                modifier = Modifier.fillMaxWidth().testTag("daily"),
                detail = state.dailyResult.label(),
                icon = MenuIcons.Today,
                active = state.dailyResult == GameStatus.IN_PROGRESS,
                done = state.dailyResult == GameStatus.WON,
            )
            MenuTile(
                label = "NY Times daily",
                accent = accent,
                onClick = onNytDaily,
                modifier = Modifier.fillMaxWidth().testTag("nyt_daily"),
                detail = state.nytResult.label(),
                icon = MenuIcons.News,
                active = state.nytResult == GameStatus.IN_PROGRESS,
                done = state.nytResult == GameStatus.WON,
            )
            if (nytUnavailable) {
                Text(
                    "Couldn't reach the New York Times. Check your connection and try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        MenuSection("Practice") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile(
                    label =
                        if (state.practice != null) {
                            "Continue practice"
                        } else {
                            "Practice"
                        },
                    accent = accent,
                    onClick = onPractice,
                    modifier = Modifier.weight(1f).testTag("practice"),
                    icon = MenuIcons.Practice,
                    active = state.practice != null,
                )
                if (state.practice != null) {
                    MenuTile(
                        label = "New word",
                        accent = accent,
                        onClick = onNewPractice,
                        modifier = Modifier.weight(1f).testTag("new_practice"),
                        icon = MenuIcons.Restart,
                    )
                }
            }
        }
        Text(
            "Everyone with Puzzles gets the same word each day, so you can compare grids.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun GameStatus?.label(): String? =
    when (this) {
        GameStatus.WON -> "Solved"
        GameStatus.LOST -> "Missed today"
        GameStatus.IN_PROGRESS -> "In progress"
        GameStatus.ABANDONED, null -> null
    }
