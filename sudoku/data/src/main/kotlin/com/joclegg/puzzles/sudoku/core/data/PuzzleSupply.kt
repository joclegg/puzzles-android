package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.Generator
import com.joclegg.puzzles.sudoku.engine.Puzzle
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import kotlinx.coroutines.CoroutineDispatcher
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
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val background: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun next(
        mode: GameMode,
        difficulty: Difficulty,
    ): PuzzleRef {
        val played = games.playedPuzzleIds(mode)
        val ref = take(mode, difficulty, played)
        if (stock(mode, difficulty, played) < LOW_WATERMARK) {
            onRunningLow()
        }
        return ref
    }

    private suspend fun take(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
    ): PuzzleRef {
        withContext(io) { bank.nextUnplayed(mode, difficulty, played) }?.let { return it }
        val stocked = generated.takeUnused(mode.name, difficulty.name)
        if (stocked != null) {
            return stocked.toRef()
        }
        val fresh = withContext(background) { generate(mode, difficulty) }
        val id = generated.insert(entity(mode, difficulty, fresh, used = true))
        return PuzzleRef(generatedId(mode, difficulty, id), mode, difficulty, fresh)
    }

    suspend fun stock(
        mode: GameMode,
        difficulty: Difficulty,
    ): Int = stock(mode, difficulty, games.playedPuzzleIds(mode))

    private suspend fun stock(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
    ): Int {
        val bundled = withContext(io) { bank.unplayedCount(mode, difficulty, played) }
        return bundled + generated.unusedCount(mode.name, difficulty.name)
    }

    suspend fun topUp(
        target: Int = STOCK_TARGET,
        shouldStop: () -> Boolean = { false },
    ) {
        for (mode in GameMode.entries) {
            val played = games.playedPuzzleIds(mode)
            for (difficulty in mode.levels) {
                var missing = target - stock(mode, difficulty, played)
                while (missing > 0 && !shouldStop()) {
                    val fresh = withContext(background) { generate(mode, difficulty) }
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
