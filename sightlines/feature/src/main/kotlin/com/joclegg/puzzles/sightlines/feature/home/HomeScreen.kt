package com.joclegg.puzzles.sightlines.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.home.HomeOptions
import com.joclegg.puzzles.core.ui.home.StandardHomeSections
import com.joclegg.puzzles.core.ui.home.rememberGameLauncher
import com.joclegg.puzzles.sightlines.core.data.SavedGame
import com.joclegg.puzzles.sightlines.engine.SIZES
import com.joclegg.puzzles.sightlines.feature.R
import com.joclegg.puzzles.sightlines.ui.LocalSightlinesColors
import com.joclegg.puzzles.sightlines.ui.SightlinesGameIcon
import com.joclegg.puzzles.sightlines.ui.SightlinesIcons
import com.joclegg.puzzles.sightlines.ui.sizeLabel
import com.joclegg.puzzles.sightlines.ui.title

private val homeOptions =
    HomeOptions<Int, SavedGame>(
        options = SIZES,
        label = ::sizeLabel,
        dailyTag = { "daily_$it" },
        newTag = { "size_$it" },
        gameId = { it.id },
        gameTitle = { it.ref.title() },
        gameElapsedMs = { it.elapsedMs },
        gameOption = { it.ref.size },
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
    val accent = LocalSightlinesColors.current.accent
    GameHomeScaffold(
        title = stringResource(R.string.sightlines),
        calendarGame = "SIGHTLINES",
        tagline = stringResource(R.string.blue_dots_count_what_they_can_see),
        accent = accent,
        settingsIcon = SightlinesIcons.Settings,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { SightlinesGameIcon() },
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
