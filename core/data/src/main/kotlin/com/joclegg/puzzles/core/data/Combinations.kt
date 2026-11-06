package com.joclegg.puzzles.core.data

private const val MAX_COMBINATION_DIGIT = 9

object Combinations {
    private val byRun: Map<Pair<Int, Int>, List<Int>> =
        (1 until (1 shl MAX_COMBINATION_DIGIT))
            .groupBy { mask -> Integer.bitCount(mask) to digitsOf(mask).sum() }

    fun masks(
        length: Int,
        sum: Int,
    ): List<Int> = byRun[length to sum].orEmpty()

    fun digitsOf(mask: Int): List<Int> = (1..MAX_COMBINATION_DIGIT).filter { mask and bit(it) != 0 }

    fun bit(digit: Int) = 1 shl (digit - 1)

    fun ordered(
        masks: List<Int>,
        order: CombinationOrder,
    ): List<List<Int>> =
        when (order) {
            CombinationOrder.INCREASING -> masks.map(::digitsOf).sortedWith(Comparator(::compareDigits))
            CombinationOrder.DECREASING -> ordered(masks, CombinationOrder.INCREASING).map { it.reversed() }.reversed()
        }

    private fun compareDigits(
        first: List<Int>,
        second: List<Int>,
    ): Int = first.zip(second).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a - b } ?: (first.size - second.size)
}

enum class CombinationOrder { INCREASING, DECREASING }
