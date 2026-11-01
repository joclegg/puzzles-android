package com.joclegg.puzzles.slideout.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.home.HomeOptions
import com.joclegg.puzzles.core.ui.home.StandardHomeSections
import com.joclegg.puzzles.core.ui.home.rememberGameLauncher
import com.joclegg.puzzles.slideout.core.data.SavedGame
import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.ui.LocalSlideColors
import com.joclegg.puzzles.slideout.ui.SlideOutGameIcon
import com.joclegg.puzzles.slideout.ui.SlideOutIcons
import com.joclegg.puzzles.slideout.ui.label
import com.joclegg.puzzles.slideout.ui.title

private val homeOptions =
    HomeOptions<Level, SavedGame>(
        options = Level.entries,
        label = { it.label() },
        dailyTag = { "daily_${it.name.lowercase()}" },
        newTag = { "level_${it.name.lowercase()}" },
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
    val accent = LocalSlideColors.current.accent
    GameHomeScaffold(
        title = "Slide Out",
        calendarGame = "SLIDE_OUT",
        tagline = "Slide the blocks to free the red one",
        accent = accent,
        settingsIcon = SlideOutIcons.Settings,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { SlideOutGameIcon() },
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
