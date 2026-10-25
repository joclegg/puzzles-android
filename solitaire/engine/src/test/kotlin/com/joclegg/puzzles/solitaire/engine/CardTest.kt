package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class CardTest {
    @ParameterizedTest
    @CsvSource("AS,1,SPADES", "TD,10,DIAMONDS", "KH,13,HEARTS", "7C,7,CLUBS")
    fun `given a card code when it is parsed then rank and suit match`(
        code: String,
        rank: Int,
        suit: Suit,
    ) {
        // Act
        val card = Card.parse(code)

        // Assert
        assertEquals(Card(rank, suit), card)
    }

    @ParameterizedTest
    @CsvSource("6H,7S,true", "6S,7H,true", "6H,7D,false", "5H,7S,false", "7S,6H,false")
    fun `given two cards when stacking is checked then only one lower in the other colour fits`(
        moving: String,
        target: String,
        fits: Boolean,
    ) {
        // Act
        val result = Card.parse(moving).stacksOnTableau(Card.parse(target))

        // Assert
        assertEquals(fits, result)
    }

    @Test
    fun `given the deck when it is counted then it has 52 different cards`() {
        // Act
        val deck = Card.deck

        // Assert
        assertEquals(52, deck.toSet().size)
    }
}
