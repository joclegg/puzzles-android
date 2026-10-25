package com.joclegg.puzzles.fiveletters.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.joclegg.puzzles.fiveletters.engine.Mark

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

val LetterMarkKey = SemanticsPropertyKey<Mark>("LetterMark")
var SemanticsPropertyReceiver.letterMark by LetterMarkKey

fun letterTag(
    row: Int,
    col: Int,
) = "letter_r${row + 1}c${col + 1}"

fun keyTag(letter: Char) = "key_$letter"
