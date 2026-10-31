package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import java.time.LocalDate
import kotlin.random.Random

class PuzzleBank(
    private val open: (String) -> String,
) {
    private val cache = mutableMapOf<String, List<String>>()

    private fun lines(
        mode: GameMode,
        difficulty: Difficulty,
    ): List<String> =
        synchronized(cache) {
            val name = fileName(mode, difficulty)
            cache.getOrPut(name) { open("sudoku/$name.txt").lines().filter { it.isNotBlank() } }
        }

    fun size(
        mode: GameMode,
        difficulty: Difficulty,
    ) = lines(mode, difficulty).size

    fun get(
        mode: GameMode,
        difficulty: Difficulty,
        index: Int,
    ): PuzzleRef {
        val lines = lines(mode, difficulty)
        val i = index.mod(lines.size)
        return PuzzleRef("${fileName(mode, difficulty)}:$i", mode, difficulty, PuzzleCodec.decode(lines[i]))
    }

    fun nextUnplayed(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
        random: Random = Random.Default,
    ): PuzzleRef? = unplayedIndices(mode, difficulty, played).randomOrNull(random)?.let { get(mode, difficulty, it) }

    fun unplayedCount(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
    ): Int = unplayedIndices(mode, difficulty, played).size

    private fun unplayedIndices(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
    ) = (0 until size(mode, difficulty)).filter { "${fileName(mode, difficulty)}:$it" !in played }

    fun next(
        mode: GameMode,
        difficulty: Difficulty,
        played: Set<String>,
        random: Random = Random.Default,
    ): PuzzleRef {
        val count = size(mode, difficulty)
        val unplayed = (0 until count).filter { "${fileName(mode, difficulty)}:$it" !in played }
        return get(mode, difficulty, unplayed.randomOrNull(random) ?: random.nextInt(count))
    }

    fun daily(
        mode: GameMode,
        difficulty: Difficulty,
        date: LocalDate,
    ): PuzzleRef {
        val ref = get(mode, difficulty, dailyIndex(mode, difficulty, date, size(mode, difficulty)))
        return ref.copy(id = DailyIds.of(mode, difficulty, date))
    }

    private fun fileName(
        mode: GameMode,
        difficulty: Difficulty,
    ) = "${mode.name.lowercase()}_${difficulty.name.lowercase()}"
}

private const val DAILY_SEED_STRIDE = 31L
private const val DAILY_MODE_STRIDE = 8L

internal fun dailyIndex(
    mode: GameMode,
    difficulty: Difficulty,
    date: LocalDate,
    size: Int,
): Int {
    val seed = date.toEpochDay() * DAILY_SEED_STRIDE + mode.ordinal * DAILY_MODE_STRIDE + mode.levels.indexOf(difficulty)
    return Random(seed).nextInt(size)
}
