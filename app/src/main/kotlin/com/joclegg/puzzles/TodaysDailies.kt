package com.joclegg.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.joclegg.puzzles.core.ui.DailyChoice
import com.joclegg.puzzles.core.ui.DailyLeft
import com.joclegg.puzzles.core.ui.DailyResult
import com.joclegg.puzzles.core.ui.Medal
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
import com.joclegg.puzzles.sudoku.core.data.DailyIds
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.levels
import com.joclegg.puzzles.sudoku.core.data.openDaily
import com.joclegg.puzzles.twotone.TwoToneRoutes
import com.joclegg.puzzles.twotone.TwoToneRoutes.openTwoToneGame
import com.joclegg.puzzles.wordwheel.WordWheelRoutes
import com.joclegg.puzzles.wordwheel.WordWheelRoutes.openWordWheelGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.joclegg.puzzles.fiveletters.core.data.GameStatus as FiveLettersStatus
import com.joclegg.puzzles.kakuro.core.data.GameStatus as KakuroStatus
import com.joclegg.puzzles.kakuro.core.data.PuzzleRef as KakuroRef
import com.joclegg.puzzles.kakuro.core.ui.sizeLabel as kakuroSizeLabel
import com.joclegg.puzzles.kakuro.engine.SIZES as KAKURO_SIZES
import com.joclegg.puzzles.sightlines.core.data.GameStatus as SightlinesStatus
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef as SightlinesRef
import com.joclegg.puzzles.sightlines.core.ui.sizeLabel as sightlinesSizeLabel
import com.joclegg.puzzles.sightlines.engine.SIZES as SIGHTLINES_SIZES
import com.joclegg.puzzles.slideout.core.data.GameStatus as SlideOutStatus
import com.joclegg.puzzles.slideout.core.data.PuzzleRef as SlideOutRef
import com.joclegg.puzzles.slideout.core.ui.label as slideOutLabel
import com.joclegg.puzzles.slideout.engine.Level as SlideOutLevel
import com.joclegg.puzzles.solitaire.core.data.GameStatus as SolitaireStatus
import com.joclegg.puzzles.sudoku.core.data.GameStatus as SudokuStatus
import com.joclegg.puzzles.sudoku.core.ui.label as sudokuLabel
import com.joclegg.puzzles.twotone.core.data.GameStatus as TwoToneStatus
import com.joclegg.puzzles.twotone.core.data.PuzzleRef as TwoToneRef
import com.joclegg.puzzles.twotone.core.ui.sizeLabel as twotoneSizeLabel
import com.joclegg.puzzles.twotone.engine.SIZES as TWO_TONE_SIZES
import com.joclegg.puzzles.wordwheel.core.data.GameStatus as WordWheelStatus
import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef as WordWheelRef
import com.joclegg.puzzles.wordwheel.core.ui.label as wordWheelLabel
import com.joclegg.puzzles.wordwheel.engine.Level as WordWheelLevel

@Composable
fun rememberTodaysDailies(
    app: PuzzlesApplication,
    navController: NavHostController,
): suspend (String) -> List<DailyLeft> {
    val scope = rememberCoroutineScope()
    return { group ->
        val tab = CALENDAR_TABS.filter { group.startsWith(it.name) }.maxBy { it.name.length }
        dailiesOn(app, tab, LocalDate.now())
            .filter { it.group == group && it.offered }
            .map { daily -> DailyLeft(daily.label, daily.opener(navController, scope)) }
    }
}

fun dailyChoices(
    dailies: List<GameDaily>,
    navController: NavHostController,
    scope: CoroutineScope,
    onChosen: () -> Unit,
): List<DailyChoice> =
    dailies.map { daily ->
        val open = daily.opener(navController, scope)
        DailyChoice(daily.calendarLabel, daily.done, daily.tag) {
            onChosen()
            open()
        }
    }

val CALENDAR_TABS = SettingsTab.entries - SettingsTab.GENERAL

