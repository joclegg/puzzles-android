package com.joclegg.puzzles.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Medal { NONE, BRONZE, SILVER, GOLD }

data class DailyResult(
    val date: LocalDate,
    val id: String,
    val finishedAt: Long?,
)

data class DailyDay(
    val date: LocalDate,
    val medal: Medal,
    val doneLate: Boolean,
)

fun medalFor(
    count: Int,
    total: Int,
    firstMedal: Medal = Medal.BRONZE,
): Medal =
    when {
        count == 0 -> Medal.NONE
        count >= total -> Medal.GOLD
        count == 1 -> firstMedal
        else -> Medal.SILVER
    }

fun dailyDays(
    results: List<DailyResult>,
    total: Int,
    firstMedal: Medal,
    zone: ZoneId,
): Map<LocalDate, DailyDay> =
    results.groupBy { it.date }.mapValues { (date, onDate) ->
        val onTheDay =
            onDate
                .filter { result ->
                    result.finishedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } == date
                }.distinctBy { it.id }
        val medal = medalFor(onTheDay.size, total, firstMedal)
        DailyDay(date, medal, doneLate = medal == Medal.NONE)
    }
