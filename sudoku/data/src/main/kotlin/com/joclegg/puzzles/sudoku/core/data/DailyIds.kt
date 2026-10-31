package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import java.time.LocalDate

object DailyIds {
    fun of(
        mode: GameMode,
        difficulty: Difficulty,
        date: LocalDate,
    ) = "${PuzzleRef.DAILY_PREFIX}${mode.name.lowercase()}_${difficulty.name.lowercase()}:$date"

    fun parse(id: String): Pair<GameMode, LocalDate>? {
        if (!id.startsWith(PuzzleRef.DAILY_PREFIX)) {
            return null
        }
        val (kind, date) = id.removePrefix(PuzzleRef.DAILY_PREFIX).split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        val gameMode = GameMode.entries.find { it.name.equals(kind.substringBefore('_'), ignoreCase = true) } ?: return null
        return runCatching { gameMode to LocalDate.parse(date) }.getOrNull()
    }
}
