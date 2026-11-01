package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BankDownloadScreen(
    failed: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize().testTag("bank_download")) {
        Box(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).testTag("back")) {
                Text("←")
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (failed) {
                    Text(stringResource(R.string.banks_failed), textAlign = TextAlign.Center)
                    Button(onClick = onRetry, modifier = Modifier.testTag("banks_retry")) { Text(stringResource(R.string.banks_retry)) }
                } else {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.banks_downloading))
                }
            }
        }
    }
}
