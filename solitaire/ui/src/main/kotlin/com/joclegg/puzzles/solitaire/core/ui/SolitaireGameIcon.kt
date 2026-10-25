package com.joclegg.puzzles.solitaire.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.solitaire.engine.Card
import com.joclegg.puzzles.solitaire.engine.Suit

@Composable
fun SolitaireGameIcon(modifier: Modifier = Modifier) {
    val card = DpSize(34.dp, 48.dp)
    Box(modifier.size(56.dp)) {
        PlayingCard(Card(Card.KING, Suit.SPADES), card, selected = false, modifier = Modifier.offset(x = 2.dp, y = 2.dp))
        PlayingCard(Card(Card.ACE, Suit.HEARTS), card, selected = false, modifier = Modifier.offset(x = 20.dp, y = 6.dp))
    }
}
