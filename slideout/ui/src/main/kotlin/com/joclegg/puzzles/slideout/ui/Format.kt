package com.joclegg.puzzles.slideout.ui

import com.joclegg.puzzles.slideout.core.data.PuzzleRef
import com.joclegg.puzzles.slideout.engine.Level
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Level.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun PuzzleRef.title(): String =
    if (isDaily) {
        val date = LocalDate.parse(id.removePrefix(PuzzleRef.DAILY_PREFIX).substringBefore('_'))
        "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${level.label()}"
    } else {
        level.label()
    }
