package com.joclegg.puzzles.solitaire.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameStateTest {
    @Test
    fun `given an ace on the tableau when it is tapped then it goes to its foundation`() {
        // Arrange
        val game = state("/AH|/;;;|||")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0)

        // Assert
        assertEquals(listOf(Card.parse("AH")), after.board.foundations[2])
    }

    @Test
    fun `given a card that fits a column and a foundation when it is tapped then the foundation wins`() {
        // Arrange
        val game = state("/2H|/3S;;;||AH|")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0)

        // Assert
        assertEquals(2, after.board.foundations[2].size)
    }

    @Test
    fun `given a run when its first card is tapped then the whole run moves`() {
        // Arrange
        val game = state("4C/TH9S|/JC;;;|||")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 1)

        // Assert
        assertEquals(board("/4C|/JCTH9S;;;|||"), after.board)
    }

    @Test
    fun `given cards build up when a king is tapped then it goes to its foundation`() {
        // Arrange
        val game = state("4C/KH|/;;;|||").copy(order = BuildOrder.UP)

        // Act
        val after = game.tapCard(Pile.Tableau(0), 1)

        // Assert
        assertEquals(board("/4C|/;;;||KH|"), after.board)
    }

    @Test
    fun `given a card with nowhere to go when it is tapped then it stays selected`() {
        // Arrange
        val game = state("/5H|/9C;;;|||")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0)

        // Assert
        assertEquals(Selection(Pile.Tableau(0), 1), after.selection)
    }

    @Test
    fun `given one tap move is off when a card is tapped and then a target then the card moves to the target`() {
        // Arrange
        val game = state("/QH|/KS|/KC;;;|||", oneTapMove = false)

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0).tapCard(Pile.Tableau(2), 0)

        // Assert
        assertEquals(board("/|/KS|/KCQH;;;|||"), after.board)
    }

    @Test
    fun `given a selected king when an empty column is tapped then it moves there`() {
        // Arrange
        val game = state("QD/KH|/;;;|||", oneTapMove = false).tapCard(Pile.Tableau(0), 1)

        // Act
        val after = game.tapEmptyPile(Pile.Tableau(1))

        // Assert
        assertEquals(board("/QD|/KH;;;|||"), after.board)
    }

    @Test
    fun `given a king at the bottom of its column when it is tapped then it is not moved to an empty column`() {
        // Arrange
        val game = state("/KH|/;;;|||")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0)

        // Assert
        assertEquals(0, after.moveCount)
    }

    @Test
    fun `given a face down card when it is tapped then nothing happens`() {
        // Arrange
        val game = state("AS/KH|/;;;|||")

        // Act
        val after = game.tapCard(Pile.Tableau(0), 0)

        // Assert
        assertEquals(Pair(0, null), Pair(after.moveCount, after.selection))
    }

    @Test
    fun `given a move that turned a card over when it is undone then the card is face down again`() {
        // Arrange
        val game = state("2D/AH|/;;;|||").tapCard(Pile.Tableau(0), 1)

        // Act
        val after = game.undo()

        // Assert
        assertEquals(board("2D/AH|/;;;|||"), after.board)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 5, 30])
    fun `given several draws when they are all undone then the board is back to the deal`(draws: Int) {
        // Arrange
        val deal = Board.deal(seed = 3)
        val played = (1..draws).fold(GameState(deal, DrawMode.DRAW_THREE)) { game, _ -> game.tapStock() }

        // Act
        val undone = (1..draws).fold(played) { game, _ -> game.undo() }

        // Assert
        assertEquals(deal, undone.board)
    }

    @Test
    fun `given four kings left when they all go up then the game is won and undo does nothing`() {
        // Arrange
        val game = state("/KC|/KD|/KH|/KS;;;${foundationsMissingKings()}")

        // Act
        val after = (0..3).fold(game) { played, column -> played.tapCard(Pile.Tableau(column), 0) }

        // Assert
        assertEquals(true, after.isWon)
        assertEquals(after, after.undo())
    }

    @Test
    fun `given an empty stock and waste when the stock is tapped then nothing changes`() {
        // Arrange
        val game = state("/KS;;;|||")

        // Act
        val after = game.tapStock()

        // Assert
        assertEquals(0, after.moveCount)
    }

    @Test
    fun `given a restarted game when its moves are read then they are empty and the board is the deal`() {
        // Arrange
        val game = state("/;2C3C;;|||").tapStock().tapStock()

        // Act
        val after = game.restart()

        // Assert
        assertEquals(board("/;2C3C;;|||"), after.board)
        assertNull(after.selection)
    }

    @Test
    fun `given a run dragged onto a column where it fits when it is dropped then it moves there`() {
        // Arrange
        val game = state("4C/TH9S|/JC|/KD;;;|||")

        // Act
        val after = game.drop(Pile.Tableau(0), 1, Pile.Tableau(1))

        // Assert
        assertEquals(board("/4C|/JCTH9S|/KD;;;|||"), after.board)
    }

    @Test
    fun `given a card dropped where it does not fit when it is dropped then nothing moves`() {
        // Arrange
        val game = state("4C/TH9S|/JC|/KD;;;|||")

        // Act
        val after = game.drop(Pile.Tableau(0), 1, Pile.Tableau(2))

        // Assert
        assertEquals(0, after.moveCount)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '#',
        value = [
            "/KH|/AHQS;;;||| # true",
            "KH/|/AHQS;;;||| # false",
            "/KH|/AH;QS;;||| # false",
            "/KH|/AH;;QS;||| # false",
        ],
    )
    fun `given a board when auto finish is checked then it is offered only with every card face up and nothing to draw`(
        text: String,
        expected: Boolean,
    ) {
        // Act
        val offered = state(text).canAutoFinish

        // Assert
        assertEquals(expected, offered)
    }

    @Test
    fun `given every card face up when auto finish is played then the game is won`() {
        // Arrange
        val game = state("/KC|/KD|/KH|/KS;;;${foundationsMissingKings()}")

        // Act
        val after = game.autoFinish()

        // Assert
        assertEquals(4, game.autoFinishMoves().size)
        assertEquals(true, after.isWon)
    }

    @Test
    fun `given cards build up when auto finish is played then the foundations build down from the kings`() {
        // Arrange
        val foundations =
            Suit.entries.joinToString("|") { suit ->
                Card.deck
                    .filter { it.suit == suit && it.rank > Card.ACE }
                    .reversed()
                    .joinToString("") { it.code }
            }
        val game = GameState(board("/AC|/AD|/AH|/AS;;;$foundations"), DrawMode.DRAW_ONE, order = BuildOrder.UP)

        // Act
        val after = game.autoFinish()

        // Assert
        assertEquals(true, after.isWon)
    }

    @Test
    fun `given a face down card when auto finish is played then nothing changes`() {
        // Arrange
        val game = state("KH/AH|/;;;|||")

        // Act
        val after = game.autoFinish()

        // Assert
        assertEquals(game, after)
    }

    @ParameterizedTest
    @CsvSource("0, false", "3, false", "4, true")
    fun `given three cards to draw when the stock is drawn some times then it counts as cycled only after a full pass`(
        draws: Int,
        expected: Boolean,
    ) {
        // Arrange
        val game = state("/KH|/;2C3C4C;;|||")

        // Act
        val drawn = (1..draws).fold(game) { played, _ -> played.tapStock() }

        // Assert
        assertEquals(expected, drawn.cycledStock)
    }

    @Test
    fun `given a full pass when a card is played then the stock is no longer cycled`() {
        // Arrange
        val cycled = (1..4).fold(state("/4S|/;5C3H;;|||")) { played, _ -> played.tapStock() }

        // Act
        val played = cycled.drop(Pile.Waste, cycled.board.waste.size - 1, Pile.Tableau(0)).tapStock()

        // Assert
        assertEquals(true, cycled.cycledStock)
        assertEquals(false, played.cycledStock)
    }

    @Test
    fun `given nothing left to draw when the stock is checked then it counts as cycled`() {
        // Act
        val cycled = state("/KH|/QS;;;|||").cycledStock

        // Assert
        assertEquals(true, cycled)
    }
}
