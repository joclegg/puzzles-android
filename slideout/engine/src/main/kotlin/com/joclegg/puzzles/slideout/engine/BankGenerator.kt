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

    fun timeUp() = System.currentTimeMillis() > deadline
    Level.entries.zip(counts).filter { (_, count) -> count > 0 }.forEach { (level, count) ->
        val file = File(directory, "${level.name.lowercase()}.txt")
        val found = ConcurrentHashMap<String, Puzzle>()
        if (file.exists()) {
            file
                .readLines()
                .filter { it.isNotBlank() }
                .map(Puzzle::decode)
                .forEach { found[it.board.encode()] = it }
        }
        println("Slide Out ${level.name}: starting with ${found.size} of $count")
        val seeds = AtomicLong(level.ordinal * SEED_SPREAD * SEED_SPREAD + seedBase * SEED_SPREAD)
        val workers =
            (1..threads).map {
                pool.submit {
                    while (found.size < count && !timeUp()) {
                        val puzzle = Generator.generate(level, seeds.incrementAndGet()) { found.size >= count || timeUp() } ?: continue
                        if (found.putIfAbsent(puzzle.board.encode(), puzzle) == null) {
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

private const val SEED_SPREAD = 1_000_000L
private const val MS_PER_MINUTE = 60_000L
