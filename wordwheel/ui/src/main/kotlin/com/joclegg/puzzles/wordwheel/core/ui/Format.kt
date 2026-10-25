package com.joclegg.puzzles.wordwheel.core.ui

import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef
import com.joclegg.puzzles.wordwheel.engine.Guess
import com.joclegg.puzzles.wordwheel.engine.Level
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val (h, m, s) = Triple(total / 3600, total % 3600 / 60, total % 60)
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

fun Level.label(): String = "$letters letters"

fun PuzzleRef.title(): String =
    if (isDaily) {
        val date = LocalDate.parse(id.removePrefix(PuzzleRef.DAILY_PREFIX).substringBefore('_'))
        "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${level.label()}"
    } else {
        level.label()
    }

fun Guess.message(): String =
    when (this) {
        Guess.FOUND -> "Nice!"
        Guess.BONUS -> "Bonus word!"
        Guess.ALREADY -> "Already found"
        Guess.NOT_A_WORD -> "Not a word"
        Guess.TOO_SHORT -> "Three letters or more"
    }
