package com.joclegg.puzzles.sudoku.core.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DailyStatus { NOT_DONE, LATE, ON_THE_DAY }

enum class Medal { NONE, SILVER, GOLD }

data class DailyDay(
    val date: LocalDate,
    val statuses: Map<GameMode, DailyStatus>,
) {
    fun status(mode: GameMode) = statuses[mode] ?: DailyStatus.NOT_DONE

    val medal: Medal
        get() {
            val onTheDay = GameMode.entries.count { status(it) == DailyStatus.ON_THE_DAY }
            return when (onTheDay) {
                GameMode.entries.size -> Medal.GOLD
                0 -> Medal.NONE
                else -> Medal.SILVER
            }
        }

    val doneLate get() = medal == Medal.NONE && GameMode.entries.any { status(it) == DailyStatus.LATE }
}

object DailyIds {
    fun of(
        mode: GameMode,
        date: LocalDate,
    ) = "${PuzzleRef.DAILY_PREFIX}${mode.name.lowercase()}:$date"

    fun parse(id: String): Pair<GameMode, LocalDate>? {
        if (!id.startsWith(PuzzleRef.DAILY_PREFIX)) {
            return null
        }
        val (mode, date) = id.removePrefix(PuzzleRef.DAILY_PREFIX).split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        val gameMode = GameMode.entries.find { it.name.equals(mode, ignoreCase = true) } ?: return null
        return runCatching { gameMode to LocalDate.parse(date) }.getOrNull()
    }
}

fun dailyDays(
    games: List<SavedGame>,
    zone: ZoneId,
): Map<LocalDate, DailyDay> =
    games
        .filter { it.status == GameStatus.COMPLETED }
        .mapNotNull { game ->
            val (mode, date) = DailyIds.parse(game.ref.id) ?: return@mapNotNull null
            val finishedOn = game.finishedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            Triple(
                date,
                mode,
                if (finishedOn == date) {
                    DailyStatus.ON_THE_DAY
                } else {
                    DailyStatus.LATE
                },
            )
        }.groupBy({ it.first }, { it.second to it.third })
        .mapValues { (date, results) ->
            DailyDay(date, results.groupBy({ it.first }, { it.second }).mapValues { (_, statuses) -> statuses.max() })
        }
