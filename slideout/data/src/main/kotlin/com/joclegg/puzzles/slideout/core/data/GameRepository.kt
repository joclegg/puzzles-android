package com.joclegg.puzzles.slideout.core.data

import com.joclegg.puzzles.slideout.engine.Level
import com.joclegg.puzzles.slideout.engine.MoveCodec
import com.joclegg.puzzles.slideout.engine.Puzzle
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

    val stats: Flow<List<LevelStats>> = dao.observeHistory().map { rows -> Level.entries.map { level -> rows.statsFor(level) } }

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(makeRef: () -> PuzzleRef): SavedGame {
        val ref = withContext(background) { makeRef() }
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.ref.slot == ref.slot }
            .forEach { dao.update(it.copy(status = GameStatus.ABANDONED).toEntity()) }
        val game = SavedGame(0, ref, emptyList(), 0, 0, GameStatus.IN_PROGRESS, now(), null)
        return game.copy(id = dao.insert(game.toEntity()))
    }

    suspend fun save(game: SavedGame) = dao.update(withContext(background) { game.toEntity() })

    suspend fun solve(game: SavedGame): Boolean {
        dao.update(withContext(background) { game.copy(status = GameStatus.SOLVED, finishedAt = now()).toEntity() })
        val previous = dao.bestTimeExcluding(game.ref.level.name, game.id)
        return previous == null || game.elapsedMs < previous
    }
}

private fun List<GameEntity>.statsFor(level: Level): LevelStats {
    val finished = filter { it.level == level.name && it.status != GameStatus.IN_PROGRESS.name }
    val solved = finished.filter { it.status == GameStatus.SOLVED.name }
    return LevelStats(
        level = level,
        played = finished.size,
        solved = solved.size,
        atPar = solved.count { it.moveCount <= Puzzle.decode(it.puzzle).par },
        bestMs = solved.minOfOrNull { it.elapsedMs },
    )
}

internal fun SavedGame.toEntity() =
    GameEntity(
        id = id,
        puzzleId = ref.id,
        level = ref.level.name,
        puzzle = ref.puzzle.encode(),
        moves = MoveCodec.encode(moves),
        moveCount = moves.size,
        elapsedMs = elapsedMs,
        hints = hints,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        ref = PuzzleRef(puzzleId, Level.valueOf(level), Puzzle.decode(puzzle)),
        moves = MoveCodec.decode(moves),
        elapsedMs = elapsedMs,
        hints = hints,
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
