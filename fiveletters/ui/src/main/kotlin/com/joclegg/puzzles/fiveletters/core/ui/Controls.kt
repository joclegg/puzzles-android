package com.joclegg.puzzles.fiveletters.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
            Icon(icon, contentDescription = description)
        }
    }
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.testTag("back")) {
        Icon(FiveLettersIcons.Back, contentDescription = "Back")
    }
}