class GameDaily(
    val group: String,
    val label: String,
    val tag: String,
    val done: Boolean,
    val offered: Boolean,
    val home: String,
    val calendarLabel: String = label,
    val open: suspend () -> Long?,
    val show: NavHostController.(Long) -> Unit,
) {
    fun opener(
        navController: NavHostController,
        scope: CoroutineScope,
    ): () -> Unit =
        {
            scope.launch {
                val id = open() ?: return@launch
                navController.navigate(home) {
                    popUpTo(LAUNCHER)
                    launchSingleTop = true
                }
                navController.show(id)
            }
        }
}

class CalendarRules(
    val total: Int,
    val firstMedal: Medal = Medal.BRONZE,
)

fun calendarRules(tab: SettingsTab): CalendarRules =
    when (tab) {
        SettingsTab.GENERAL, SettingsTab.SUDOKU -> CalendarRules(GameMode.entries.sumOf { it.levels.size })
        SettingsTab.SOLITAIRE -> CalendarRules(1)
        SettingsTab.TWO_TONE -> CalendarRules(TWO_TONE_SIZES.size)
        SettingsTab.SIGHTLINES -> CalendarRules(SIGHTLINES_SIZES.size)
        SettingsTab.FIVE_LETTERS -> CalendarRules(2, Medal.SILVER)
        SettingsTab.KAKURO -> CalendarRules(KAKURO_SIZES.size)
        SettingsTab.SLIDE_OUT -> CalendarRules(SlideOutLevel.entries.size)
        SettingsTab.WORD_WHEEL -> CalendarRules(WordWheelLevel.entries.size)
    }

