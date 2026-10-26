package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStateTest {
    private val emptyCellAnswer4 = Grid.STANDARD.cellAt(0, 2)
    private val emptyCellAnswer6 = Grid.STANDARD.cellAt(0, 3)
    private val given5 = Grid.STANDARD.cellAt(0, 0)
    private val game = GameState(easyPuzzle)

    @Test
    fun `given a selected empty cell when a digit is tapped in cell-first then it is placed`() {
        // Arrange
        val selected = game.tapCell(emptyCellAnswer4)

        // Act
        val next = selected.tapDigit(4)

        // Assert
        assertEquals(4, next.values[emptyCellAnswer4])
    }

    @ParameterizedTest
    @CsvSource("CELL_FIRST", "DIGIT_FIRST")
    fun `given a given cell when a digit is entered or erased then it does not change`(mode: InputMode) {
        // Arrange
        val playing = game.withInputMode(mode)

        // Act
        val next =
            if (mode == InputMode.CELL_FIRST) {
                playing.tapCell(given5).tapDigit(9).erase()
            } else {
                playing.tapDigit(9).tapCell(given5).erase()
            }

        // Assert
        assertEquals(5, next.values[given5])
    }

    @Test
    fun `given an active digit in digit-first when several cells are tapped then each gets the digit`() {
        // Arrange
        val active = game.withInputMode(InputMode.DIGIT_FIRST).tapDigit(4)

        // Act
        val next = active.tapCell(emptyCellAnswer4).tapCell(emptyCellAnswer6)

        // Assert
        assertEquals(listOf(4, 4), listOf(next.values[emptyCellAnswer4], next.values[emptyCellAnswer6]))
    }

    @Test
    fun `given a cell holding the active digit when it is tapped in digit-first then it clears`() {
        // Arrange
        val placed = game.withInputMode(InputMode.DIGIT_FIRST).tapDigit(4).tapCell(emptyCellAnswer4)

        // Act
        val next = placed.tapCell(emptyCellAnswer4)

        // Assert
        assertEquals(0, next.values[emptyCellAnswer4])
    }

    @ParameterizedTest
    @MethodSource("cellsToTapWithActiveDigit")
    fun `given an active digit when any cell is tapped then the active digit stays selected`(cell: Int) {
        // Arrange
        val active =
            game
                .withInputMode(InputMode.DIGIT_FIRST)
                .tapDigit(9)
                .tapCell(emptyCellAnswer4)
                .tapDigit(4)

        // Act
        val next = active.tapCell(cell)

        // Assert
        assertEquals(4, next.activeDigit)
    }

    @Test
    fun `given an active digit when the same digit is tapped again then it is deselected`() {
        // Arrange
        val active = game.withInputMode(InputMode.DIGIT_FIRST).tapDigit(4)

        // Act
        val next = active.tapDigit(4)

        // Assert
        assertNull(next.activeDigit)
    }

    @ParameterizedTest
    @CsvSource(
        "SHOW_WRONG, 9, 9, 1, true",
        "SHOW_WRONG, 4, 4, 0, false",
        "OFF, 9, 9, 0, false",
        "BLOCK_CLASHES, 1, 1, 0, false",
        "BLOCK_CLASHES, 5, 0, 1, false",
    )
    fun `given a mistake checking level when a digit is entered then it is placed, flagged or refused`(
        level: MistakeCheck,
        digit: Int,
        expectedValue: Int,
        expectedMistakes: Int,
        expectedWrong: Boolean,
    ) {
        // Arrange
        val selected = game.copy(mistakeCheck = level).tapCell(emptyCellAnswer4)

        // Act
        val next = selected.tapDigit(digit)

        // Assert
        assertEquals(
            Triple(expectedValue, expectedMistakes, expectedWrong),
            Triple(next.values[emptyCellAnswer4], next.mistakes, next.isWrong(emptyCellAnswer4)),
        )
    }

    @Test
    fun `given block clashes when a digit already in the row is entered then the clashing cells are reported`() {
        // Arrange
        val selected = game.copy(mistakeCheck = MistakeCheck.BLOCK_CLASHES).tapCell(emptyCellAnswer4)

        // Act
        val next = selected.tapDigit(5)

        // Assert
        assertEquals(Rejection(emptyCellAnswer4, setOf(given5)), next.rejected)
    }

    @Test
    fun `given a refused entry when the next cell is tapped then the refusal is cleared`() {
        // Arrange
        val refused = game.copy(mistakeCheck = MistakeCheck.BLOCK_CLASHES).tapCell(emptyCellAnswer4).tapDigit(5)

        // Act
        val next = refused.tapCell(emptyCellAnswer6)

        // Assert
        assertNull(next.rejected)
    }

    @Test
    fun `given show wrong on a puzzle without a known answer when checking is worked out then it falls back to blocking clashes`() {
        // Arrange
        val unverified = GameState(easyPuzzle.copy(solution = null), mistakeCheck = MistakeCheck.SHOW_WRONG)

        // Act
        val level = unverified.effectiveMistakeCheck

        // Assert
        assertEquals(MistakeCheck.BLOCK_CLASHES, level)
    }

    @ParameterizedTest
    @CsvSource("true, 0", "false, 16")
    fun `given a peer's pencil mark when the digit is placed then it is cleared only with auto clean-up on`(
        autoRemoveNotes: Boolean,
        expectedNotes: Int,
    ) {
        // Arrange
        val withNote =
            game
                .copy(options = PlayOptions(autoRemoveNotes = autoRemoveNotes))
                .toggleNotesMode()
                .tapCell(emptyCellAnswer6)
                .tapDigit(4)
                .toggleNotesMode()

        // Act
        val next = withNote.tapCell(emptyCellAnswer4).tapDigit(4)

        // Assert
        assertEquals(expectedNotes, next.notes[emptyCellAnswer6])
    }

    @Test
    fun `given a selected cell when mark is used twice then it is marked and then unmarked`() {
        // Arrange
        val selected = game.tapCell(emptyCellAnswer4)

        // Act
        val marked = selected.mark()

        // Assert
        assertEquals(setOf(emptyCellAnswer4), marked.marked)
        assertEquals(emptySet(), marked.mark().marked)
    }

    @Test
    fun `given number first with mark on when cells are tapped then they are marked and nothing is entered`() {
        // Arrange
        val marking = game.copy(inputMode = InputMode.DIGIT_FIRST).mark()

        // Act
        val next = marking.tapCell(emptyCellAnswer4).tapCell(emptyCellAnswer6)

        // Assert
        assertEquals(setOf(emptyCellAnswer4, emptyCellAnswer6), next.marked)
        assertEquals(marking.values, next.values)
    }

    @Test
    fun `given an undone move when redo is used then the move is back, and the notes it cleared are cleared again`() {
        // Arrange
        val placed = game.tapCell(emptyCellAnswer4).tapDigit(4)

        // Act
        val redone = placed.undo().redo()

        // Assert
        assertEquals(placed.values to placed.notes, redone.values to redone.notes)
        assertEquals(placed.undoStack.size, redone.undoStack.size)
    }

    @Test
    fun `given an undone move when a new move is made then there is nothing to redo`() {
        // Arrange
        val undone = game.tapCell(emptyCellAnswer4).tapDigit(4).undo()

        // Act
        val next = undone.tapCell(emptyCellAnswer6).tapDigit(6)

        // Assert
        assertEquals(next, next.redo())
    }

    @Test
    fun `given a placed digit when undo is used then the cell is empty again`() {
        // Arrange
        val placed = game.tapCell(emptyCellAnswer4).tapDigit(4)

        // Act
        val next = placed.undo()

        // Assert
        assertEquals(0, next.values[emptyCellAnswer4])
    }

    @Test
    fun `given 500 moves when undo is used 500 times then the board is back to the start`() {
        // Arrange
        var state = game.toggleNotesMode().tapCell(emptyCellAnswer4)
        repeat(500) { state = state.tapDigit(it % 9 + 1) }

        // Act
        repeat(500) { state = state.undo() }

        // Assert
        assertEquals(game.notes to emptyList(), state.notes to state.undoStack)
    }

    @Test
    fun `given a placement that cleared a pencil mark when undo is used then the pencil mark is back`() {
        // Arrange
        val placed =
            game
                .toggleNotesMode()
                .tapCell(emptyCellAnswer6)
                .tapDigit(4)
                .toggleNotesMode()
                .tapCell(emptyCellAnswer4)
                .tapDigit(4)

        // Act
        val next = placed.undo()

        // Assert
        assertEquals(digitBit(4), next.notes[emptyCellAnswer6])
    }

    @Test
    fun `given a move history when it is encoded and decoded then it is unchanged`() {
        // Arrange
        val played =
            game
                .tapCell(emptyCellAnswer4)
                .tapDigit(4)
                .toggleNotesMode()
                .tapCell(emptyCellAnswer6)
                .tapDigit(6)

        // Act
        val decoded = MoveCodec.decode(MoveCodec.encode(played.undoStack))

        // Assert
        assertEquals(played.undoStack, decoded)
    }

    @ParameterizedTest
    @MethodSource("highlightCases")
    fun `given the top-left given is selected when highlights are worked out then each cell gets the expected one`(
        options: PlayOptions,
        cell: Int,
        expected: Highlight,
    ) {
        // Arrange
        val selected = game.copy(options = options).tapCell(given5)

        // Act
        val highlight = selected.highlight(cell)

        // Assert
        assertEquals(expected, highlight)
    }

    @Test
    fun `given an active digit in digit-first when highlights are worked out then cells with it are highlighted`() {
        // Arrange
        val active = game.withInputMode(InputMode.DIGIT_FIRST).tapDigit(8)

        // Act
        val highlight = active.highlight(Grid.STANDARD.cellAt(2, 2))

        // Assert
        assertEquals(Highlight.SAME_DIGIT, highlight)
    }

    @Test
    fun `given one empty cell left when its answer is entered then the game is complete`() {
        // Arrange
        val nearlyDone = game.copy(values = easyPuzzle.solution!!.toMutableList().also { it[emptyCellAnswer4] = 0 })

        // Act
        val next = nearlyDone.tapCell(emptyCellAnswer4).tapDigit(4)

        // Assert
        assertTrue(next.isComplete)
    }

    @ParameterizedTest
    @CsvSource("3, true", "4, false")
    fun `given a two-cell cage when both cells are filled then a wrong total is flagged`(
        firstDigit: Int,
        expectedWrong: Boolean,
    ) {
        // Arrange
        val cage = Cage(listOf(emptyCellAnswer4, emptyCellAnswer6), sum = 10)
        val killer = GameState(easyPuzzle.copy(cages = listOf(cage)))

        // Act
        val next =
            killer
                .tapCell(emptyCellAnswer4)
                .tapDigit(firstDigit)
                .tapCell(emptyCellAnswer6)
                .tapDigit(6)

        // Assert
        assertEquals(expectedWrong, next.isCageWrong(cage))
    }

    @ParameterizedTest
    @CsvSource("false, 2", "true, 3")
    fun `given the board when a digit is counted then givens and entries both count`(
        placeAnother4: Boolean,
        expectedCount: Int,
    ) {
        // Arrange
        val board =
            if (placeAnother4) {
                game.tapCell(emptyCellAnswer4).tapDigit(4)
            } else {
                game
            }

        // Act
        val count = board.digitCount(4)

        // Assert
        assertEquals(expectedCount, count)
    }

    @Test
    fun `given number-first when erase is tapped then it stays selected and the active digit is dropped`() {
        // Arrange
        val active = game.withInputMode(InputMode.DIGIT_FIRST).tapDigit(4)

        // Act
        val next = active.erase()

        // Assert
        assertEquals(true to null, next.eraserActive to next.activeDigit)
    }

    @Test
    fun `given the eraser in number-first when filled cells are tapped then each is cleared and givens are kept`() {
        // Arrange
        val filled =
            game
                .withInputMode(InputMode.DIGIT_FIRST)
                .tapDigit(4)
                .tapCell(emptyCellAnswer4)
                .tapDigit(6)
                .tapCell(emptyCellAnswer6)
        val erasing = filled.erase()

        // Act
        val next = erasing.tapCell(emptyCellAnswer4).tapCell(emptyCellAnswer6).tapCell(given5)

        // Assert
        assertEquals(listOf(0, 0, 5), listOf(next.values[emptyCellAnswer4], next.values[emptyCellAnswer6], next.values[given5]))
    }

    @ParameterizedTest
    @CsvSource("erase, false", "digit, false")
    fun `given the eraser in number-first when erase or a digit is tapped then erasing stops`(
        action: String,
        expected: Boolean,
    ) {
        // Arrange
        val erasing = game.withInputMode(InputMode.DIGIT_FIRST).erase()

        // Act
        val next =
            if (action == "erase") {
                erasing.erase()
            } else {
                erasing.tapDigit(3)
            }

        // Assert
        assertEquals(expected, next.eraserActive)
    }

    @Test
    fun `given cell-first and a selected filled cell when erase is tapped then that cell is cleared`() {
        // Arrange
        val filled = game.tapCell(emptyCellAnswer4).tapDigit(4)

        // Act
        val next = filled.erase()

        // Assert
        assertEquals(0 to false, next.values[emptyCellAnswer4] to next.eraserActive)
    }

    @ParameterizedTest
    @CsvSource("place, true", "undo, false", "clear, false")
    fun `given a placement when the next action happens then the last placed cell is tracked`(
        action: String,
        expectTracked: Boolean,
    ) {
        // Arrange
        val placed = game.tapCell(emptyCellAnswer4).tapDigit(4)

        // Act
        val next =
            when (action) {
                "undo" -> placed.undo()
                "clear" -> placed.tapDigit(4)
                else -> placed
            }

        // Assert
        val expected =
            if (expectTracked) {
                emptyCellAnswer4
            } else {
                null
            }
        assertEquals(expected, next.lastPlaced)
    }

    @ParameterizedTest
    @CsvSource("true, SELECTED, PEER", "false, SAME_DIGIT, NONE")
    fun `given a just filled cell when highlights are worked out then the selection shows only if last placed highlighting is on`(
        highlightLastPlaced: Boolean,
        expectedCell: Highlight,
        expectedPeer: Highlight,
    ) {
        // Arrange
        val placed = game.copy(options = PlayOptions(highlightLastPlaced = highlightLastPlaced)).tapCell(emptyCellAnswer4).tapDigit(4)

        // Act
        val highlights = placed.highlight(emptyCellAnswer4) to placed.highlight(Grid.STANDARD.cellAt(8, 2))

        // Assert
        assertEquals(expectedCell to expectedPeer, highlights)
    }

    @Test
    fun `given just-filled highlighting off when a pencil mark is added then the cell is not highlighted as selected`() {
        // Arrange
        val noting = game.copy(options = PlayOptions(highlightLastPlaced = false)).toggleNotesMode().tapCell(emptyCellAnswer4)

        // Act
        val next = noting.tapDigit(2)

        // Assert
        assertEquals(Highlight.NONE, next.highlight(emptyCellAnswer4))
    }

    @ParameterizedTest
    @CsvSource("CELL, NOTE_MATCH, ", "NUMBER, PEER, 5", "OFF, PEER, ")
    fun `given a pencil mark matching the selected number when highlights are worked out then the style decides how it shows`(
        style: HighlightStyle,
        expectedCell: Highlight,
        expectedNoteDigit: Int?,
    ) {
        // Arrange
        val noted =
            game
                .copy(options = PlayOptions(notesStyle = style))
                .toggleNotesMode()
                .tapCell(emptyCellAnswer6)
                .tapDigit(5)
                .toggleNotesMode()

        // Act
        val selected = noted.tapCell(given5)

        // Assert
        assertEquals(expectedCell to expectedNoteDigit, selected.highlight(emptyCellAnswer6) to selected.noteHighlightDigit)
    }

    @ParameterizedTest
    @CsvSource("CELL, false", "NUMBER, true", "OFF, false")
    fun `given the same number elsewhere when the style is number then the digit itself is highlighted`(
        style: HighlightStyle,
        expected: Boolean,
    ) {
        // Arrange
        val options = PlayOptions(sameNumberStyle = style)

        // Act
        val selected = game.copy(options = options).tapCell(given5)

        // Assert
        assertEquals(expected, selected.isNumberHighlighted(Grid.STANDARD.cellAt(1, 5)))
    }

    @Test
    fun `given a game with entries and notes when it is restarted then only the givens are left`() {
        // Arrange
        val played =
            game
                .tapCell(emptyCellAnswer4)
                .tapDigit(9)
                .toggleNotesMode()
                .tapCell(emptyCellAnswer6)
                .tapDigit(2)

        // Act
        val restarted = played.restart()

        // Assert
        assertEquals(
            listOf(easyPuzzle.givens, List(Grid.STANDARD.cells) { 0 }, emptyList<Any>(), listOf(0)),
            listOf(restarted.values, restarted.notes, restarted.undoStack, listOf(restarted.mistakes)),
        )
    }

    @Test
    fun `given a puzzle with a known answer when the solution is revealed then the board is the answer`() {
        // Act
        val revealed = game.revealSolution()

        // Assert
        assertEquals(easyPuzzle.solution to true, revealed.values to revealed.isComplete)
    }

    @Test
    fun `given a puzzle without a known answer when the solution is revealed then nothing changes`() {
        // Arrange
        val unverified = GameState(easyPuzzle.copy(solution = null))

        // Act
        val revealed = unverified.revealSolution()

        // Assert
        assertEquals(unverified, revealed)
    }

    companion object {
        @JvmStatic
        fun cellsToTapWithActiveDigit() =
            listOf(
                Arguments.of(Grid.STANDARD.cellAt(0, 0)),
                Arguments.of(Grid.STANDARD.cellAt(0, 2)),
                Arguments.of(Grid.STANDARD.cellAt(0, 3)),
            )

        @JvmStatic
        fun highlightCases() =
            listOf(
                Arguments.of(PlayOptions(), Grid.STANDARD.cellAt(0, 0), Highlight.SELECTED),
                Arguments.of(PlayOptions(), Grid.STANDARD.cellAt(0, 1), Highlight.PEER),
                Arguments.of(PlayOptions(), Grid.STANDARD.cellAt(1, 5), Highlight.SAME_DIGIT),
                Arguments.of(PlayOptions(), Grid.STANDARD.cellAt(4, 3), Highlight.NONE),
                Arguments.of(PlayOptions(highlightHouses = false), Grid.STANDARD.cellAt(0, 1), Highlight.NONE),
                Arguments.of(PlayOptions(sameNumberStyle = HighlightStyle.OFF), Grid.STANDARD.cellAt(1, 5), Highlight.NONE),
                Arguments.of(PlayOptions(sameNumberStyle = HighlightStyle.NUMBER), Grid.STANDARD.cellAt(1, 5), Highlight.NONE),
            )
    }
}
