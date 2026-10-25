package com.joclegg.puzzles.sightlines.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.joclegg.puzzles.sightlines.engine.Dot

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

val DotKey = SemanticsPropertyKey<Dot>("Dot")
var SemanticsPropertyReceiver.dot by DotKey

val DotGivenKey = SemanticsPropertyKey<Boolean>("DotGiven")
var SemanticsPropertyReceiver.dotGiven by DotGivenKey

val DotBrokenKey = SemanticsPropertyKey<Boolean>("DotBroken")
var SemanticsPropertyReceiver.dotBroken by DotBrokenKey

val DotHintKey = SemanticsPropertyKey<Boolean>("DotHint")
var SemanticsPropertyReceiver.dotHint by DotHintKey

fun dotTag(
    cell: Int,
    size: Int,
) = "dot_r${cell / size + 1}c${cell % size + 1}"
