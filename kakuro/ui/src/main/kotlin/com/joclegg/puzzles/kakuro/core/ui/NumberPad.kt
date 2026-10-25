package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.kakuro.engine.MAX_DIGIT

@Composable
fun NumberPad(
    onDigitClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..MAX_DIGIT).forEach { digit ->
            OutlinedButton(
                onClick = { onDigitClick(digit) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.weight(1f).height(56.dp).testTag(digitTag(digit)),
            ) { Text(digit.toString(), fontSize = 22.sp) }
        }
    }
}
