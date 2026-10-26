package com.joclegg.puzzles.fiveletters.core.ui

import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.fiveletters.engine.Rejection
import com.joclegg.puzzles.fiveletters.engine.Words
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun WordRef.title(): String =
    if (isDaily) {
        "Five Letters ${Words.dayNumber(LocalDate.parse(id.removePrefix(WordRef.DAILY_PREFIX)))}"
    } else if (isNyt) {
        "NY Times · ${LocalDate.parse(id.removePrefix(WordRef.NYT_PREFIX)).format(DateTimeFormatter.ofPattern("d MMM"))}"
    } else {
        "Practice"
    }

fun Rejection.message(): String =
    when (this) {
        Rejection.TOO_SHORT -> "Not enough letters"
        Rejection.NOT_A_WORD -> "Not in the word list"
        Rejection.MUST_USE_HINTS -> "Hard mode: use every hint you've found"
    }
