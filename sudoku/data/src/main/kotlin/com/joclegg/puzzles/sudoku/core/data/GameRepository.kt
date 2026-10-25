package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.MoveCodec
import com.joclegg.puzzles.sudoku.engine.PuzzleCodec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(
    private val dao: GameDao,
    private val background: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val inProgress: Flow<List<SavedGame>> = dao.observeInProgress().map { rows -> rows.map { it.toModel() } }

    val history: Flow<List<SavedGame>> = dao.observeHistory().map { rows -> rows.map { it.toModel() } }

    val bestTimes: Flow<List<BestTime>> =
        combine(dao.observeBestTimes(), dao.observePastBests()) { rows, pastBests ->
            val played =
                rows.map {
                    BestTime(GameMode.valueOf(it.mode), Difficulty.valueOf(it.difficulty), it.bestMs, it.averageMs.toLong(), it.completed)
                }
            val past = pastBests.map { BestTime(GameMode.valueOf(it.mode), Difficulty.valueOf(it.difficulty), it.bestMs, 0, 0) }
            (played + past)
                .groupBy { it.mode to it.difficulty }
                .values
                .map { level -> level.first { it.completed > 0 || level.size == 1 }.copy(bestMs = level.minOf { it.bestMs }) }
        }

    suspend fun setPastBest(
        mode: GameMode,
        difficulty: Difficulty,
        bestMs: Long,
    ) = dao.upsertPastBest(PastBestEntity(mode.name, difficulty.name, bestMs))

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(
        ref: PuzzleRef,
        mistakeCheck: MistakeCheck,
    ): SavedGame {
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.ref.slot == ref.slot }
            .forEach { dao.update(it.copy(status = GameStatus.ABANDONED).toEntity()) }
        val game =
            SavedGame(
                id = 0,
                ref = ref,
                values = ref.puzzle.givens,
                notes = List(ref.puzzle.givens.size) { 0 },
                elapsedMs = 0,
                mistakes = 0,
                mistakeCheck = mistakeCheck,
                status = GameStatus.IN_PROGRESS,
                startedAt = now(),
                finishedAt = null,
            )
        return game.copy(id = dao.insert(game.toEntity()))
    }

    suspend fun save(game: SavedGame) = dao.update(withContext(background) { game.toEntity() })

    suspend fun reveal(game: SavedGame) {
        dao.update(withContext(background) { game.copy(status = GameStatus.REVEALED, finishedAt = now()).toEntity() })
    }

    suspend fun complete(game: SavedGame): Boolean {
        dao.update(withContext(background) { game.copy(status = GameStatus.COMPLETED, finishedAt = now()).toEntity() })
        val difficulty = game.ref.difficulty ?: return false
        if (game.ref.custom) {
            return false
        }
        val previous =
            listOfNotNull(
                dao.bestTimeExcluding(game.ref.mode.name, difficulty.name, game.id),
                dao.pastBest(game.ref.mode.name, difficulty.name),
            ).minOrNull()
        return previous == null || game.elapsedMs < previous
    }

    suspend fun playedPuzzleIds(mode: GameMode): Set<String> = dao.playedPuzzleIds(mode.name).toSet()
}

internal fun SavedGame.toEntity() =
    GameEntity(
        id = id,
        puzzleId = ref.id,
        mode = ref.mode.name,
        difficulty = ref.difficulty?.name,
        custom = ref.custom,
        puzzle = PuzzleCodec.encode(ref.puzzle),
        cellValues = values.joinToString(""),
        notes = notes.joinToString(","),
        elapsedMs = elapsedMs,
        mistakes = mistakes,
        mistakeLevel = mistakeCheck.name,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
        undoHistory = MoveCodec.encode(moves),
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        ref =
            PuzzleRef(
                id = puzzleId,
                mode = GameMode.valueOf(mode),
                difficulty = difficulty?.let(Difficulty::valueOf),
                puzzle = PuzzleCodec.decode(puzzle),
                custom = custom,
            ),
        values = cellValues.map { it.digitToInt() },
        notes = notes.split(',').map { it.toInt() },
        elapsedMs = elapsedMs,
        mistakes = mistakes,
        mistakeCheck = MistakeCheck.valueOf(mistakeLevel),
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
        moves = MoveCodec.decode(undoHistory),
    )
