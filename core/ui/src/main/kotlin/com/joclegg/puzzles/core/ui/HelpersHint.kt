package com.joclegg.puzzles.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun HelpersHint(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier.testTag("customise_helpers")) {
        Icon(MenuIcons.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.customise_helpers),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

const val HELPERS_NOTE = "Helpers such as hints, highlights and mistake checks start off. Turn on the ones you like below."
