package com.joclegg.puzzles.wordwheel.core.data

import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.engine.Puzzle
import java.time.LocalDate
import kotlin.random.Random

enum class GameStatus { IN_PROGRESS, SOLVED, ABANDONED, REVEALED }

data class PuzzleRef(
    val id: String,
    val level: Level,
    val puzzle: Puzzle,
) {
    val isDaily: Boolean get() = id.startsWith(DAILY_PREFIX)

    val slot: String
        get() =
            if (isDaily) {
                id
            } else {
                level.name
            }

    companion object {
        const val DAILY_PREFIX = "daily_"
        private val FIRST_DAY: LocalDate = LocalDate.of(2026, 10, 25)

        fun random(
            bank: PuzzleBank,
            level: Level,
            seed: Long,
        ): PuzzleRef {
            val puzzles = bank.puzzles(level)
            val index = Random(seed).nextInt(puzzles.size)
            return PuzzleRef("bank_${level.name}_$index", level, puzzles[index])
        }

        fun dailyId(
            date: LocalDate,
            level: Level,
        ) = "$DAILY_PREFIX${date}_${level.name}"

        fun daily(
            bank: PuzzleBank,
            date: LocalDate,
            level: Level,
        ): PuzzleRef {
            val puzzles = bank.puzzles(level)
            val order = puzzles.indices.shuffled(Random(level.ordinal.toLong()))
            val day = (date.toEpochDay() - FIRST_DAY.toEpochDay()).toInt()
            return PuzzleRef(dailyId(date, level), level, puzzles[order[day.mod(puzzles.size)]])
        }
    }
}

data class SavedGame(
    val id: Long,
    val ref: PuzzleRef,
    val found: List<String>,
    val bonus: List<String>,
    val revealed: Set<Pair<Int, Int>>,
    val elapsedMs: Long,
    val status: GameStatus,
    val startedAt: Long,
    val finishedAt: Long?,
)

data class LevelStats(
    val level: Level,
    val played: Int,
    val solved: Int,
    val bonusWords: Int,
    val bestMs: Long?,
)
