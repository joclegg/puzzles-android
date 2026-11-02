package com.joclegg.puzzles.fiveletters.core.data

import com.joclegg.puzzles.fiveletters.engine.Words
import java.time.LocalDate

enum class GameStatus { IN_PROGRESS, WON, LOST, ABANDONED }

data class WordRef(
    val id: String,
    val answer: String,
) {
    val isDaily: Boolean get() = id.startsWith(DAILY_PREFIX)

    val daily2Date: LocalDate? get() = daily2DateOf(id)

    val isDaily2: Boolean get() = daily2Date != null

    val slot: String
        get() =
            if (isDaily || isDaily2) {
                id
            } else {
                PRACTICE_SLOT
            }

    companion object {
        const val DAILY_PREFIX = "daily_"
        const val DAILY2_PREFIX = "daily2_"
        const val LEGACY_DAILY2_PREFIX = "nyt_"
        private const val PRACTICE_SLOT = "practice"

        fun dailyId(date: LocalDate) = "$DAILY_PREFIX$date"

        fun daily2DateOf(id: String): LocalDate? =
            listOf(DAILY2_PREFIX, LEGACY_DAILY2_PREFIX)
                .firstOrNull { id.startsWith(it) }
                ?.let { runCatching { LocalDate.parse(id.removePrefix(it)) }.getOrNull() }

        fun daily(
            words: Words,
            date: LocalDate,
        ) = WordRef(dailyId(date), words.daily(date))

        fun daily2Id(date: LocalDate) = "$DAILY2_PREFIX$date"

        fun daily2(
            date: LocalDate,
            answer: String,
        ) = WordRef(daily2Id(date), answer)

        fun practice(
            words: Words,
            seed: Long,
        ) = WordRef("practice_$seed", words.random(seed))
    }
}

data class SavedGame(
    val id: Long,
    val ref: WordRef,
    val guesses: List<String>,
    val hardMode: Boolean,
    val status: GameStatus,
    val startedAt: Long,
    val finishedAt: Long?,
)

data class WordStats(
    val played: Int = 0,
    val won: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val guessCounts: List<Int> = emptyList(),
)
