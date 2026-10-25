package com.joclegg.puzzles.kakuro.core.ui

import com.joclegg.puzzles.kakuro.core.data.PuzzleRef
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

fun sizeLabel(size: Int) = "$size x $size"

fun PuzzleRef.title(): String =
    if (isDaily) {
        val date = LocalDate.parse(id.removePrefix(PuzzleRef.DAILY_PREFIX).substringBefore('_'))
        "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${sizeLabel(size)}"
    } else {
        sizeLabel(size)
    }
