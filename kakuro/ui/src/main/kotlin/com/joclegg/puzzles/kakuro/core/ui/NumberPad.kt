package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.kakuro.engine.MAX_DIGIT

@Composable
fun NumberPad(
    onDigitClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeDigit: Int? = null,
    doneDigits: Set<Int> = emptySet(),
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..MAX_DIGIT).forEach { digit ->
            val buttonModifier =
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag(digitTag(digit))
                    .semantics { selected = digit == activeDigit }
            if (digit == activeDigit) {
                Button(onClick = { onDigitClick(digit) }, contentPadding = PaddingValues(0.dp), modifier = buttonModifier) {
                    Text(digit.toString(), fontSize = 22.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { onDigitClick(digit) },
                    enabled = digit !in doneDigits,
                    contentPadding = PaddingValues(0.dp),
                    modifier = buttonModifier,
                ) { Text(digit.toString(), fontSize = 22.sp) }
            }
        }
    }
}
