package com.joclegg.puzzles.wordwheel.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

fun wheelTag(index: Int) = "wheel_letter_${index + 1}"

fun cellTag(
    row: Int,
    col: Int,
) = "cell_r${row + 1}c${col + 1}"
