package com.joclegg.puzzles.crossword.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

val CellWrongKey = SemanticsPropertyKey<Boolean>("CellWrong")
var SemanticsPropertyReceiver.cellWrong by CellWrongKey

val CellSelectedKey = SemanticsPropertyKey<Boolean>("CellSelected")
var SemanticsPropertyReceiver.cellSelected by CellSelectedKey

val CellInWordKey = SemanticsPropertyKey<Boolean>("CellInWord")
var SemanticsPropertyReceiver.cellInWord by CellInWordKey

fun cellTag(
    cell: Int,
    size: Int,
) = "cell_r${cell / size + 1}c${cell % size + 1}"

fun clueTag(
    number: Int,
    isAcross: Boolean,
) = if (isAcross) {
    "clue_${number}_across"
} else {
    "clue_${number}_down"
}

const val INPUT_TAG = "crossword_keyboard"
