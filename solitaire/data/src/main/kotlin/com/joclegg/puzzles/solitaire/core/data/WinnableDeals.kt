package com.joclegg.puzzles.solitaire.core.data

import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class WinnableDeals(
    private val read: (String) -> String,
) {
    private val loaded = ConcurrentHashMap<Pair<DrawMode, BuildOrder>, List<Long>>()

    fun seeds(
        drawMode: DrawMode,
        order: BuildOrder,
    ): List<Long> =
        loaded.getOrPut(drawMode to order) {
            read("solitaire/${fileName(drawMode)}_${order.name.lowercase()}.txt")
                .lines()
                .filter { it.isNotBlank() }
                .map { it.trim().toLong() }
        }

    fun random(
        seed: Long,
        drawMode: DrawMode,
        order: BuildOrder,
    ): Long {
        val seeds = seeds(drawMode, order)
        return seeds[Random(seed).nextInt(seeds.size)]
    }

    fun daily(
        date: LocalDate,
        drawMode: DrawMode,
        order: BuildOrder,
    ): Long {
        val seeds = seeds(drawMode, order)
        val count = minOf(DAILY_SEED_LINES, seeds.size)
        return seeds[Math.floorMod(date.toEpochDay(), count.toLong()).toInt()]
    }

    private fun fileName(drawMode: DrawMode) =
        when (drawMode) {
            DrawMode.DRAW_ONE -> "drawOne"
            DrawMode.DRAW_THREE -> "drawThree"
        }

    private companion object {
        const val DAILY_SEED_LINES = 1000
    }
}
