package com.joclegg.puzzles.core.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.ContinueCard
import com.joclegg.puzzles.core.ui.DailyCalendarButton
import com.joclegg.puzzles.core.ui.DailyTile
import com.joclegg.puzzles.core.ui.GameHero
import com.joclegg.puzzles.core.ui.LevelMeter
import com.joclegg.puzzles.core.ui.MenuIcons
import com.joclegg.puzzles.core.ui.MenuSection
import com.joclegg.puzzles.core.ui.MenuTile
import com.joclegg.puzzles.core.ui.TileGrid
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.core.ui.todayLabel
import kotlinx.coroutines.launch

class GameLauncher internal constructor(
    private val launch: (suspend () -> Long?) -> Unit,
) {
    operator fun invoke(start: suspend () -> Long?) = launch(start)
}

@Composable
fun rememberGameLauncher(
    onOpenGame: (Long) -> Unit,
    onResult: (Long?) -> Unit = {},
): GameLauncher {
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }
    return GameLauncher { start ->
        if (!starting) {
            starting = true
            scope.launch {
                try {
                    val id = start()
                    onResult(id)
                    if (id != null) {
                        onOpenGame(id)
                    }
                } finally {
                    starting = false
                }
            }
        }
    }
}

@Composable
fun GameHomeScaffold(
    title: String,
    calendarGame: String,
    tagline: String,
    accent: Color,
    settingsIcon: ImageVector,
    onStats: () -> Unit,
    onHistory: (() -> Unit)?,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    gameIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    statsLabel: String = "Stats",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            DailyCalendarButton(calendarGame)
            IconButton(onClick = onStats, modifier = Modifier.testTag("stats")) {
                Icon(MenuIcons.Stats, contentDescription = statsLabel)
            }
            if (onHistory != null) {
                IconButton(onClick = onHistory, modifier = Modifier.testTag("history")) {
                    Icon(MenuIcons.History, contentDescription = "History")
                }
            }
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(settingsIcon, contentDescription = "Settings")
            }
        }
        GameHero(tagline, accent) { gameIcon() }
        content()
    }
}

class HomeOptions<O, G>(
    val options: List<O>,
    val label: (O) -> String,
    val dailyTag: (O) -> String,
    val newTag: (O) -> String,
    val gameId: (G) -> Long,
    val gameTitle: (G) -> String,
    val gameElapsedMs: (G) -> Long,
    val gameOption: (G) -> O,
    val dailyColumns: Int = 3,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <O, G : Any> ColumnScope.StandardHomeSections(
    config: HomeOptions<O, G>,
    latest: G?,
    dailySolved: Set<O>,
    savedFor: (O) -> G?,
    accent: Color,
    onResume: (Long) -> Unit,
    onStart: (O) -> Unit,
    onDaily: (O) -> Unit,
) {
    var resumeChoice by remember { mutableStateOf<G?>(null) }
    val options = config.options
    latest?.let { game ->
        ContinueCard(
            detail = "${config.gameTitle(game)} · ${formatDuration(config.gameElapsedMs(game))}",
            accent = accent,
            onClick = { onResume(config.gameId(game)) },
            modifier = Modifier.testTag("continue"),
        )
    }
    MenuSection("Today", aside = todayLabel()) {
        TileGrid(count = options.size, columns = config.dailyColumns) { index, tileModifier ->
            val option = options[index]
            DailyTile(
                label = config.label(option),
                done = option in dailySolved,
                accent = accent,
                onClick = { onDaily(option) },
                modifier = tileModifier.testTag(config.dailyTag(option)),
            )
        }
    }
    MenuSection("New puzzle") {
        TileGrid(count = options.size, columns = 2) { index, tileModifier ->
            val option = options[index]
            val saved = savedFor(option)
            MenuTile(
                label = config.label(option),
                accent = accent,
                onClick = {
                    if (saved != null) {
                        resumeChoice = saved
                    } else {
                        onStart(option)
                    }
                },
                modifier = tileModifier.testTag(config.newTag(option)),
                detail = saved?.let { "In progress · ${formatDuration(config.gameElapsedMs(it))}" },
                active = saved != null,
                meter = { LevelMeter(rank = index, count = options.size, accent = accent) },
            )
        }
    }
    resumeChoice?.let { saved ->
        AlertDialog(
            onDismissRequest = { resumeChoice = null },
            modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("resume_dialog"),
            title = { Text(config.gameTitle(saved)) },
            text = { Text("You have a puzzle in progress (${formatDuration(config.gameElapsedMs(saved))}).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onResume(config.gameId(saved))
                    },
                    modifier = Modifier.testTag("resume_game"),
                ) { Text("Resume") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resumeChoice = null
                        onStart(config.gameOption(saved))
                    },
                    modifier = Modifier.testTag("new_game"),
                ) { Text("New puzzle") }
            },
        )
    }
}
