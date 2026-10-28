package com.joclegg.puzzles.fiveletters.core.ui

import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.fiveletters.engine.Rejection
import com.joclegg.puzzles.fiveletters.engine.Words
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private val WORDLE_LAUNCH = LocalDate.of(2021, 6, 19)

fun WordRef.title(): String =
    if (isDaily) {
        "Five Letters ${Words.dayNumber(LocalDate.parse(id.removePrefix(WordRef.DAILY_PREFIX)))}"
    } else if (isNyt) {
        val wordleNumber = ChronoUnit.DAYS.between(WORDLE_LAUNCH, LocalDate.parse(id.removePrefix(WordRef.NYT_PREFIX)))
        "Wordle ${"%,d".format(Locale.US, wordleNumber)}"
    } else {
        "Practice"
    }

fun Rejection.message(): String =
    when (this) {
        Rejection.TOO_SHORT -> "Not enough letters"
        Rejection.NOT_A_WORD -> "Not in the word list"
        Rejection.MUST_USE_HINTS -> "Hard mode: use every hint you've found"
    }
