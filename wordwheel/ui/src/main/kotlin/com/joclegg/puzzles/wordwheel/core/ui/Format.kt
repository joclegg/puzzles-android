package com.joclegg.puzzles.wordwheel.core.ui

import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef
import com.joclegg.puzzles.wordwheel.engine.Guess
import com.joclegg.puzzles.wordwheel.engine.Level
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
