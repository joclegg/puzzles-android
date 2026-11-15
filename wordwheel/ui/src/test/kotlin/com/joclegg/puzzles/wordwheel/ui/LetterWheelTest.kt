package com.joclegg.puzzles.wordwheel.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LetterWheelTest {
    @get:Rule
    val compose = createComposeRule()

    private var chosen by mutableStateOf(emptyList<Int>())
    private val changes = mutableListOf<List<Int>>()
    private var submitted = 0

    private fun show(applyChanges: Boolean = true) {
        compose.setContent {
            WordWheelTheme(darkTheme = false) {
                LetterWheel(
                    "ABCDEF",
                    chosen,
                    onChosenChange = {
                        changes += it
                        if (applyChanges) {
                            chosen = it
                        }
                    },
                    onSwipeEnd = { submitted++ },
                    Modifier.size(260.dp).testTag(WHEEL),
                )
            }
        }
    }

    private fun centre(index: Int): Offset {
        val wheel = compose.onNodeWithTag(WHEEL).fetchSemanticsNode().boundsInRoot
        return compose
            .onNodeWithTag(wheelTag(index))
            .fetchSemanticsNode()
            .boundsInRoot.center - wheel.topLeft
    }

    @Test
    fun `given a finger on a letter when it jumps straight to the next letter then both letters are chosen`() {
        // Arrange
        show()
        val first = centre(0)
        val second = centre(1)

        // Act
        compose.onNodeWithTag(WHEEL).performTouchInput {
            down(first)
            moveTo(second)
            up()
        }

        // Assert
        assertEquals(listOf(0, 1), chosen)
        assertEquals(1, submitted)
    }

    @Test
    fun `given the chosen letters are slow to update when swiping across three letters then no letter is lost`() {
        // Arrange
        show(applyChanges = false)
        val first = centre(0)
        val second = centre(1)
        val third = centre(2)

        // Act
        compose.onNodeWithTag(WHEEL).performTouchInput {
            down(first)
            moveTo(first + (second - first) / 4f)
            moveTo(second)
            moveTo(third)
            up()
        }

        // Assert
        assertEquals(listOf(0, 1, 2), changes.last())
        assertEquals(1, submitted)
    }

    @Test
    fun `given a built word when a swipe starts on another letter then the swipe replaces the word`() {
        // Arrange
        chosen = listOf(4, 5)
        show()
        val first = centre(0)
        val second = centre(1)

        // Act
        compose.onNodeWithTag(WHEEL).performTouchInput {
            down(first)
            moveTo(second)
            up()
        }

        // Assert
        assertEquals(listOf(0, 1), chosen)
    }

    @Test
    fun `given a built word when a letter is tapped with a slight drift then it joins the word without submitting`() {
        // Arrange
        chosen = listOf(4, 5)
        show()
        val first = centre(0)

        // Act
        compose.onNodeWithTag(WHEEL).performTouchInput {
            down(first)
            moveTo(first + Offset(DRIFT_PX, 0f))
            up()
        }

        // Assert
        assertEquals(listOf(4, 5, 0), chosen)
        assertEquals(0, submitted)
    }

    @Test
    fun `given an empty word when a letter is tapped then it is chosen without submitting`() {
        // Arrange
        show()
        val first = centre(0)

        // Act
        compose.onNodeWithTag(WHEEL).performTouchInput {
            down(first)
            up()
        }

        // Assert
        assertEquals(listOf(0), chosen)
        assertEquals(0, submitted)
    }

    private companion object {
        const val WHEEL = "wheel"
        const val DRIFT_PX = 20f
    }
}
