package com.joclegg.puzzles.wordwheel.core.data

import com.joclegg.puzzles.wordwheel.engine.Level
import com.joclegg.puzzles.wordwheel.engine.Puzzle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(
    private val dao: GameDao,
    private val background: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val inProgress: Flow<List<SavedGame>> = dao.observeInProgress().map { rows -> rows.map { it.toModel() } }

    val history: Flow<List<SavedGame>> = dao.observeHistory().map { rows -> rows.map { it.toModel() } }

    val stats: Flow<List<LevelStats>> = history.map { games -> Level.entries.map { level -> games.statsFor(level) } }

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(makeRef: () -> PuzzleRef): SavedGame {
        val ref = withContext(background) { makeRef() }
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.ref.slot == ref.slot }
            .forEach { dao.update(it.copy(status = GameStatus.ABANDONED).toEntity()) }
        val game = SavedGame(0, ref, emptyList(), emptyList(), emptySet(), 0, GameStatus.IN_PROGRESS, now(), null)
        return game.copy(id = dao.insert(game.toEntity()))
    }

    suspend fun save(game: SavedGame) = dao.update(withContext(background) { game.toEntity() })

    suspend fun reveal(game: SavedGame) {
        dao.update(withContext(background) { game.copy(status = GameStatus.REVEALED, finishedAt = now()).toEntity() })
    }

    suspend fun solve(game: SavedGame): Boolean {
        dao.update(withContext(background) { game.copy(status = GameStatus.SOLVED, finishedAt = now()).toEntity() })
        val previous = dao.bestTimeExcluding(game.ref.level.name, game.id)
        return previous == null || game.elapsedMs < previous
    }
}

private fun List<SavedGame>.statsFor(level: Level): LevelStats {
    val finished = filter { it.ref.level == level && it.status != GameStatus.IN_PROGRESS }
    val solved = finished.filter { it.status == GameStatus.SOLVED }
    return LevelStats(level, finished.size, solved.size, finished.sumOf { it.bonus.size }, solved.minOfOrNull { it.elapsedMs })
}

internal fun SavedGame.toEntity() =
    GameEntity(
        id = id,
        puzzleId = ref.id,
        level = ref.level.name,
        puzzle = ref.puzzle.encode(),
        found = found.joinToString(","),
        bonus = bonus.joinToString(","),
        revealed = revealed.joinToString(",") { (row, col) -> "$row:$col" },
        elapsedMs = elapsedMs,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        ref = PuzzleRef(puzzleId, Level.valueOf(level), Puzzle.decode(puzzle)),
        found = found.split(',').filter { it.isNotEmpty() },
        bonus = bonus.split(',').filter { it.isNotEmpty() },
        revealed =
            revealed
                .split(',')
                .filter { it.isNotEmpty() }
                .map { cell -> cell.split(':').let { (row, col) -> row.toInt() to col.toInt() } }
                .toSet(),
        elapsedMs = elapsedMs,
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
