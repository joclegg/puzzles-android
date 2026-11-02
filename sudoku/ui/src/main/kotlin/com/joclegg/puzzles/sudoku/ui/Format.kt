package com.joclegg.puzzles.sudoku.ui

import androidx.compose.ui.res.stringResource
import com.joclegg.puzzles.sudoku.engine.Difficulty

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
