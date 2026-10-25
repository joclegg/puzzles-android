package com.joclegg.puzzles.slideout.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

fun blockTag(index: Int) = "block_$index"
