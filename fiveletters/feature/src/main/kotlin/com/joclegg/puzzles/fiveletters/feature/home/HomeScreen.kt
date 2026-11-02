package com.joclegg.puzzles.fiveletters.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.todayLabel
import com.joclegg.puzzles.fiveletters.core.data.GameStatus
import com.joclegg.puzzles.fiveletters.feature.R
import com.joclegg.puzzles.fiveletters.ui.FiveLettersGameIcon
import com.joclegg.puzzles.fiveletters.ui.FiveLettersIcons
import com.joclegg.puzzles.fiveletters.ui.LocalFiveLettersColors
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
    var daily2Unavailable by remember { mutableStateOf(false) }

    fun launchGame(start: suspend () -> Long?) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            try {
                val id = start()
                daily2Unavailable = id == null
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
        onDaily2 = { launchGame { viewModel.openDaily2() } },
        daily2Unavailable = daily2Unavailable,
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
    onDaily2: () -> Unit,
    daily2Unavailable: Boolean,
    onPractice: () -> Unit,
    onNewPractice: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalFiveLettersColors.current.accent
    GameHomeScaffold(
        title = stringResource(R.string.five_letters),
        calendarGame = "FIVE_LETTERS",
        tagline = stringResource(R.string.guess_the_word_in_six_tries_a_new_one),
        accent = accent,
        settingsIcon = FiveLettersIcons.Settings,
        onStats = onStats,
        onHistory = null,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { FiveLettersGameIcon() },
        modifier = modifier,
    ) {
        MenuSection(stringResource(R.string.today), aside = todayLabel()) {
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
                label = stringResource(R.string.daily_puzzle_2),
                accent = accent,
                onClick = onDaily2,
                modifier = Modifier.fillMaxWidth().testTag("daily2"),
                detail = state.daily2Result.label(),
                icon = MenuIcons.News,
                active = state.daily2Result == GameStatus.IN_PROGRESS,
                done = state.daily2Result == GameStatus.WON,
            )
            if (daily2Unavailable) {
                Text(
                    stringResource(R.string.couldn_t_load_daily_puzzle_2_check),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        MenuSection(stringResource(R.string.practice)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile(
                    label =
                        if (state.practice != null) {
                            stringResource(R.string.continue_practice)
                        } else {
                            stringResource(R.string.practice)
                        },
                    accent = accent,
                    onClick = onPractice,
                    modifier = Modifier.weight(1f).testTag("practice"),
                    icon = MenuIcons.Practice,
                    active = state.practice != null,
                )
                if (state.practice != null) {
                    MenuTile(
                        label = stringResource(R.string.new_word),
                        accent = accent,
                        onClick = onNewPractice,
                        modifier = Modifier.weight(1f).testTag("new_practice"),
                        icon = MenuIcons.Restart,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.everyone_with_puzzles_gets_the_same),
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
