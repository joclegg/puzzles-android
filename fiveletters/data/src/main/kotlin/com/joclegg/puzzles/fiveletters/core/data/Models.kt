package com.joclegg.puzzles.fiveletters.core.data

import com.joclegg.puzzles.fiveletters.engine.Words
import java.time.LocalDate

enum class GameStatus { IN_PROGRESS, WON, LOST, ABANDONED }

data class WordRef(
    val id: String,
    val answer: String,
) {
    val isDaily: Boolean get() = id.startsWith(DAILY_PREFIX)

    val slot: String
        get() =
            if (isDaily) {
                id
            } else {
                PRACTICE_SLOT
            }

    companion object {
        const val DAILY_PREFIX = "daily_"
        private const val PRACTICE_SLOT = "practice"

        fun dailyId(date: LocalDate) = "$DAILY_PREFIX$date"

        fun daily(
            words: Words,
            date: LocalDate,
            nytWord: String? = null,
        ) = WordRef(dailyId(date), nytWord ?: words.daily(date))

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
