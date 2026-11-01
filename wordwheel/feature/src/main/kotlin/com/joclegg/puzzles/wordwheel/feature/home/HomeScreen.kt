package com.joclegg.puzzles.wordwheel.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.home.HomeOptions
import com.joclegg.puzzles.core.ui.home.StandardHomeSections
import com.joclegg.puzzles.core.ui.home.rememberGameLauncher
import com.joclegg.puzzles.wordwheel.core.data.SavedGame
import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.ui.LocalWheelColors
import com.joclegg.puzzles.wordwheel.ui.WordWheelGameIcon
import com.joclegg.puzzles.wordwheel.ui.WordWheelIcons
import com.joclegg.puzzles.wordwheel.ui.label
import com.joclegg.puzzles.wordwheel.ui.title

private val homeOptions =
    HomeOptions<Level, SavedGame>(
        options = Level.entries,
        label = { it.label() },
        dailyTag = { "daily_${it.letters}" },
        newTag = { "level_${it.letters}" },
        gameId = { it.id },
        gameTitle = { it.ref.title() },
        gameElapsedMs = { it.elapsedMs },
        gameOption = { it.ref.level },
        dailyColumns = 3,
    )

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
    val launchGame = rememberGameLauncher(onOpenGame)
    HomeScreen(
        state = state,
        onResume = onOpenGame,
        onStart = { level -> launchGame { viewModel.start(level) } },
        onDaily = { level -> launchGame { viewModel.openDaily(level) } },
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
    onStart: (Level) -> Unit,
    onDaily: (Level) -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalWheelColors.current.accent
    GameHomeScaffold(
        title = "Word Wheel",
        calendarGame = "WORD_WHEEL",
        tagline = "Swipe the letters to fill the crossword",
        accent = accent,
        settingsIcon = WordWheelIcons.Settings,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { WordWheelGameIcon() },
        modifier = modifier,
    ) {
        StandardHomeSections(
            config = homeOptions,
            latest = state.inProgress.firstOrNull(),
            dailySolved = state.dailySolved,
            savedFor = state::savedFor,
            accent = accent,
            onResume = onResume,
            onStart = onStart,
            onDaily = onDaily,
        )
    }
}
