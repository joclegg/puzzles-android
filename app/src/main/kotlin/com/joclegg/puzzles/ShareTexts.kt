package com.joclegg.puzzles

import com.joclegg.puzzles.core.ui.Medal
import com.joclegg.puzzles.core.ui.ShareDaily
import com.joclegg.puzzles.core.ui.ShareRecord
import com.joclegg.puzzles.core.ui.currentStreak
import com.joclegg.puzzles.core.ui.dailiesShareText
import com.joclegg.puzzles.core.ui.dailyDays
import com.joclegg.puzzles.core.ui.dailyLine
import com.joclegg.puzzles.core.ui.guessChart
import com.joclegg.puzzles.core.ui.lastSevenDays
import com.joclegg.puzzles.core.ui.longestStreak
import com.joclegg.puzzles.core.ui.recordLines
import com.joclegg.puzzles.core.ui.recordsShareText
import com.joclegg.puzzles.core.ui.solitaireDailyLine
import com.joclegg.puzzles.core.ui.solitaireRecordLines
import com.joclegg.puzzles.core.ui.solitaireSummary
import com.joclegg.puzzles.core.ui.solvedSummary
import com.joclegg.puzzles.core.ui.wordSummary
import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.fiveletters.engine.GameState
import com.joclegg.puzzles.fiveletters.ui.title
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.sudoku.core.data.DailyIds
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.levels
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.joclegg.puzzles.crossword.core.data.GameStatus as CrosswordStatus
import com.joclegg.puzzles.crossword.core.data.PuzzleRef as CrosswordRef
import com.joclegg.puzzles.crossword.engine.SIZES as CROSSWORD_SIZES
import com.joclegg.puzzles.crossword.ui.sizeLabel as crosswordSizeLabel
import com.joclegg.puzzles.fiveletters.core.data.GameStatus as FiveLettersStatus
import com.joclegg.puzzles.kakuro.core.data.GameStatus as KakuroStatus
import com.joclegg.puzzles.kakuro.core.data.PuzzleRef as KakuroRef
import com.joclegg.puzzles.kakuro.engine.SIZES as KAKURO_SIZES
import com.joclegg.puzzles.kakuro.ui.sizeLabel as kakuroSizeLabel
import com.joclegg.puzzles.sightlines.core.data.GameStatus as SightlinesStatus
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef as SightlinesRef
import com.joclegg.puzzles.sightlines.engine.SIZES as SIGHTLINES_SIZES
import com.joclegg.puzzles.sightlines.ui.sizeLabel as sightlinesSizeLabel
import com.joclegg.puzzles.slideout.core.data.GameStatus as SlideOutStatus
import com.joclegg.puzzles.slideout.core.data.PuzzleRef as SlideOutRef
import com.joclegg.puzzles.slideout.engine.Level as SlideOutLevel
import com.joclegg.puzzles.slideout.ui.label as slideOutLabel
import com.joclegg.puzzles.solitaire.core.data.GameStatus as SolitaireStatus
import com.joclegg.puzzles.sudoku.core.data.GameStatus as SudokuStatus
import com.joclegg.puzzles.sudoku.ui.label as sudokuLabel
import com.joclegg.puzzles.twotone.core.data.GameStatus as TwoToneStatus
import com.joclegg.puzzles.twotone.core.data.PuzzleRef as TwoToneRef
import com.joclegg.puzzles.twotone.engine.SIZES as TWO_TONE_SIZES
import com.joclegg.puzzles.twotone.ui.sizeLabel as twotoneSizeLabel
import com.joclegg.puzzles.wordwheel.core.data.GameStatus as WordWheelStatus
import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef as WordWheelRef
import com.joclegg.puzzles.wordwheel.engine.Level as WordWheelLevel
import com.joclegg.puzzles.wordwheel.ui.label as wordWheelLabel

private class Slot(
    val label: String,
    val size: String,
    val dailyId: (LocalDate) -> String,
)