fun dailyResults(
    app: PuzzlesApplication,
    tab: SettingsTab,
): Flow<List<DailyResult>> =
    when (tab) {
        SettingsTab.GENERAL, SettingsTab.SUDOKU -> {
            app.sudoku.games.history.map { games ->
                games.filter { it.status == SudokuStatus.COMPLETED }.mapNotNull { game ->
                    DailyIds.parse(game.ref.id)?.let { (_, date) -> DailyResult(date, game.ref.id, game.finishedAt) }
                }
            }
        }

        SettingsTab.SOLITAIRE -> {
            app.solitaire.games.history.map { games ->
                games.filter { it.status == SolitaireStatus.WON }.mapNotNull { result(it.deal.id, it.finishedAt) }
            }
        }

        SettingsTab.TWO_TONE -> {
            app.twoTone.games.history.map { games ->
                games.filter { it.status == TwoToneStatus.SOLVED }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }

        SettingsTab.SIGHTLINES -> {
            app.sightlines.games.history.map { games ->
                games.filter { it.status == SightlinesStatus.SOLVED }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }

        SettingsTab.FIVE_LETTERS -> {
            app.fiveLetters.games.history.map { games ->
                games.filter { it.status == FiveLettersStatus.WON }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }

        SettingsTab.KAKURO -> {
            app.kakuro.games.history.map { games ->
                games.filter { it.status == KakuroStatus.SOLVED }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }

        SettingsTab.SLIDE_OUT -> {
            app.slideOut.games.history.map { games ->
                games.filter { it.status == SlideOutStatus.SOLVED }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }

        SettingsTab.WORD_WHEEL -> {
            app.wordWheel.games.history.map { games ->
                games.filter { it.status == WordWheelStatus.SOLVED }.mapNotNull { result(it.ref.id, it.finishedAt) }
            }
        }
    }

private val DAILY_ID = Regex("^(?:daily|nyt)_(\\d{4}-\\d{2}-\\d{2})(?:_.*)?$")

private fun result(
    id: String,
    finishedAt: Long?,
): DailyResult? =
    DAILY_ID.find(id)?.let { match ->
        runCatching { DailyResult(LocalDate.parse(match.groupValues[1]), id, finishedAt) }.getOrNull()
    }

suspend fun dailiesOn(
    app: PuzzlesApplication,
    tab: SettingsTab,
    date: LocalDate,
): List<GameDaily> =
    when (tab) {
        SettingsTab.GENERAL, SettingsTab.SUDOKU -> sudoku(app, date)
        SettingsTab.SOLITAIRE -> solitaire(app, date)
        SettingsTab.TWO_TONE -> twoTone(app, date)
        SettingsTab.SIGHTLINES -> sightlines(app, date)
        SettingsTab.FIVE_LETTERS -> fiveLetters(app, date)
        SettingsTab.KAKURO -> kakuro(app, date)
        SettingsTab.SLIDE_OUT -> slideOut(app, date)
        SettingsTab.WORD_WHEEL -> wordWheel(app, date)
    }

private suspend fun sudoku(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val solved =
        app.sudoku.games.history
            .first()
            .filter { it.status == SudokuStatus.COMPLETED }
            .map { it.ref.id }
            .toSet()
    return GameMode.entries.flatMap { mode ->
        val modeName = mode.name.lowercase().replaceFirstChar(Char::uppercase)
        mode.levels.map { level ->
            val done = DailyIds.of(mode, level, date) in solved
            GameDaily(
                "SUDOKU_${mode.name}",
                level.sudokuLabel(),
                "daily_play_${mode.name.lowercase()}_${level.name.lowercase()}",
                done,
                !done,
                SudokuRoutes.HOME,
                "$modeName ${level.sudokuLabel()}",
                {
                    app.sudoku.games.openDaily(
                        app.sudoku.bank,
                        mode,
                        level,
                        date,
                        app.sudoku.settings.settings
                            .first()
                            .mistakeCheckFor(mode),
                    )
                },
            ) { openSudokuGame(it) }
        }
    }
}

private suspend fun solitaire(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val daily = DealRef.dailyId(date)
    val won =
        app.solitaire.games.history
            .first()
            .any { it.deal.id == daily && it.status == SolitaireStatus.WON }
    return listOf(
        GameDaily("SOLITAIRE", "Deal", "daily_play_solitaire", won, !won, SolitaireRoutes.HOME, "Daily deal", {
            app.solitaire.games.inProgress
                .first()
                .find { it.deal.id == daily }
                ?.id ?: run {
                val prefs =
                    app.solitaire.settings.settings
                        .first()
                app.solitaire.games
                    .start(DealRef.daily(date, prefs.drawMode, prefs.order, app.solitaire.winnableDeals.takeIf { prefs.winnableOnly }))
                    .id
            }
        }) { openSolitaireGame(it) },
    )
}

private suspend fun fiveLetters(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.fiveLetters.games.history
            .first()
    val daily = WordRef.dailyId(date)
    val nyt = WordRef.nytId(date)
    val finished = history.any { it.ref.id == daily && (it.status == FiveLettersStatus.WON || it.status == FiveLettersStatus.LOST) }
    return listOf(
        GameDaily(
            "FIVE_LETTERS",
            "Word",
            "daily_play_word",
            history.any { it.ref.id == daily && it.status == FiveLettersStatus.WON },
            !finished,
            FiveLettersRoutes.HOME,
            "Puzzles daily",
            {
                app.fiveLetters.games.history
                    .first()
                    .find { it.ref.id == daily && it.status != FiveLettersStatus.ABANDONED }
                    ?.id ?: app.fiveLetters.games
                    .start(
                        WordRef.daily(app.fiveLetters.words(), date),
                        app.fiveLetters.settings.settings
                            .first()
                            .hardMode,
                    ).id
            },
        ) { openFiveLettersGame(it) },
        GameDaily(
            "FIVE_LETTERS",
            "NY Times",
            "daily_play_nyt",
            history.any { it.ref.id == nyt && it.status == FiveLettersStatus.WON },
            false,
            FiveLettersRoutes.HOME,
            "NY Times daily",
            {
                app.fiveLetters.games.history
                    .first()
                    .find { it.ref.id == nyt && it.status != FiveLettersStatus.ABANDONED }
                    ?.id ?: app.fiveLetters.nytWords.word(date)?.let { answer ->
                    app.fiveLetters.games
                        .start(
                            WordRef.nyt(date, answer),
                            app.fiveLetters.settings.settings
                                .first()
                                .hardMode,
                        ).id
                }
            },
        ) { openFiveLettersGame(it) },
    )
}

private suspend fun twoTone(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.twoTone.games.history
            .first()
    return TWO_TONE_SIZES.map { item ->
        val id = TwoToneRef.dailyId(date, item)
        val solved = history.any { it.ref.id == id && it.status == TwoToneStatus.SOLVED }
        GameDaily(
            "TWO_TONE",
            twotoneSizeLabel(item),
            "daily_play_two_tone_$item",
            solved,
            !solved,
            TwoToneRoutes.HOME,
            open = {
                app.twoTone.games.inProgress
                    .first()
                    .find { it.ref.id == id }
                    ?.id ?: app.twoTone.games
                    .start { TwoToneRef.daily(date, item) }
                    .id
            },
        ) { openTwoToneGame(it) }
    }
}

private suspend fun sightlines(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.sightlines.games.history
            .first()
    return SIGHTLINES_SIZES.map { item ->
        val id = SightlinesRef.dailyId(date, item)
        val solved = history.any { it.ref.id == id && it.status == SightlinesStatus.SOLVED }
        GameDaily(
            "SIGHTLINES",
            sightlinesSizeLabel(item),
            "daily_play_sightlines_$item",
            solved,
            !solved,
            SightlinesRoutes.HOME,
            open = {
                app.sightlines.games.inProgress
                    .first()
                    .find { it.ref.id == id }
                    ?.id ?: app.sightlines.games
                    .start { SightlinesRef.daily(date, item) }
                    .id
            },
        ) { openSightlinesGame(it) }
    }
}

private suspend fun kakuro(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.kakuro.games.history
            .first()
    return KAKURO_SIZES.map { item ->
        val id = KakuroRef.dailyId(date, item)
        val solved = history.any { it.ref.id == id && it.status == KakuroStatus.SOLVED }
        GameDaily(
            "KAKURO",
            kakuroSizeLabel(item),
            "daily_play_kakuro_$item",
            solved,
            !solved,
            KakuroRoutes.HOME,
            open = {
                app.kakuro.games.inProgress
                    .first()
                    .find { it.ref.id == id }
                    ?.id ?: app.kakuro.games
                    .start { KakuroRef.daily(app.kakuro.bank, date, item) }
                    .id
            },
        ) { openKakuroGame(it) }
    }
}

private suspend fun slideOut(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.slideOut.games.history
            .first()
    return SlideOutLevel.entries.map { item ->
        val id = SlideOutRef.dailyId(date, item)
        val solved = history.any { it.ref.id == id && it.status == SlideOutStatus.SOLVED }
        GameDaily(
            "SLIDE_OUT",
            item.slideOutLabel(),
            "daily_play_slide_out_${item.name.lowercase()}",
            solved,
            !solved,
            SlideOutRoutes.HOME,
            open = {
                app.slideOut.games.inProgress
                    .first()
                    .find { it.ref.id == id }
                    ?.id ?: app.slideOut.games
                    .start { SlideOutRef.daily(app.slideOut.bank, date, item) }
                    .id
            },
        ) { openSlideOutGame(it) }
    }
}

private suspend fun wordWheel(
    app: PuzzlesApplication,
    date: LocalDate,
): List<GameDaily> {
    val history =
        app.wordWheel.games.history
            .first()
    return WordWheelLevel.entries.map { item ->
        val id = WordWheelRef.dailyId(date, item)
        val solved = history.any { it.ref.id == id && it.status == WordWheelStatus.SOLVED }
        GameDaily(
            "WORD_WHEEL",
            item.wordWheelLabel(),
            "daily_play_word_wheel_${item.name.lowercase()}",
            solved,
            !solved,
            WordWheelRoutes.HOME,
            open = {
                app.wordWheel.games.inProgress
                    .first()
                    .find { it.ref.id == id }
                    ?.id ?: app.wordWheel.games
                    .start { WordWheelRef.daily(app.wordWheel.bank, date, item) }
                    .id
            },
        ) { openWordWheelGame(it) }
    }
}
