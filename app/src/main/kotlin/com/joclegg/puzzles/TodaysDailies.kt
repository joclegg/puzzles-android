package com.joclegg.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.joclegg.puzzles.core.ui.DailyLeft
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes.openFiveLettersGame
import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.kakuro.KakuroRoutes
import com.joclegg.puzzles.kakuro.KakuroRoutes.openKakuroGame
import com.joclegg.puzzles.sightlines.SightlinesRoutes
import com.joclegg.puzzles.sightlines.SightlinesRoutes.openSightlinesGame
import com.joclegg.puzzles.slideout.SlideOutRoutes
import com.joclegg.puzzles.slideout.SlideOutRoutes.openSlideOutGame
import com.joclegg.puzzles.solitaire.SolitaireRoutes
import com.joclegg.puzzles.solitaire.SolitaireRoutes.openSolitaireGame
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.sudoku.SudokuRoutes
import com.joclegg.puzzles.sudoku.SudokuRoutes.openSudokuGame
import com.joclegg.puzzles.sudoku.core.data.DailyStatus
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.dailyDays
import com.joclegg.puzzles.sudoku.core.data.openDaily
import com.joclegg.puzzles.twotone.TwoToneRoutes
import com.joclegg.puzzles.twotone.TwoToneRoutes.openTwoToneGame
import com.joclegg.puzzles.wordwheel.WordWheelRoutes
import com.joclegg.puzzles.wordwheel.WordWheelRoutes.openWordWheelGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import com.joclegg.puzzles.fiveletters.core.data.GameStatus as FiveLettersStatus
import com.joclegg.puzzles.kakuro.core.data.GameStatus as KakuroStatus
import com.joclegg.puzzles.kakuro.core.data.PuzzleRef as KakuroRef
import com.joclegg.puzzles.kakuro.engine.SIZES as KAKURO_SIZES
import com.joclegg.puzzles.sightlines.core.data.GameStatus as SightlinesStatus
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef as SightlinesRef
import com.joclegg.puzzles.sightlines.engine.SIZES as SIGHTLINES_SIZES
import com.joclegg.puzzles.slideout.core.data.GameStatus as SlideOutStatus
import com.joclegg.puzzles.slideout.core.data.PuzzleRef as SlideOutRef
import com.joclegg.puzzles.slideout.engine.Level as SlideOutLevel
import com.joclegg.puzzles.solitaire.core.data.GameStatus as SolitaireStatus
import com.joclegg.puzzles.twotone.core.data.GameStatus as TwoToneStatus
import com.joclegg.puzzles.twotone.core.data.PuzzleRef as TwoToneRef
import com.joclegg.puzzles.twotone.engine.SIZES as TWO_TONE_SIZES
import com.joclegg.puzzles.wordwheel.core.data.GameStatus as WordWheelStatus
import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef as WordWheelRef
import com.joclegg.puzzles.wordwheel.engine.Level as WordWheelLevel

@Composable
fun rememberTodaysDailies(
    app: PuzzlesApplication,
    darkTheme: Boolean,
    navController: NavHostController,
): suspend () -> List<DailyLeft> {
    val accents = SettingsTab.entries.associateWith { settingsAccent(it, darkTheme) }
    val current by rememberUpdatedState(accents)
    val scope = rememberCoroutineScope()
    return { todaysDailies(app).map { it.toDailyLeft(current, navController, scope) } }
}

private class TodaysDaily(
    val tab: SettingsTab,
    val name: String?,
    val home: String,
    val open: suspend () -> Long,
    val show: NavHostController.(Long) -> Unit,
) {
    fun toDailyLeft(
        accents: Map<SettingsTab, Color>,
        navController: NavHostController,
        scope: CoroutineScope,
    ) = DailyLeft(
        listOfNotNull(tab.label, name).joinToString(" "),
        accents.getValue(tab),
    ) {
        scope.launch {
            val id = open()
            navController.navigate(home) {
                popUpTo(LAUNCHER)
                launchSingleTop = true
            }
            navController.show(id)
        }
    }
}

private suspend fun todaysDailies(app: PuzzlesApplication): List<TodaysDaily> =
    sudoku(app) + solitaire(app) + twoTone(app) + sightlines(app) + fiveLetters(app) + kakuro(app) + slideOut(app) + wordWheel(app)

private suspend fun sudoku(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.solitaire.today()
    val day =
        dailyDays(
            app.sudoku.games.history
                .first(),
            ZoneId.systemDefault(),
        )[today]
    return GameMode.entries
        .filter { mode -> (day?.status(mode) ?: DailyStatus.NOT_DONE) == DailyStatus.NOT_DONE }
        .map { mode ->
            TodaysDaily(SettingsTab.SUDOKU, mode.name.lowercase().replaceFirstChar(Char::uppercase), SudokuRoutes.HOME, {
                app.sudoku.games.openDaily(
                    app.sudoku.bank,
                    mode,
                    today,
                    app.sudoku.settings.settings
                        .first()
                        .mistakeCheckFor(mode),
                )
            }) { openSudokuGame(it) }
        }
}

