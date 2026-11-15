package com.joclegg.puzzles.crossword.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.crossword.engine.Arrow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrosswordInputTest {
    @get:Rule
    val compose = createComposeRule()

    private val received = mutableListOf<BoardInput>()

    private fun show() {
        compose.setContent {
            val focus = remember { FocusRequester() }
            CrosswordInput(onInput = { received += it }, focusRequester = focus)
            LaunchedEffect(focus) { focus.requestFocus() }
        }
    }

    @Test
    fun `given the input field when the keyboard commits letters then each is a letter and the field keeps its sentinel`() {
        // Arrange
        show()

        // Act
        compose.onNodeWithTag(INPUT_TAG).performTextInput("c")
        compose.onNodeWithTag(INPUT_TAG).performTextInput("At")

        // Assert
        assertEquals(listOf('C', 'A', 'T').map(BoardInput::Letter), received)
        compose.onNodeWithTag(INPUT_TAG).assertTextEquals(SENTINEL)
    }

    @Test
    fun `given the input field when the keyboard deletes twice then each delete is one backspace`() {
        // Arrange
        show()

        // Act
        compose.onNodeWithTag(INPUT_TAG).performTextClearance()
        compose.onNodeWithTag(INPUT_TAG).performTextClearance()

        // Assert
        assertEquals(listOf(BoardInput.Backspace, BoardInput.Backspace), received)
        compose.onNodeWithTag(INPUT_TAG).assertTextEquals(SENTINEL)
    }

    @Test
    fun `given the input field when digits and punctuation are committed then they are ignored`() {
        // Arrange
        show()

        // Act
        compose.onNodeWithTag(INPUT_TAG).performTextInput("7-!")

        // Assert
        assertEquals(emptyList(), received)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `given a hardware keyboard when backspace, arrows and shift tab are pressed then each maps to its input`() {
        // Arrange
        show()

        // Act
        compose.onNodeWithTag(INPUT_TAG).performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.DirectionDown)
            withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
        }

        // Assert
        assertEquals(listOf(BoardInput.Backspace, BoardInput.Move(Arrow.DOWN), BoardInput.PreviousClue), received)
    }
}