private class Solved(
    val id: String,
    val size: String,
    val elapsedMs: Long,
    val moves: Int? = null,
)

private class Played(
    val slots: List<Slot>,
    val solved: List<Solved>,
    val withMoves: Boolean = false,
    val pastBests: Map<String, Long> = emptyMap(),
) {
    fun bestFor(size: String): Long? = (solved.filter { it.size == size }.map { it.elapsedMs } + listOfNotNull(pastBests[size])).minOrNull()
}

fun shareTab(group: String): SettingsTab = CALENDAR_TABS.filter { group.startsWith(it.name) }.maxBy { it.name.length }

suspend fun dailiesShare(
    app: PuzzlesApplication,
    tab: SettingsTab,
    date: LocalDate,
): String {
    val rules = calendarRules(tab)
    val days = dailyDays(dailyResults(app, tab).first(), rules.total, rules.firstMedal, ZoneId.systemDefault())
    val medalOn = { day: LocalDate -> days[day]?.medal ?: Medal.NONE }
    return dailiesShareText(tab.label, date, medalOn(date), dailyBody(app, tab, date), lastSevenDays(date, medalOn))
}

suspend fun recordsShare(
    app: PuzzlesApplication,
    tab: SettingsTab,
): String {
    val days =
        dailyResults(app, tab)
            .first()
            .map { it.date }
            .toSet()
    val streak = currentStreak(days, LocalDate.now())
    val longest = longestStreak(days)
    return when (tab) {
        SettingsTab.SOLITAIRE -> {
            val stats =
                app.solitaire.games.stats
                    .first()
            recordsShareText(
                tab.label,
                solitaireRecordLines(stats.mapNotNull { it.bestMs }.minOrNull(), stats.mapNotNull { it.fewestMoves }.minOrNull()),
                solitaireSummary(stats.sumOf { it.won }, stats.sumOf { it.played }, streak, longest),
            )
        }

        SettingsTab.FIVE_LETTERS -> {
            val stats =
                app.fiveLetters.games.stats
                    .first()
            recordsShareText(
                tab.label,
                guessChart(stats.guessCounts),
                wordSummary(stats.won, stats.played, stats.currentStreak, stats.maxStreak),
            )
        }

        else -> {
            val played = played(app, tab)
            val records =
                played.slots.map { slot ->
                    val onSize = played.solved.filter { it.size == slot.size }
                    ShareRecord(
                        slot.label,
                        played.bestFor(slot.size),
                        onSize.mapNotNull { it.moves }.minOrNull().takeIf { played.withMoves },
                    )
                }
            recordsShareText(tab.label, recordLines(records), solvedSummary(played.solved.size, streak, longest))
        }
    }
}

private suspend fun dailyBody(
    app: PuzzlesApplication,
    tab: SettingsTab,
    date: LocalDate,
): List<String> =
    when (tab) {
        SettingsTab.SOLITAIRE -> {
            val won =
                app.solitaire.games.history
                    .first()
                    .find { it.deal.id == DealRef.dailyId(date) && it.status == SolitaireStatus.WON }
            listOf(solitaireDailyLine(won?.elapsedMs))
        }

        SettingsTab.FIVE_LETTERS -> {
            val finished =
                app.fiveLetters.games.history
                    .first()
                    .filter { it.status == FiveLettersStatus.WON || it.status == FiveLettersStatus.LOST }
            val daily = finished.find { it.ref.id == WordRef.dailyId(date) }
            val daily2 =
                finished.find { game ->
                    WordRef.daily2DateOf(game.ref.id) == date &&
                        game.finishedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } == date
                }
            listOfNotNull(
                daily?.let { GameState(it.ref.answer, it.guesses, hardMode = it.hardMode).shareText(it.ref.title()) }
                    ?: dailyLine(ShareDaily("Puzzles daily", null)),
                daily2?.let { "\n" + GameState(it.ref.answer, it.guesses, hardMode = it.hardMode).shareText(it.ref.title()) },
            )
        }

        else -> {
            val played = played(app, tab)
            played.slots.map { slot ->
                val best = played.bestFor(slot.size)
                val game = played.solved.find { it.id == slot.dailyId(date) }
                dailyLine(ShareDaily(slot.label, game?.elapsedMs, game != null && game.elapsedMs == best, game?.moves))
            }
        }
    }

