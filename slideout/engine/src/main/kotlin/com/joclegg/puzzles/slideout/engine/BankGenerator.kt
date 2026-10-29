package com.joclegg.puzzles.slideout.engine

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {
    val directory = File(args[0]).apply { mkdirs() }
    val counts = args.drop(1).map(String::toInt)
    val threads = Runtime.getRuntime().availableProcessors()
    val pool = Executors.newFixedThreadPool(threads)
    val seedBase = System.getenv("BANK_SEED_BASE")?.toLong() ?: System.currentTimeMillis() % SEED_SPREAD
    val deadline = System.getenv("BANK_MINUTES")?.toLong()?.let { System.currentTimeMillis() + it * MS_PER_MINUTE } ?: Long.MAX_VALUE

    val shared = System.getenv("BANK_SEEN")?.let(::File)

    fun timeUp() = System.currentTimeMillis() > deadline
    Level.entries.zip(counts).filter { (_, count) -> count > 0 }.forEach { (level, count) ->
        val file = File(directory, "${level.name.lowercase()}.txt")
        val found = ConcurrentHashMap<String, Puzzle>()
        if (file.exists()) {
            file
                .readLines()
                .filter { it.isNotBlank() }
                .map(Puzzle::decode)
                .forEach { found[it.board.identity()] = it }
        }
        val seen = SeenElsewhere(shared)
        println("Slide Out ${level.name}: starting with ${found.size} of $count")
        val seeds = AtomicLong(level.ordinal * SEED_SPREAD * SEED_SPREAD + seedBase * SEED_SPREAD)
        val workers =
            (1..threads).map {
                pool.submit {
                    while (found.size < count && !timeUp()) {
                        val puzzle = Generator.generate(level, seeds.incrementAndGet()) { found.size >= count || timeUp() } ?: continue
                        val identity = puzzle.board.identity()
                        if (!seen.contains(identity) && found.putIfAbsent(identity, puzzle) == null) {
                            synchronized(file) { file.appendText(puzzle.encode() + "\n") }
                            println("Slide Out ${level.name}: ${found.size} of $count (fewest moves ${puzzle.par})")
                        }
                    }
                }
            }
        workers.forEach { it.get() }
        println("Slide Out ${level.name}: done")
    }
    pool.shutdown()
}

private class SeenElsewhere(
    private val file: File?,
) {
    private var modified = -1L
    private var identities = emptySet<String>()

    @Synchronized
    fun contains(identity: String): Boolean {
        if (file != null && file.exists() && file.lastModified() != modified) {
            modified = file.lastModified()
            identities =
                file
                    .readLines()
                    .filter { it.isNotBlank() }
                    .map { Puzzle.decode(it).board.identity() }
                    .toSet()
        }
        return identity in identities
    }
}

private const val SEED_SPREAD = 1_000_000L
private const val MS_PER_MINUTE = 60_000L
