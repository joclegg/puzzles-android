package com.joclegg.puzzles.solitaire.core.ui

import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
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

fun DrawMode.label(): String =
    when (this) {
        DrawMode.DRAW_ONE -> "Draw 1"
        DrawMode.DRAW_THREE -> "Draw 3"
    }

fun DealRef.title(): String {
    val kind =
        if (isDaily) {
            val date = LocalDate.parse(id.removePrefix(DealRef.DAILY_PREFIX))
            "Daily · ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
        } else {
            drawMode.label()
        }
    return if (order == BuildOrder.UP) {
        "$kind · Reverse"
    } else {
        kind
    }
}
