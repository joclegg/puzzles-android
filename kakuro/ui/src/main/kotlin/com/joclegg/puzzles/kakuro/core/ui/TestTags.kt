package com.joclegg.puzzles.kakuro.core.ui

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

val CellGivenKey = SemanticsPropertyKey<Boolean>("CellGiven")
var SemanticsPropertyReceiver.cellGiven by CellGivenKey

fun cellTag(
    cell: Int,
    size: Int,
) = "cell_r${cell / size + 1}c${cell % size + 1}"

fun digitTag(digit: Int) = "digit_$digit"
