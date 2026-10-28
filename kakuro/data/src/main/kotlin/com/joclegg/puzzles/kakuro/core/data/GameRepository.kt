package com.joclegg.puzzles.kakuro.core.data

import com.joclegg.puzzles.kakuro.engine.ChangeCodec
import com.joclegg.puzzles.kakuro.engine.Puzzle
import com.joclegg.puzzles.kakuro.engine.SIZES
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class GameRepository(
    private val dao: GameDao,
    private val background: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val inProgress: Flow<List<SavedGame>> = dao.observeInProgress().map { rows -> rows.map { it.toModel() } }

    val history: Flow<List<SavedGame>> = dao.observeHistory().map { rows -> rows.map { it.toModel() } }

    val stats: Flow<List<SizeStats>> = dao.observeHistory().map { rows -> SIZES.map { size -> rows.statsFor(size) } }

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(makeRef: () -> PuzzleRef): SavedGame {
        val ref = withContext(background) { makeRef() }
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.ref.slot == ref.slot }
            .forEach { dao.save(it.copy(status = GameStatus.ABANDONED).toEntity(now())) }
        val game =
            SavedGame(
                id = 0,
                ref = ref,
                changes = emptyList(),
                elapsedMs = 0,
                hints = 0,
                status = GameStatus.IN_PROGRESS,
                startedAt = now(),
                finishedAt = null,
            )
        return game.copy(id = dao.insert(game.toEntity(now())))
    }

    suspend fun save(game: SavedGame) = dao.save(withContext(background) { game.toEntity(now()) })

    suspend fun reveal(game: SavedGame) {
        dao.save(withContext(background) { game.copy(status = GameStatus.REVEALED, finishedAt = now()).toEntity(now()) })
    }

    suspend fun solve(game: SavedGame): Boolean {
        dao.save(withContext(background) { game.copy(status = GameStatus.SOLVED, finishedAt = now()).toEntity(now()) })
        val previous = dao.bestTimeExcluding(game.ref.size, game.id)
        return previous == null || game.elapsedMs < previous
    }
}

private fun List<GameEntity>.statsFor(size: Int): SizeStats {
    val finished = filter { it.size == size && it.status != GameStatus.IN_PROGRESS.name }
    val solved = finished.filter { it.status == GameStatus.SOLVED.name }
    return SizeStats(
        size = size,
        played = finished.size,
        solved = solved.size,
        bestMs = solved.minOfOrNull { it.elapsedMs },
        averageMs =
            solved
                .takeIf { it.isNotEmpty() }
                ?.map { it.elapsedMs }
                ?.average()
                ?.toLong(),
    )
}

internal fun SavedGame.toEntity(updatedAt: Long) =
    GameEntity(
        id = id,
        uuid = UUID.randomUUID().toString(),
        updatedAt = updatedAt,
        puzzleId = ref.id,
        size = ref.size,
        puzzle = ref.puzzle.encode(),
        changes = ChangeCodec.encode(changes),
        elapsedMs = elapsedMs,
        hints = hints,
        mistakes = mistakes,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        ref = PuzzleRef(puzzleId, Puzzle.decode(puzzle)),
        changes = ChangeCodec.decode(changes),
        elapsedMs = elapsedMs,
        hints = hints,
        mistakes = mistakes,
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
