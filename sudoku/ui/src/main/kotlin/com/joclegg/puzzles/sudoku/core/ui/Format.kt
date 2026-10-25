package com.joclegg.puzzles.sudoku.core.ui

import com.joclegg.puzzles.sudoku.engine.Difficulty

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val (h, m, s) = Triple(total / 3600, total % 3600 / 60, total % 60)
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

fun Difficulty?.label(): String =
    when (this) {
        Difficulty.FAST -> "Fast"
        Difficulty.BEGINNER -> "Beginner"
        Difficulty.EASY -> "Easy"
        Difficulty.MEDIUM -> "Medium"
        Difficulty.HARD -> "Hard"
        Difficulty.EXPERT -> "Expert"
        Difficulty.EVIL -> "Evil"
        null -> "Expert+"
    }

fun gameTitle(
    killer: Boolean,
    custom: Boolean,
    difficulty: Difficulty?,
    answerKnown: Boolean = true,
): String {
    val kind =
        when {
            custom && killer -> "Custom killer"
            custom -> "Custom"
            killer -> "Killer"
            else -> "Classic"
        }
    return if (answerKnown) {
        "$kind · ${difficulty.label()}"
    } else {
        kind
    }
}
