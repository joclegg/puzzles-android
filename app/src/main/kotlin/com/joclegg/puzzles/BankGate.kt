package com.joclegg.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object BankGate {
    const val ROUTE = "banks/{game}"

    fun route(game: PuzzleGame) = "banks/${game.name}"
}

fun PuzzlesApplication.hasBanksFor(game: PuzzleGame): Boolean = definition(game).bankFolder?.let(banks::isReady) ?: true

@Composable
fun BankGateRoute(
    app: PuzzlesApplication,
    game: PuzzleGame,
    onReady: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status by app.bankStatus.collectAsStateWithLifecycle()
    val ready = remember(status) { app.hasBanksFor(game) }
    val currentOnReady by rememberUpdatedState(onReady)
    LaunchedEffect(ready) {
        if (ready) {
            currentOnReady()
        } else {
            app.updateBanks()
        }
    }
    if (!ready) {
        BankDownloadScreen(failed = !status.updating, onRetry = app::updateBanks, onBack = onBack, modifier = modifier)
    }
}
