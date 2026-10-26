package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.kakuro.engine.MAX_DIGIT

data class KeypadTool(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val tag: String,
    val on: Boolean = false,
)

@Composable
fun NumberPad(
    onDigitClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeDigit: Int? = null,
    doneDigits: Set<Int> = emptySet(),
    tools: List<KeypadTool> = emptyList(),
) {
    val keys: List<Any> = (1..MAX_DIGIT).toList() + tools
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        keys.chunked(KEYS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                row.forEach { key ->
                    val keyModifier = Modifier.weight(1f).aspectRatio(1f)
                    when (key) {
                        is Int -> DigitKey(key, onDigitClick, key == activeDigit, key in doneDigits, keyModifier)
                        is KeypadTool -> PadToolKey(key, keyModifier)
                    }
                }
                repeat(KEYS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val KEYS_PER_ROW = 6
private val KEY_GAP = 6.dp
private val KEY_SHAPE = RoundedCornerShape(8.dp)

@Composable
private fun DigitKey(
    digit: Int,
    onDigitClick: (Int) -> Unit,
    active: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    val buttonModifier =
        Modifier
            .fillMaxSize()
            .testTag(digitTag(digit))
            .semantics { selected = active }
    val label: @Composable () -> Unit = { Text(digit.toString(), fontSize = 36.sp) }
    Box(modifier) {
        if (active && !done) {
            FilledTonalButton(
                onClick = { onDigitClick(digit) },
                modifier = buttonModifier,
                shape = KEY_SHAPE,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = LocalKakuroColors.current.selected),
            ) { label() }
        } else {
            OutlinedButton(
                onClick = { onDigitClick(digit) },
                modifier = buttonModifier,
                enabled = !done,
                shape = KEY_SHAPE,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) { label() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PadToolKey(
    tool: KeypadTool,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(tool.description) } },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = tool.onClick,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag(tool.tag)
                        .semantics { selected = tool.on },
                shape = KEY_SHAPE,
                contentPadding = PaddingValues(0.dp),
                colors =
                    if (tool.on) {
                        ButtonDefaults.filledTonalButtonColors(containerColor = LocalKakuroColors.current.selected)
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
            ) { Icon(tool.icon, contentDescription = tool.description, modifier = Modifier.size(26.dp)) }
        }
    }
}
