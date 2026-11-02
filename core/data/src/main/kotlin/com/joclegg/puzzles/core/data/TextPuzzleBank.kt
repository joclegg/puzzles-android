package com.joclegg.puzzles.core.data

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

open class TextPuzzleBank<K : Any, P>(
    private val read: (String) -> String,
    private val path: (K) -> String,
    private val decode: (String) -> P,
) {
    private val loaded = ConcurrentHashMap<K, List<P>>()
    private val orders = ConcurrentHashMap<Pair<K, Long>, List<Int>>()

    fun puzzles(key: K): List<P> =
        loaded.getOrPut(key) {
            read(path(key)).lines().filter { it.isNotBlank() }.map(decode)
        }

    fun shuffledOrder(
        key: K,
        seed: Long,
    ): List<Int> =
        orders.getOrPut(key to seed) {
            puzzles(key).indices.shuffled(Random(seed))
        }
}
