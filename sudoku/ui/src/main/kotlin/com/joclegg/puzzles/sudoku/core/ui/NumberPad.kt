package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.sudoku.engine.Grid

@Composable
fun NumberPad(
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeDigit: Int? = null,
    doneDigits: Set<Int> = emptySet(),
    counts: List<Int> = emptyList(),
    digits: IntRange = Grid.STANDARD.digits,
    tools: List<KeypadTool> = emptyList(),
) {
    val keys: List<Any> = digits.toList() + tools
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        keys.chunked(KEYS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                row.forEach { key ->
                    val keyModifier = Modifier.weight(1f).aspectRatio(1f)
                    when (key) {
                        is Int -> {
                            val fontSize =
                                if (digits.last == Grid.STANDARD.size) {
                                    STANDARD_KEY_FONT
                                } else {
                                    FAST_KEY_FONT
                                }
                            DigitKey(key, onDigit, activeDigit, doneDigits, counts, fontSize, keyModifier)
                        }

                        is KeypadTool -> {
                            ToolButton(key.icon, key.description, key.onClick, key.tag, keyModifier, key.on)
                        }
                    }
                }
                repeat(KEYS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

data class KeypadTool(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val tag: String,
    val on: Boolean = false,
)

private const val KEYS_PER_ROW = 6
private val STANDARD_KEY_FONT = 40.sp
private val FAST_KEY_FONT = 34.sp
private val KEY_GAP = 6.dp

@Composable
private fun DigitKey(
    digit: Int,
    onDigit: (Int) -> Unit,
    activeDigit: Int?,
    doneDigits: Set<Int>,
    counts: List<Int>,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    val active = digit == activeDigit
    val done = digit in doneDigits
    val count = counts.getOrNull(digit)
    val buttonModifier =
        Modifier
            .fillMaxSize()
            .testTag(digitTag(digit))
            .semantics {
                selected = active
                if (done) {
                    stateDescription = "done"
                }
            }
    Box(modifier) {
        val shape = RoundedCornerShape(8.dp)
        val padding = PaddingValues(0.dp)
        val label: @Composable () -> Unit = {
            Text(
                digit.toString(),
                fontSize = fontSize,
                color =
                    when {
                        done -> colors.doneDigit
                        active -> colors.enteredDigit
                        else -> MaterialTheme.colorScheme.primary
                    },
            )
        }
        if (active && !done) {
            FilledTonalButton(
                onClick = { onDigit(digit) },
                modifier = buttonModifier,
                shape = shape,
                contentPadding = padding,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.selected),
            ) { label() }
        } else {
            OutlinedButton(
                onClick = { onDigit(digit) },
                modifier = buttonModifier,
                enabled = !done,
                shape = shape,
                contentPadding = padding,
                border =
                    if (done) {
                        BorderStroke(1.dp, colors.doneDigit)
                    } else {
                        ButtonDefaults.outlinedButtonBorder()
                    },
            ) { label() }
        }
        if (count != null && !done) {
            Text(
                count.toString(),
                fontSize = 11.sp,
                color = colors.noteDigit,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp).testTag("${digitTag(digit)}_count"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    on: Boolean = false,
) {
    val colors = LocalBoardColors.current

    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(description) } },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = onClick,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag(tag)
                        .semantics { selected = on },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors =
                    if (on) {
                        ButtonDefaults.filledTonalButtonColors(containerColor = colors.selected)
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
            ) { Icon(icon, contentDescription = description, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.testTag("back")) {
        Icon(SudokuIcons.Back, contentDescription = "Back")
    }
}
