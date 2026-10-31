package com.joclegg.puzzles.kakuro.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.joclegg.puzzles.core.ui.ContinueCard
import com.joclegg.puzzles.core.ui.DailyCalendarButton
import com.joclegg.puzzles.core.ui.DailyTile
import com.joclegg.puzzles.core.ui.GameHero
import com.joclegg.puzzles.core.ui.LevelMeter
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.TileGrid
import com.joclegg.puzzles.core.ui.todayLabel
import com.joclegg.puzzles.kakuro.core.data.SavedGame
import com.joclegg.puzzles.kakuro.core.ui.BackButton
import com.joclegg.puzzles.kakuro.core.ui.KakuroGameIcon
import com.joclegg.puzzles.kakuro.core.ui.KakuroIcons
import com.joclegg.puzzles.kakuro.core.ui.LocalKakuroColors
import com.joclegg.puzzles.kakuro.core.ui.dialogTestTags
import com.joclegg.puzzles.kakuro.core.ui.formatDuration
import com.joclegg.puzzles.kakuro.core.ui.sizeLabel
import com.joclegg.puzzles.kakuro.core.ui.title
import com.joclegg.puzzles.kakuro.engine.SIZES
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenGame: (Long) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }

    fun launchGame(start: suspend () -> Long) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            try {
                onOpenGame(start())
            } finally {
                starting = false
            }
        }
    }
    HomeScreen(
        state = state,
        onResume = onOpenGame,
        onStart = { size -> launchGame { viewModel.start(size) } },
        onDaily = { size -> launchGame { viewModel.openDaily(size) } },
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onResume: (Long) -> Unit,
    onStart: (Int) -> Unit,
    onDaily: (Int) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resumeChoice by remember { mutableStateOf<SavedGame?>(null) }
    val accent = LocalKakuroColors.current.accent
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Kakuro", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            DailyCalendarButton("KAKURO")
            IconButton(onClick = onStats, modifier = Modifier.testTag("stats")) {
                Icon(MenuIcons.Stats, contentDescription = "Stats")
            }
            IconButton(onClick = onHistory, modifier = Modifier.testTag("history")) {
                Icon(MenuIcons.History, contentDescription = "History")
            }
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(KakuroIcons.Settings, contentDescription = "Settings")
            }
        }
        GameHero("Cross sums: runs of different digits that add up", accent) { KakuroGameIcon() }
        state.inProgress.firstOrNull()?.let { latest ->
            ContinueCard(
                detail = "${latest.ref.title()} · ${formatDuration(latest.elapsedMs)}",
                accent = accent,
                onClick = { onResume(latest.id) },
                modifier = Modifier.testTag("continue"),
            )
        }
        MenuSection("Today", aside = todayLabel()) {
            TileGrid(count = SIZES.size, columns = SIZES.size) { index, tileModifier ->
                val size = SIZES[index]
                DailyTile(
                    label = sizeLabel(size),
                    done = size in state.dailySolved,
                    accent = accent,
                    onClick = { onDaily(size) },
                    modifier = tileModifier.testTag("daily_$size"),
                )
            }
        }
        MenuSection("New puzzle") {
            TileGrid(count = SIZES.size, columns = 2) { index, tileModifier ->
                val size = SIZES[index]
                val saved = state.savedFor(size)
                MenuTile(
                    label = sizeLabel(size),
                    accent = accent,
                    onClick = {
                        if (saved != null) {
                            resumeChoice = saved
                        } else {
                            onStart(size)
                        }
                    },
                    modifier = tileModifier.testTag("size_$size"),
                    detail = saved?.let { "In progress · ${formatDuration(it.elapsedMs)}" },
                    active = saved != null,
                    meter = { LevelMeter(rank = index, count = SIZES.size, accent = accent) },
                )
            }
        }
    }
    resumeChoice?.let { saved ->
        AlertDialog(
            onDismissRequest = { resumeChoice = null },
            modifier = Modifier.dialogTestTags().testTag("resume_dialog"),
            title = { Text(saved.ref.title()) },
            text = { Text("You have a puzzle in progress (${formatDuration(saved.elapsedMs)}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onResume(saved.id)
                    },
                    modifier = Modifier.testTag("resume_game"),
                ) { Text("Resume") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onStart(saved.ref.size)
                    },
                    modifier = Modifier.testTag("new_game"),
                ) { Text("New puzzle") }
            },
        )
    }
}
