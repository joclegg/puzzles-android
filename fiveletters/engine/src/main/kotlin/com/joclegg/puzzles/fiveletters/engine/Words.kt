package com.joclegg.puzzles.fiveletters.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

const val WORD_LENGTH = 5
const val MAX_GUESSES = 6

class Words(
    answers: List<String>,
    guesses: List<String>,
) {
    val answers: List<String> = answers.map { it.trim().uppercase() }.filter { it.length == WORD_LENGTH }
    private val allowed: Set<String> = (guesses.map { it.trim().uppercase() } + this.answers).toSet()

    fun isAllowed(word: String) = word in allowed

    fun daily(date: LocalDate): String = dailyOrder[(dayNumber(date) - 1).mod(dailyOrder.size)]

    fun random(seed: Long): String = this.answers.random(Random(seed))

    private val dailyOrder: List<String> by lazy {
        this.answers.take(SHUFFLED_ANSWER_COUNT).shuffled(Random(DAILY_ORDER_SEED)) + this.answers.drop(SHUFFLED_ANSWER_COUNT)
    }

    companion object {
        private const val DAILY_ORDER_SEED = 5L
        private const val SHUFFLED_ANSWER_COUNT = 944
        val FIRST_DAY: LocalDate = LocalDate.of(2026, 10, 25)

        fun dayNumber(date: LocalDate) = ChronoUnit.DAYS.between(FIRST_DAY, date).toInt() + 1
    }
}
