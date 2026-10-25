package com.joclegg.puzzles.sightlines.core.data

import com.joclegg.puzzles.sightlines.engine.Change
import com.joclegg.puzzles.sightlines.engine.Generator
import com.joclegg.puzzles.sightlines.engine.Puzzle
import java.time.LocalDate

enum class GameStatus { IN_PROGRESS, SOLVED, ABANDONED }

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
        const val DAILY_SIZE = 7
        private const val DAILY_SEED_SPREAD = 7_919L

        fun random(
            seed: Long,
            size: Int,
        ) = PuzzleRef("random_$seed", Generator.generate(size, seed))

        fun daily(date: LocalDate) = PuzzleRef("$DAILY_PREFIX$date", Generator.generate(DAILY_SIZE, date.toEpochDay() * DAILY_SEED_SPREAD))
    }
}

data class SavedGame(
    val id: Long,
    val ref: PuzzleRef,
    val changes: List<Change>,
    val elapsedMs: Long,
    val hints: Int,
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
