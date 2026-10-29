package com.joclegg.puzzles.solitaire.core.data

import com.joclegg.puzzles.solitaire.engine.Board
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.Move
import java.time.LocalDate

enum class GameStatus { IN_PROGRESS, WON, LOST, ABANDONED }

data class DealRef(
    val id: String,
    val drawMode: DrawMode,
    val board: Board,
    val order: BuildOrder = BuildOrder.DOWN,
) {
    val isDaily: Boolean get() = id.startsWith(DAILY_PREFIX)

    val slot: String
        get() =
            if (isDaily) {
                id
            } else {
                RANDOM_SLOT
            }

    companion object {
        const val DAILY_PREFIX = "daily_"
        private const val RANDOM_SLOT = "deal"
        private const val RANDOM_PREFIX = "deal_"

        fun random(
            seed: Long,
            drawMode: DrawMode,
            order: BuildOrder = BuildOrder.DOWN,
            winnable: WinnableDeals? = null,
        ) = DealRef(
            "$RANDOM_PREFIX$seed",
            drawMode,
            Board.deal(winnable?.random(seed, drawMode, order) ?: seed),
            order,
        )

        fun daily(
            date: LocalDate,
            drawMode: DrawMode,
            order: BuildOrder = BuildOrder.DOWN,
            winnable: WinnableDeals? = null,
        ) = DealRef(
            dailyId(date),
            drawMode,
            Board.deal(winnable?.daily(date, drawMode, order) ?: (date.toEpochDay() * DAILY_SEED_SPREAD)),
            order,
        )

        fun dailyId(date: LocalDate) = "$DAILY_PREFIX$date"

        private const val DAILY_SEED_SPREAD = 7_919L
    }
}

data class SavedGame(
    val id: Long,
    val deal: DealRef,
    val moves: List<Move>,
    val elapsedMs: Long,
    val status: GameStatus,
    val startedAt: Long,
    val finishedAt: Long?,
)

data class DrawStats(
    val drawMode: DrawMode,
    val played: Int,
    val won: Int,
    val bestMs: Long?,
    val averageMs: Long?,
    val fewestMoves: Int?,
)
