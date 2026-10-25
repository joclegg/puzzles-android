package com.joclegg.puzzles.fiveletters.core.data

import com.joclegg.puzzles.fiveletters.engine.MAX_GUESSES
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

    val stats: Flow<WordStats> = history.map(::statsOf)

    suspend fun get(id: Long): SavedGame? = dao.get(id)?.toModel()

    suspend fun start(
        ref: WordRef,
        hardMode: Boolean,
    ): SavedGame {
        dao
            .inProgress()
            .map { it.toModel() }
            .filter { it.ref.slot == ref.slot }
            .forEach { dao.update(it.copy(status = GameStatus.ABANDONED).toEntity()) }
        val game = SavedGame(0, ref, emptyList(), hardMode, GameStatus.IN_PROGRESS, now(), null)
        return game.copy(id = dao.insert(game.toEntity()))
    }

    suspend fun save(game: SavedGame) = dao.update(withContext(background) { game.toEntity() })

    suspend fun finish(
        game: SavedGame,
        won: Boolean,
    ) {
        val status =
            if (won) {
                GameStatus.WON
            } else {
                GameStatus.LOST
            }
        dao.update(withContext(background) { game.copy(status = status, finishedAt = now()).toEntity() })
    }
}

internal fun statsOf(history: List<SavedGame>): WordStats {
    val finished = history.filter { it.status == GameStatus.WON || it.status == GameStatus.LOST }.sortedBy { it.finishedAt }
    val streaks =
        finished.runningFold(0) { streak, game ->
            if (game.status == GameStatus.WON) {
                streak + 1
            } else {
                0
            }
        }
    return WordStats(
        played = finished.size,
        won = finished.count { it.status == GameStatus.WON },
        currentStreak = streaks.last(),
        maxStreak = streaks.max(),
        guessCounts = (1..MAX_GUESSES).map { count -> finished.count { it.status == GameStatus.WON && it.guesses.size == count } },
    )
}

internal fun SavedGame.toEntity() =
    GameEntity(
        id = id,
        wordId = ref.id,
        answer = ref.answer,
        guesses = guesses.joinToString(","),
        hardMode = hardMode,
        status = status.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

internal fun GameEntity.toModel() =
    SavedGame(
        id = id,
        ref = WordRef(wordId, answer),
        guesses = guesses.split(',').filter { it.isNotEmpty() },
        hardMode = hardMode,
        status = GameStatus.valueOf(status),
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
