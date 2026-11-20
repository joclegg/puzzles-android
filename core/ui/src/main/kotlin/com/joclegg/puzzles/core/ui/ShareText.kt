package com.joclegg.puzzles.core.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val SHARE_LINK = "joclegg.github.io/puzzles-web"

private const val CHART_WIDTH = 8
private const val LAST_DAYS = 7L
private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DAILY_DATE = Regex("^daily2?_.*?(\\d{4}-\\d{2}-\\d{2})")

data class ShareDaily(
    val label: String,
    val elapsedMs: Long?,
    val best: Boolean = false,
    val moves: Int? = null,
)

data class ShareRecord(
    val label: String,
    val bestMs: Long?,
    val fewestMoves: Int? = null,
)

fun dailyDateOf(id: String): LocalDate? =
    DAILY_DATE.find(id)?.let { match -> runCatching { LocalDate.parse(match.groupValues[1]) }.getOrNull() }

fun dailiesShareText(
    game: String,
    date: LocalDate,
    medal: Medal,
    body: List<String>,
    lastSeven: List<Medal>,
): String {
    val heading = listOfNotNull(game, "·", date.format(DAY_FORMAT), medal.emoji).joinToString(" ")
    val squares = lastSeven.joinToString("") { it.square }
    return (listOf(heading) + body + listOf("", "Last 7 days $squares", SHARE_LINK)).joinToString("\n")
}

fun dailyLine(daily: ShareDaily): String {
    val time = daily.elapsedMs ?: return "${daily.label} ⬛"
    val moves = daily.moves?.let { " · $it moves" }.orEmpty()
    val star =
        if (daily.best) {
            " ⭐"
        } else {
            ""
        }
    return "${daily.label} ${formatDuration(time)}$moves$star"
}

fun solitaireDailyLine(wonMs: Long?): String = wonMs?.let { "Won in ${formatDuration(it)}" } ?: "Not won"

fun lastSevenDays(
    date: LocalDate,
    medalOn: (LocalDate) -> Medal,
): List<Medal> = (LAST_DAYS - 1 downTo 0).map { medalOn(date.minusDays(it)) }

fun recordsShareText(
    game: String,
    lines: List<String>,
    summary: String,
): String = (listOf("$game records 🏆") + lines + listOf("", summary, SHARE_LINK)).joinToString("\n")

fun recordLines(records: List<ShareRecord>): List<String> =
    records.mapNotNull { record ->
        record.bestMs?.let { best ->
            val moves = record.fewestMoves?.let { " · $it moves" }.orEmpty()
            "${record.label} ${formatDuration(best)}$moves"
        }
    }

fun solvedSummary(
    solved: Int,
    streak: Int,
    longest: Int,
): String = "$solved solved · 🔥 $streak · longest 🔥 $longest"

fun solitaireRecordLines(
    fastestMs: Long?,
    fewestMoves: Int?,
): List<String> =
    listOfNotNull(
        fastestMs?.let { "Fastest win ${formatDuration(it)}" },
        fewestMoves?.let { "Fewest moves $it" },
    )

fun solitaireSummary(
    wins: Int,
    played: Int,
    streak: Int,
    longest: Int,
): String {
    val percent =
        if (played == 0) {
            0
        } else {
            (wins * 100.0 / played).roundToInt()
        }
    return "$wins wins · $percent% · 🔥 $streak · longest 🔥 $longest"
}

fun guessChart(counts: List<Int>): List<String> {
    val largest = counts.maxOrNull() ?: 0
    return counts.mapIndexed { index, count ->
        val width =
            if (count == 0 || largest == 0) {
                0
            } else {
                maxOf(1, (count * CHART_WIDTH.toDouble() / largest).roundToInt())
            }
        "${index + 1} ${"🟩".repeat(width)} $count"
    }
}

fun wordSummary(
    won: Int,
    played: Int,
    streak: Int,
    longest: Int,
): String = "$won/$played won · 🔥 $streak · longest 🔥 $longest"

fun currentStreak(
    days: Set<LocalDate>,
    today: LocalDate,
): Int {
    var day =
        if (today in days) {
            today
        } else {
            today.minusDays(1)
        }
    var streak = 0
    while (day in days) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

fun longestStreak(days: Set<LocalDate>): Int =
    days.maxOfOrNull { start ->
        if (start.minusDays(1) in days) {
            0
        } else {
            generateSequence(start) { it.plusDays(1) }.takeWhile { it in days }.count()
        }
    } ?: 0

private val Medal.emoji: String?
    get() =
        when (this) {
            Medal.GOLD -> "🥇"
            Medal.SILVER -> "🥈"
            Medal.BRONZE -> "🥉"
            Medal.NONE -> null
        }

private val Medal.square: String
    get() =
        when (this) {
            Medal.GOLD -> "🟨"
            Medal.SILVER -> "⬜"
            Medal.BRONZE -> "🟧"
            Medal.NONE -> "⬛"
        }
