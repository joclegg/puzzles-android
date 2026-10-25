package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.joclegg.puzzles.twotone.engine.Tile

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

val TileKey = SemanticsPropertyKey<Tile>("Tile")
var SemanticsPropertyReceiver.tile by TileKey

val TileGivenKey = SemanticsPropertyKey<Boolean>("TileGiven")
var SemanticsPropertyReceiver.tileGiven by TileGivenKey

val TileBrokenKey = SemanticsPropertyKey<Boolean>("TileBroken")
var SemanticsPropertyReceiver.tileBroken by TileBrokenKey

val TileHintKey = SemanticsPropertyKey<Boolean>("TileHint")
var SemanticsPropertyReceiver.tileHint by TileHintKey

fun tileTag(
    cell: Int,
    size: Int,
) = "tile_r${cell / size + 1}c${cell % size + 1}"
