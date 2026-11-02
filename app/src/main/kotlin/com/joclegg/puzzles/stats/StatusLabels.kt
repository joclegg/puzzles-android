package com.joclegg.puzzles.stats

fun puzzleStatusLabel(name: String): String =
    when (name) {
        "SOLVED", "COMPLETED" -> "Solved"
        "WON" -> "Won"
        "LOST" -> "Lost"
        "IN_PROGRESS" -> "In progress"
        "ABANDONED" -> "Abandoned"
        "REVEALED" -> "Solution shown"
        else -> error("Unknown status $name")
    }

fun hintsSummary(
    status: String,
    hints: Int,
): String =
    if (hints > 0) {
        "$status · $hints hints"
    } else {
        status
    }
