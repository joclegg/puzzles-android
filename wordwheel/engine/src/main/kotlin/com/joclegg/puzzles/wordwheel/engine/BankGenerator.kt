package com.joclegg.puzzles.wordwheel.engine

import java.io.File
import kotlin.random.Random

fun main(args: Array<String>) {
    val data = File(args[0])
    val output = File(args[1]).apply { mkdirs() }
    val counts = args.drop(2).map(String::toInt)
    val valid = File(data, "3of6game.txt").readLines(Charsets.ISO_8859_1).map { it.trim() }.filter { it.matches(WORD) }
    val blocked = File(data, "blocked.txt").readLines().map { it.trim() }.toSet()
    val common = commonWords(File(data, "2+2+3frq.txt").readLines(Charsets.ISO_8859_1)) - blocked
    val words = WordList(valid, common)
    File(output, "valid.txt").writeText(words.valid.sorted().joinToString("\n", postfix = "\n"))
    Level.entries.zip(counts).forEach { (level, count) ->
        val random = Random(level.ordinal.toLong())
        val bases = words.common.filter { it.length == level.letters }.shuffled(random)
        val puzzles = mutableListOf<Puzzle>()
        val used = mutableSetOf<String>()
        for (base in bases) {
            if (puzzles.size >= count) {
                break
            }
            val key = base.toList().sorted().joinToString("")
            if (key in used) {
                continue
            }
            val puzzle = Generator.generate(words, level, base, random) ?: continue
            used += key
            puzzles += puzzle
        }
        File(output, "${level.name.lowercase()}.txt").writeText(puzzles.joinToString("\n", postfix = "\n") { it.encode() })
        println("Word Wheel ${level.name}: ${puzzles.size} puzzles, ${puzzles.sumOf { it.crossword.placements.size }} words in all")
    }
}

private val WORD = Regex("[a-z]{3,7}")
private const val COMMON_BANDS = 13

private fun commonWords(lines: List<String>): Set<String> {
    var band = 0
    val words = mutableSetOf<String>()
    for (line in lines) {
        val header = Regex("^----- (\\d+) -----").find(line)
        if (header != null) {
            band = header.groupValues[1].toInt()
            continue
        }
        if (band <= COMMON_BANDS) {
            line
                .split(',')
                .map { it.trim() }
                .filter { it.matches(WORD) }
                .forEach { words += it }
        }
    }
    return words
}
