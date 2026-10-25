package com.joclegg.puzzles.solitaire.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.Pile

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dialogTestTags(): Modifier = semantics { testTagsAsResourceId = true }

val CardSelectedKey = SemanticsPropertyKey<Boolean>("CardSelected")
var SemanticsPropertyReceiver.cardSelected by CardSelectedKey

val CardFaceUpKey = SemanticsPropertyKey<Boolean>("CardFaceUp")
var SemanticsPropertyReceiver.cardFaceUp by CardFaceUpKey

fun cardTag(card: Card) = "card_${card.code}"

fun pileTag(pile: Pile) =
    when (pile) {
        Pile.Waste -> "waste"
        is Pile.Foundation -> "foundation_${pile.index + 1}"
        is Pile.Tableau -> "tableau_${pile.index + 1}"
    }

fun faceDownTag(
    column: Int,
    index: Int,
) = "tableau_${column + 1}_down_${index + 1}"
