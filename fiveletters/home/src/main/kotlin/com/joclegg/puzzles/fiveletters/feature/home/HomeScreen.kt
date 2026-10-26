package com.joclegg.puzzles.fiveletters.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.joclegg.puzzles.fiveletters.core.data.GameStatus
import com.joclegg.puzzles.fiveletters.core.ui.BackButton
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersIcons
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
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Five Letters", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Icon(FiveLettersIcons.Settings, contentDescription = "Settings")
            }
        }
        Button(onClick = onDaily, modifier = Modifier.fillMaxWidth().testTag("daily")) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today's word · #${state.dailyNumber}")
                state.dailyResult.label()?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
        OutlinedButton(onClick = onNytDaily, modifier = Modifier.fillMaxWidth().testTag("nyt_daily")) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NY Times daily")
                state.nytResult.label()?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
        if (nytUnavailable) {
            Text(
                "Couldn't reach the New York Times. Check your connection and try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPractice, modifier = Modifier.weight(1f).testTag("practice")) {
                Text(
                    if (state.practice != null) {
                        "Continue practice"
                    } else {
                        "Practice"
                    },
                )
            }
            if (state.practice != null) {
                OutlinedButton(onClick = onNewPractice, modifier = Modifier.weight(1f).testTag("new_practice")) { Text("New word") }
            }
        }
        OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth().testTag("stats")) { Text("Stats") }
        Text(
            "Everyone with Puzzles gets the same word each day, so you can compare grids.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun GameStatus?.label(): String? =
    when (this) {
        GameStatus.WON -> "Solved ✓"
        GameStatus.LOST -> "Missed today"
        GameStatus.IN_PROGRESS -> "In progress"
        GameStatus.ABANDONED, null -> null
    }
