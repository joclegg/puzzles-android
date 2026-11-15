package com.joclegg.puzzles.crossword.ui

import com.joclegg.puzzles.crossword.core.data.PuzzleRef
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun sizeLabel(size: Int) =
    when (size) {
        11 -> "Small"
        13 -> "Medium"
        else -> "Large"
    }

fun PuzzleRef.title(): String =
    if (isDaily) {
        val date = LocalDate.parse(id.removePrefix(PuzzleRef.DAILY_PREFIX).substringBefore('_'))
        "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${sizeLabel(size)}"
    } else {
        sizeLabel(size)
    }
