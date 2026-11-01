package com.joclegg.puzzles.kakuro.core.ui

import com.joclegg.puzzles.kakuro.core.data.PuzzleRef
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun sizeLabel(size: Int) = "$size x $size"

fun PuzzleRef.title(): String =
    if (isDaily) {
        val date = LocalDate.parse(id.removePrefix(PuzzleRef.DAILY_PREFIX).substringBefore('_'))
        "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${sizeLabel(size)}"
    } else {
        sizeLabel(size)
    }
