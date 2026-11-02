package com.joclegg.puzzles.sightlines.ui

import androidx.compose.ui.res.stringResource
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef
import com.joclegg.puzzles.sightlines.engine.Hint
import com.joclegg.puzzles.sightlines.engine.Reason
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

fun Hint.message(): String =
    when (this) {
        is Hint.Mistake -> {
            if (cells.size == 1) {
                "This dot is the wrong colour."
            } else {
                "These ${cells.size} dots are the wrong colour."
            }
        }

        is Hint.Next -> {
            when (deduction.reason) {
                Reason.SATISFIED -> "This number already sees enough blue dots, so the dots that close off its view are red."
                Reason.TOO_MANY -> "A blue dot here would let this number see too many, so it is red."
                Reason.NEEDS -> "This dot can't see enough the other ways, so this way carries on blue."
                Reason.ALONE -> "A blue dot here would be boxed in and see nothing, so it is red."
            }
        }
    }
