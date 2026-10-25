package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.Generator
import com.joclegg.puzzles.sudoku.engine.Puzzle
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class PuzzleSupply(
    private val bank: PuzzleBank,
    private val games: GameRepository,
    private val generated: GeneratedPuzzleDao,
    private val onRunningLow: () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
    private val generate: (GameMode, Difficulty) -> Puzzle = ::generatePuzzle,
) {
    suspend fun next(
        mode: GameMode,
        difficulty: Difficulty,
    ): PuzzleRef {
        val ref = take(mode, difficulty)
        if (stock(mode, difficulty) < LOW_WATERMARK) {
            onRunningLow()
        }
        return ref
    }

    private suspend fun take(
        mode: GameMode,
        difficulty: Difficulty,
    ): PuzzleRef {
        val played = games.playedPuzzleIds(mode)
        withContext(Dispatchers.IO) { bank.nextUnplayed(mode, difficulty, played) }?.let { return it }
        val stocked = generated.takeUnused(mode.name, difficulty.name)
        if (stocked != null) {
            return stocked.toRef()
        }
        val fresh = withContext(Dispatchers.Default) { generate(mode, difficulty) }
        val id = generated.insert(entity(mode, difficulty, fresh, used = true))
        return PuzzleRef(generatedId(mode, difficulty, id), mode, difficulty, fresh)
    }

    suspend fun stock(
        mode: GameMode,
        difficulty: Difficulty,
    ): Int {
        val played = games.playedPuzzleIds(mode)
        val bundled = withContext(Dispatchers.IO) { bank.unplayedCount(mode, difficulty, played) }
        return bundled + generated.unusedCount(mode.name, difficulty.name)
    }

    suspend fun topUp(
        target: Int = STOCK_TARGET,
        shouldStop: () -> Boolean = { false },
    ) {
        for (mode in GameMode.entries) {
            for (difficulty in mode.levels) {
                var missing = target - stock(mode, difficulty)
                while (missing > 0 && !shouldStop()) {
                    val fresh = withContext(Dispatchers.Default) { generate(mode, difficulty) }
                    generated.insert(entity(mode, difficulty, fresh, used = false))
                    missing--
                }
            }
        }
    }

    private fun entity(
        mode: GameMode,
        difficulty: Difficulty,
        puzzle: Puzzle,
        used: Boolean,
    ) = GeneratedPuzzleEntity(
        mode = mode.name,
        difficulty = difficulty.name,
        puzzle = PuzzleCodec.encode(puzzle),
        createdAt = now(),
        used = used,
    )

    private fun GeneratedPuzzleEntity.toRef(): PuzzleRef {
        val gameMode = GameMode.valueOf(mode)
        val level = Difficulty.valueOf(difficulty)
        return PuzzleRef(generatedId(gameMode, level, id), gameMode, level, PuzzleCodec.decode(puzzle))
    }

    private fun generatedId(
        mode: GameMode,
        difficulty: Difficulty,
        id: Long,
    ) = "generated_${mode.name.lowercase()}_${difficulty.name.lowercase()}:$id"

    companion object {
        const val STOCK_TARGET = 30
        const val LOW_WATERMARK = 10
    }
}

fun generatePuzzle(
    mode: GameMode,
    difficulty: Difficulty,
    random: Random = Random.Default,
): Puzzle {
    while (true) {
        val made =
            when (mode) {
                GameMode.CLASSIC -> Generator.classic(difficulty, random)
                GameMode.KILLER -> Generator.killer(difficulty, random)
            }
        if (made != null) {
            return made.puzzle
        }
    }
}
