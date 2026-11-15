package com.joclegg.puzzles.crossword.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.joclegg.puzzles.crossword.engine.Arrow

sealed interface BoardInput {
    data class Letter(
        val letter: Char,
    ) : BoardInput

    data object Backspace : BoardInput

    data object ToggleDirection : BoardInput

    data object NextClue : BoardInput

    data object PreviousClue : BoardInput

    data class Move(
        val arrow: Arrow,
    ) : BoardInput
}

const val SENTINEL = " "

fun sentinelInputs(text: String): List<BoardInput> {
    if (text.isEmpty()) {
        return listOf(BoardInput.Backspace)
    }
    val typed =
        if (text.startsWith(SENTINEL)) {
            text.drop(SENTINEL.length)
        } else {
            text
        }
    return typed.mapNotNull { char ->
        val upper = char.uppercaseChar()
        when {
            upper in 'A'..'Z' -> BoardInput.Letter(upper)
            char == ' ' -> BoardInput.ToggleDirection
            else -> null
        }
    }
}

fun hardwareInput(
    key: Key,
    shift: Boolean,
    codePoint: Int,
): BoardInput? =
    when (key) {
        Key.Backspace, Key.Delete -> {
            BoardInput.Backspace
        }

        Key.DirectionLeft -> {
            BoardInput.Move(Arrow.LEFT)
        }

        Key.DirectionRight -> {
            BoardInput.Move(Arrow.RIGHT)
        }

        Key.DirectionUp -> {
            BoardInput.Move(Arrow.UP)
        }

        Key.DirectionDown -> {
            BoardInput.Move(Arrow.DOWN)
        }

        Key.Spacebar -> {
            BoardInput.ToggleDirection
        }

        Key.Tab -> {
            if (shift) {
                BoardInput.PreviousClue
            } else {
                BoardInput.NextClue
            }
        }

        else -> {
            val upper = codePoint.toChar().uppercaseChar()
            if (upper in 'A'..'Z') {
                BoardInput.Letter(upper)
            } else {
                null
            }
        }
    }

private class SentinelTransformation(
    private val onInput: () -> (BoardInput) -> Unit,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence().toString()
        if (text == SENTINEL) {
            placeCursorAtEnd()
            return
        }
        replace(0, length, SENTINEL)
        placeCursorAtEnd()
        sentinelInputs(text).forEach(onInput())
    }
}

@Composable
fun CrosswordInput(
    onInput: (BoardInput) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val currentOnInput by rememberUpdatedState(onInput)
    val state = rememberTextFieldState(SENTINEL)
    val transformation = remember { SentinelTransformation { currentOnInput } }
    BasicTextField(
        state = state,
        inputTransformation = transformation,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.None,
            ),
        modifier =
            modifier
                .alpha(0f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        val handled = hardwareInput(event.key, event.isShiftPressed, event.utf16CodePoint) != null
                        return@onPreviewKeyEvent handled
                    }
                    val input = hardwareInput(event.key, event.isShiftPressed, event.utf16CodePoint)
                    if (input == null) {
                        false
                    } else {
                        currentOnInput(input)
                        true
                    }
                }.testTag(INPUT_TAG),
    )
}
