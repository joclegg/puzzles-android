package com.joclegg.puzzles.core.data

import java.util.concurrent.ConcurrentHashMap

open class TextPuzzleBank<K : Any, P>(
    private val read: (String) -> String,
    private val path: (K) -> String,
    private val decode: (String) -> P,
) {
    private val loaded = ConcurrentHashMap<K, List<P>>()

    fun puzzles(key: K): List<P> =
        loaded.getOrPut(key) {
            read(path(key)).lines().filter { it.isNotBlank() }.map(decode)
        }
}
