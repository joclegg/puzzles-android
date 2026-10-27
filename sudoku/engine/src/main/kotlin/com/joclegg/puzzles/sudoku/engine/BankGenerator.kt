package com.joclegg.puzzles.sudoku.engine

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "puzzles-data/sudoku" }).apply { mkdirs() }
    val perFile = args.getOrElse(1) { "300" }.toInt()
    val seed = args.getOrElse(2) { "2026" }.toLong()
    for (killer in listOf(false, true)) {
        for (level in if (killer) {
            KILLER_LEVELS
        } else {
            CLASSIC_LEVELS
        }) {
            val name = "${if (killer) "killer" else "classic"}_${level.name.lowercase()}"
            val started = System.currentTimeMillis()
            val puzzles =
                generate(
                    killer,
                    level,
                    perFile,
                    seed + level.ordinal * 1000 +
                        if (killer) {
                            99
                        } else {
                            0
                        },
                )
            File(outDir, "$name.txt").writeText(puzzles.joinToString("\n", postfix = "\n") { PuzzleCodec.encode(it) })
            println("$name: ${puzzles.size} in ${(System.currentTimeMillis() - started) / 1000}s")
        }
    }
}

private fun generate(
    killer: Boolean,
    level: Difficulty,
    count: Int,
    seed: Long,
): List<Puzzle> {
    val threads = Runtime.getRuntime().availableProcessors()
    val results = arrayOfNulls<Puzzle>(count)
    val next = AtomicInteger()
    val pool = Executors.newFixedThreadPool(threads)
    repeat(threads) { worker ->
        pool.execute {
            val random = Random(seed * 31 + worker)
            while (true) {
                val generated =
                    (
                        if (killer) {
                            Generator.killer(level, random)
                        } else {
                            Generator.classic(level, random)
                        }
                    ) ?: continue
                val slot = next.getAndIncrement()
                if (slot >= count) {
                    break
                }
                results[slot] = generated.puzzle
            }
        }
    }
    pool.shutdown()
    pool.awaitTermination(1, TimeUnit.DAYS)

    return results.filterNotNull().sortedBy { Grader(it.givens, it.cages).grade().score }
}
