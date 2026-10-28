package com.joclegg.puzzles.solitaire.core.data

import com.joclegg.puzzles.solitaire.engine.BoardCodec
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.MoveCodec
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

    val stats: Flow<List<DrawStats>> = dao.observeHistory().map { rows -> DrawMode.entries.map { mode -> rows.statsFor(mode) } }

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(deal: DealRef): SavedGame {
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.deal.slot == deal.slot }
            .forEach { dao.save(it.copy(status = GameStatus.ABANDONED).toEntity(now())) }
        val game =
            SavedGame(
                id = 0,
                deal = deal,
                moves = emptyList(),
                elapsedMs = 0,
                status = GameStatus.IN_PROGRESS,
                startedAt = now(),
                finishedAt = null,
            )
        return game.copy(id = dao.insert(game.toEntity(now())))
    }

    suspend fun save(game: SavedGame) = dao.save(withContext(background) { game.toEntity(now()) })

    suspend fun lose(game: SavedGame) {
        dao.save(withContext(background) { game.copy(status = GameStatus.LOST, finishedAt = now()).toEntity(now()) })
    }

    suspend fun win(game: SavedGame): Boolean {
        dao.save(withContext(background) { game.copy(status = GameStatus.WON, finishedAt = now()).toEntity(now()) })
        val previous = dao.bestTimeExcluding(game.deal.drawMode.name, game.id)
        return previous == null || game.elapsedMs < previous
    }
}

private fun List<GameEntity>.statsFor(mode: DrawMode): DrawStats {
    val finished = filter { it.drawMode == mode.name && it.status != GameStatus.IN_PROGRESS.name }
    val won = finished.filter { it.status == GameStatus.WON.name }
    return DrawStats(
        drawMode = mode,
        played = finished.size,
        won = won.size,
        bestMs = won.minOfOrNull { it.elapsedMs },
        averageMs =
            won
                .takeIf { it.isNotEmpty() }
                ?.map { it.elapsedMs }
                ?.average()
                ?.toLong(),
        fewestMoves = won.minOfOrNull { it.moveCount },
    )
}

internal fun SavedGame.toEntity(updatedAt: Long) =
    GameEntity(
        id = id,
        uuid = UUID.randomUUID().toString(),
        updatedAt = updatedAt,
        dealId = deal.id,
        drawMode = deal.drawMode.name,
        buildOrder = deal.order.name,
        deal = BoardCodec.encode(deal.board),
        moves = MoveCodec.encode(moves),
        moveCount = moves.size,
        elapsedMs = elapsedMs,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        deal = DealRef(dealId, DrawMode.valueOf(drawMode), BoardCodec.decode(deal), BuildOrder.valueOf(buildOrder)),
        moves = MoveCodec.decode(moves),
        elapsedMs = elapsedMs,
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
