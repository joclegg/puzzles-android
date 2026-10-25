package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

suspend fun GameRepository.openDaily(
    bank: PuzzleBank,
    mode: GameMode,
    date: LocalDate,
    mistakeCheck: MistakeCheck,
): Long {
    val ref = withContext(Dispatchers.IO) { bank.daily(mode, date) }
    return inProgress.first().find { it.ref.id == ref.id }?.id ?: start(ref, mistakeCheck).id
}
