package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.CLASSIC_LEVELS
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.KILLER_LEVELS
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Move
import com.joclegg.puzzles.sudoku.engine.Puzzle

enum class GameMode { CLASSIC, KILLER }

val GameMode.levels: List<Difficulty>
    get() =
        if (this == GameMode.KILLER) {
            KILLER_LEVELS
        } else {
            CLASSIC_LEVELS
        }

enum class GameStatus { IN_PROGRESS, COMPLETED, ABANDONED, REVEALED }

data class PuzzleRef(
    val id: String,
    val mode: GameMode,
    val difficulty: Difficulty?,
    val puzzle: Puzzle,
    val custom: Boolean = false,
) {
    val slot: String
        get() =
            when {
                custom -> "custom"
                id.startsWith(DAILY_PREFIX) -> id
                else -> levelSlot(mode, difficulty)
            }

    companion object {
        const val DAILY_PREFIX = "daily_"

        fun levelSlot(
            mode: GameMode,
            difficulty: Difficulty?,
        ) = "${mode.name}_${difficulty?.name}"
    }
}

data class SavedGame(
    val id: Long,
    val ref: PuzzleRef,
    val values: List<Int>,
    val notes: List<Int>,
    val elapsedMs: Long,
    val mistakes: Int,
    val mistakeCheck: MistakeCheck,
    val status: GameStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val moves: List<Move> = emptyList(),
)

data class BestTime(
    val mode: GameMode,
    val difficulty: Difficulty,
    val bestMs: Long,
    val averageMs: Long,
    val completed: Int,
)
