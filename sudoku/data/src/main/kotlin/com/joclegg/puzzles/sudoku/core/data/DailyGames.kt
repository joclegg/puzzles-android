package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

suspend fun GameRepository.openDaily(
    bank: PuzzleBank,
    mode: GameMode,
    difficulty: Difficulty,
    date: LocalDate,
    mistakeCheck: MistakeCheck,
): Long {
    val ref = withContext(Dispatchers.IO) { bank.daily(mode, difficulty, date) }
    return inProgress.first().find { it.ref.id == ref.id }?.id ?: start(ref, mistakeCheck).id
}

suspend fun GameRepository.openLevel(
    supply: PuzzleSupply,
    mode: GameMode,
    difficulty: Difficulty,
    mistakeCheck: MistakeCheck,
): Long =
    inProgress.first().find { it.ref.slot == PuzzleRef.levelSlot(mode, difficulty) }?.id
        ?: start(supply.next(mode, difficulty), mistakeCheck).id
