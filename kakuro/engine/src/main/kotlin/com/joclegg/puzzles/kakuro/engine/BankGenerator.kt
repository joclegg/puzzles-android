package com.joclegg.puzzles.kakuro.engine

import java.io.File
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val directory = File(args[0]).apply { mkdirs() }
    val counts = args.drop(1).map(String::toInt)
    val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    SIZES.zip(counts).forEach { (size, count) ->
        val puzzles =
            (1..count)
                .map { index ->
                    pool.submit<Puzzle> { Generator.generate(size, size * SEED_SPREAD + index) }
                }.map { it.get() }
        File(directory, "$size.txt").writeText(puzzles.joinToString("\n", postfix = "\n") { it.encode() })
        println("Kakuro $size x $size: $count puzzles, ${puzzles.sumOf { it.givens.size }} starting digits in all")
    }
    pool.shutdown()
}

private const val SEED_SPREAD = 1_000_000L