private suspend fun solitaire(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.solitaire.today()
    val daily = DealRef.dailyId(today)
    if (app.solitaire.games.history
            .first()
            .any { it.deal.id == daily && it.status == SolitaireStatus.WON }
    ) {
        return emptyList()
    }
    return listOf(
        TodaysDaily(SettingsTab.SOLITAIRE, null, SolitaireRoutes.HOME, {
            app.solitaire.games.inProgress
                .first()
                .find { it.deal.id == daily }
                ?.id ?: run {
                val prefs =
                    app.solitaire.settings.settings
                        .first()
                app.solitaire.games
                    .start(DealRef.daily(today, prefs.drawMode, prefs.order, app.solitaire.winnableDeals.takeIf { prefs.winnableOnly }))
                    .id
            }
        }) { openSolitaireGame(it) },
    )
}

private suspend fun twoTone(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.twoTone.today()
    val history =
        app.twoTone.games.history
            .first()
    return TWO_TONE_SIZES
        .filter { size -> history.none { it.ref.id == TwoToneRef.dailyId(today, size) && it.status == TwoToneStatus.SOLVED } }
        .map { size ->
            TodaysDaily(SettingsTab.TWO_TONE, "$size×$size", TwoToneRoutes.HOME, {
                app.twoTone.games.inProgress
                    .first()
                    .find { it.ref.id == TwoToneRef.dailyId(today, size) }
                    ?.id ?: app.twoTone.games
                    .start { TwoToneRef.daily(today, size) }
                    .id
            }) { openTwoToneGame(it) }
        }
}

private suspend fun sightlines(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.sightlines.today()
    val history =
        app.sightlines.games.history
            .first()
    return SIGHTLINES_SIZES
        .filter { size -> history.none { it.ref.id == SightlinesRef.dailyId(today, size) && it.status == SightlinesStatus.SOLVED } }
        .map { size ->
            TodaysDaily(SettingsTab.SIGHTLINES, "$size×$size", SightlinesRoutes.HOME, {
                app.sightlines.games.inProgress
                    .first()
                    .find { it.ref.id == SightlinesRef.dailyId(today, size) }
                    ?.id ?: app.sightlines.games
                    .start { SightlinesRef.daily(today, size) }
                    .id
            }) { openSightlinesGame(it) }
        }
}

private suspend fun fiveLetters(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.fiveLetters.today()
    val daily = WordRef.dailyId(today)
    val history =
        app.fiveLetters.games.history
            .first()
    if (history.any { it.ref.id == daily && (it.status == FiveLettersStatus.WON || it.status == FiveLettersStatus.LOST) }) {
        return emptyList()
    }
    return listOf(
        TodaysDaily(SettingsTab.FIVE_LETTERS, null, FiveLettersRoutes.HOME, {
            app.fiveLetters.games.history
                .first()
                .find { it.ref.id == daily && it.status != FiveLettersStatus.ABANDONED }
                ?.id ?: app.fiveLetters.games
                .start(
                    WordRef.daily(app.fiveLetters.words(), today),
                    app.fiveLetters.settings.settings
                        .first()
                        .hardMode,
                ).id
        }) { openFiveLettersGame(it) },
    )
}

private suspend fun kakuro(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.kakuro.today()
    val history =
        app.kakuro.games.history
            .first()
    return KAKURO_SIZES
        .filter { size -> history.none { it.ref.id == KakuroRef.dailyId(today, size) && it.status == KakuroStatus.SOLVED } }
        .map { size ->
            TodaysDaily(SettingsTab.KAKURO, "$size×$size", KakuroRoutes.HOME, {
                app.kakuro.games.inProgress
                    .first()
                    .find { it.ref.id == KakuroRef.dailyId(today, size) }
                    ?.id ?: app.kakuro.games
                    .start { KakuroRef.daily(app.kakuro.bank, today, size) }
                    .id
            }) { openKakuroGame(it) }
        }
}

private suspend fun slideOut(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.slideOut.today()
    val history =
        app.slideOut.games.history
            .first()
    return SlideOutLevel.entries
        .filter { level -> history.none { it.ref.id == SlideOutRef.dailyId(today, level) && it.status == SlideOutStatus.SOLVED } }
        .map { level ->
            TodaysDaily(SettingsTab.SLIDE_OUT, level.name.lowercase().replaceFirstChar(Char::uppercase), SlideOutRoutes.HOME, {
                app.slideOut.games.inProgress
                    .first()
                    .find { it.ref.id == SlideOutRef.dailyId(today, level) }
                    ?.id ?: app.slideOut.games
                    .start { SlideOutRef.daily(app.slideOut.bank, today, level) }
                    .id
            }) { openSlideOutGame(it) }
        }
}

private suspend fun wordWheel(app: PuzzlesApplication): List<TodaysDaily> {
    val today = app.wordWheel.today()
    val history =
        app.wordWheel.games.history
            .first()
    return WordWheelLevel.entries
        .filter { level -> history.none { it.ref.id == WordWheelRef.dailyId(today, level) && it.status == WordWheelStatus.SOLVED } }
        .map { level ->
            TodaysDaily(SettingsTab.WORD_WHEEL, "${level.letters} letters", WordWheelRoutes.HOME, {
                app.wordWheel.games.inProgress
                    .first()
                    .find { it.ref.id == WordWheelRef.dailyId(today, level) }
                    ?.id ?: app.wordWheel.games
                    .start { WordWheelRef.daily(app.wordWheel.bank, today, level) }
                    .id
            }) { openWordWheelGame(it) }
        }
}
