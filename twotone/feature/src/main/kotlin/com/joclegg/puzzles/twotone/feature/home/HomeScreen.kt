package com.joclegg.puzzles.twotone.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.home.GameHomeScaffold
import com.joclegg.puzzles.core.ui.home.HomeOptions
import com.joclegg.puzzles.core.ui.home.StandardHomeSections
import com.joclegg.puzzles.core.ui.home.rememberGameLauncher
import com.joclegg.puzzles.twotone.core.data.SavedGame
import com.joclegg.puzzles.twotone.engine.SIZES
import com.joclegg.puzzles.twotone.ui.LocalTwoToneColors
import com.joclegg.puzzles.twotone.ui.TwoToneGameIcon
import com.joclegg.puzzles.twotone.ui.TwoToneIcons
import com.joclegg.puzzles.twotone.ui.sizeLabel
import com.joclegg.puzzles.twotone.ui.title

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
    val accent = LocalTwoToneColors.current.accent
    GameHomeScaffold(
        title = "Two Tone",
        calendarGame = "TWO_TONE",
        tagline = "Fill the grid red and blue, three simple rules",
        accent = accent,
        settingsIcon = TwoToneIcons.Settings,
        onStats = onStats,
        onHistory = onHistory,
        onSettings = onSettings,
        onBack = onBack,
        gameIcon = { TwoToneGameIcon() },
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
