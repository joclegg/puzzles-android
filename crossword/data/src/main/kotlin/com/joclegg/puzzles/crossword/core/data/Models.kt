package com.joclegg.puzzles.crossword.core.data

import com.joclegg.puzzles.crossword.engine.Change
import com.joclegg.puzzles.crossword.engine.Puzzle
import java.time.LocalDate
import kotlin.random.Random

enum class GameStatus { IN_PROGRESS, SOLVED, ABANDONED, REVEALED }

data class PuzzleRef(
    val id: String,
    val puzzle: Puzzle,
) {
    val size: Int get() = puzzle.size

    val isDaily: Boolean get() = id.startsWith(DAILY_PREFIX)

    val slot: String
        get() =
            if (isDaily) {
                id
            } else {
                "size_$size"
            }

    companion object {
        const val DAILY_PREFIX = "daily_"
        const val DAILY_POOL = 3000
        private val FIRST_DAY: LocalDate = LocalDate.of(2026, 11, 16)

        fun random(
            bank: PuzzleBank,
            size: Int,
            seed: Long,
        ): PuzzleRef {
            val puzzles = bank.puzzles(size)
            val index = Random(seed).nextInt(puzzles.size)
            return PuzzleRef("bank_${size}_$index", puzzles[index])
        }

        fun dailyId(
            date: LocalDate,
            size: Int,
        ) = "$DAILY_PREFIX${date}_$size"

        fun daily(
            bank: PuzzleBank,
            date: LocalDate,
            size: Int,
        ): PuzzleRef {
            val puzzles = bank.puzzles(size)
            val count = minOf(puzzles.size, DAILY_POOL)
            val order = (0 until count).shuffled(Random(size.toLong()))
            val day = (date.toEpochDay() - FIRST_DAY.toEpochDay()).toInt()
            return PuzzleRef(dailyId(date, size), puzzles[order[day.mod(count)]])
        }
    }
}

data class SavedGame(
    val id: Long,
    val ref: PuzzleRef,
    val entries: String,
    val changes: List<Change>,
    val elapsedMs: Long,
    val hints: Int,
    val mistakes: Int = 0,
    val status: GameStatus,
    val startedAt: Long,
    val finishedAt: Long?,
)

data class SizeStats(
    val size: Int,
    val played: Int,
    val solved: Int,
    val bestMs: Long?,
    val averageMs: Long?,
)
