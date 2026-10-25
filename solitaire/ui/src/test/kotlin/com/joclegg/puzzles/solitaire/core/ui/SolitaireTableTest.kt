package com.joclegg.puzzles.solitaire.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.solitaire.engine.BoardCodec
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.GameState
import com.joclegg.puzzles.solitaire.engine.Pile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SolitaireTableTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        game: GameState,
        onCardClick: (Pile, Int) -> Unit = { _, _ -> },
        onStockClick: () -> Unit = {},
    ) {
        compose.setContent {
            SolitaireTheme(darkTheme = false) {
                SolitaireTable(game.toTableUi(), onStockClick, onCardClick, onEmptyPileClick = {})
            }
        }
    }

    private fun game(
        text: String,
        oneTapMove: Boolean = false,
    ) = GameState(BoardCodec.decode(text), DrawMode.DRAW_ONE, oneTapMove = oneTapMove)

    @Test
    fun `given a selected run when the table is shown then every card in the run is marked selected`() {
        // Arrange
        val selected = game("4C/TH9S|/AD;;;|||").tapCard(Pile.Tableau(0), 1)

        // Act
        show(selected)

        // Assert
        compose.onNodeWithTag("card_TH").assert(SemanticsMatcher.expectValue(CardSelectedKey, true))
        compose.onNodeWithTag("card_9S").assert(SemanticsMatcher.expectValue(CardSelectedKey, true))
        compose.onNodeWithTag("card_AD").assert(SemanticsMatcher.expectValue(CardSelectedKey, false))
    }

    @Test
    fun `given a column with a hidden card when the table is shown then the hidden card is face down`() {
        // Act
        show(game("4C/TH;;;|||"))

        // Assert
        compose.onNodeWithTag(faceDownTag(0, 0)).assert(SemanticsMatcher.expectValue(CardFaceUpKey, false))
    }

    @Test
    fun `given a card partly covered by the next one when its visible edge is tapped then its pile and index are reported`() {
        // Arrange
        var tapped: Pair<Pile, Int>? = null
        show(game("4C/TH9S|/AD;;;|||"), onCardClick = { pile, index -> tapped = pile to index })

        // Act
        compose.onNodeWithTag("card_TH").performTouchInput { click(Offset(centerX, VISIBLE_EDGE_PX)) }

        // Assert
        assertEquals(Pile.Tableau(0) to 1, tapped)
    }

    @Test
    fun `given cards in the stock when it is tapped then the stock click is reported`() {
        // Arrange
        var taps = 0
        show(game("/AD;2C3C;;|||"), onStockClick = { taps++ })

        // Act
        compose.onNodeWithTag("stock").performClick()

        // Assert
        assertEquals(1, taps)
    }

    private companion object {
        const val VISIBLE_EDGE_PX = 4f
    }
}
