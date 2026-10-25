package com.joclegg.puzzles.twotone.core.ui

import com.joclegg.puzzles.twotone.core.data.PuzzleRef
import com.joclegg.puzzles.twotone.engine.Hint
import com.joclegg.puzzles.twotone.engine.Reason
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

fun Hint.message(): String =
    when (this) {
        is Hint.Mistake -> {
            if (cells.size == 1) {
                "This tile is the wrong colour."
            } else {
                "These ${cells.size} tiles are the wrong colour."
            }
        }

        is Hint.Next -> {
            val line =
                if (deduction.line.isRow) {
                    "row"
                } else {
                    "column"
                }
            when (deduction.reason) {
                Reason.PAIR -> "Two tiles of the same colour sit side by side in this $line, so the tile next to them is the other colour."
                Reason.GAP -> "A gap between two tiles of the same colour in this $line must be the other colour."
                Reason.BALANCE -> "This $line already has half its tiles in one colour, so the rest are the other colour."
                Reason.UNIQUE -> "Filling this $line one way would copy another $line, so it goes the other way."
            }
        }
    }