private suspend fun played(
    app: PuzzlesApplication,
    tab: SettingsTab,
): Played =
    when (tab) {
        SettingsTab.SLIDE_OUT -> {
            Played(
                SlideOutLevel.entries.map { level ->
                    Slot(level.slideOutLabel(), level.name) { SlideOutRef.dailyId(it, level) }
                },
                app.slideOut.games.history
                    .first()
                    .filter { it.status == SlideOutStatus.SOLVED }
                    .map { Solved(it.ref.id, it.ref.level.name, it.elapsedMs, it.moves.size) },
                withMoves = true,
            )
        }

        SettingsTab.WORD_WHEEL -> {
            Played(
                WordWheelLevel.entries.map { level ->
                    Slot(level.wordWheelLabel(), level.name) { WordWheelRef.dailyId(it, level) }
                },
                app.wordWheel.games.history
                    .first()
                    .filter { it.status == WordWheelStatus.SOLVED }
                    .map { Solved(it.ref.id, it.ref.level.name, it.elapsedMs) },
            )
        }

        SettingsTab.TWO_TONE -> {
            Played(
                TWO_TONE_SIZES.map { size -> Slot(twotoneSizeLabel(size), "$size") { TwoToneRef.dailyId(it, size) } },
                app.twoTone.games.history
                    .first()
                    .filter { it.status == TwoToneStatus.SOLVED }
                    .map { Solved(it.ref.id, "${it.ref.size}", it.elapsedMs) },
            )
        }

        SettingsTab.SIGHTLINES -> {
            Played(
                SIGHTLINES_SIZES.map { size -> Slot(sightlinesSizeLabel(size), "$size") { SightlinesRef.dailyId(it, size) } },
                app.sightlines.games.history
                    .first()
                    .filter { it.status == SightlinesStatus.SOLVED }
                    .map { Solved(it.ref.id, "${it.ref.size}", it.elapsedMs) },
            )
        }

        SettingsTab.KAKURO -> {
            Played(
                KAKURO_SIZES.map { size -> Slot(kakuroSizeLabel(size), "$size") { KakuroRef.dailyId(it, size) } },
                app.kakuro.games.history
                    .first()
                    .filter { it.status == KakuroStatus.SOLVED }
                    .map { Solved(it.ref.id, "${it.ref.size}", it.elapsedMs) },
            )
        }

        SettingsTab.CROSSWORD -> {
            Played(
                CROSSWORD_SIZES.map { size -> Slot(crosswordSizeLabel(size), "$size") { CrosswordRef.dailyId(it, size) } },
                app.crossword.games.history
                    .first()
                    .filter { it.status == CrosswordStatus.SOLVED }
                    .map { Solved(it.ref.id, "${it.ref.size}", it.elapsedMs) },
            )
        }

        else -> {
            Played(
                GameMode.entries.flatMap { mode ->
                    val modeName = mode.name.lowercase().replaceFirstChar(Char::uppercase)
                    mode.levels.map { level ->
                        Slot("$modeName ${level.sudokuLabel()}", "${mode.name}_${level.name}") { DailyIds.of(mode, level, it) }
                    }
                },
                app.sudoku.games.history
                    .first()
                    .filter { it.status == SudokuStatus.COMPLETED && !it.ref.custom && it.ref.difficulty != null }
                    .map { Solved(it.ref.id, "${it.ref.mode.name}_${it.ref.difficulty?.name}", it.elapsedMs) },
                pastBests =
                    app.sudoku.games.bestTimes
                        .first()
                        .associate { "${it.mode.name}_${it.difficulty.name}" to it.bestMs },
            )
        }
    }
