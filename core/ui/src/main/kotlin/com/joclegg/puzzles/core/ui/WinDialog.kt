package com.joclegg.puzzles.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class WinStat(
    val label: String,
    val value: String,
    val tag: String? = null,
)

data class DailyLeft(
    val label: String,
    val accent: Color,
    val left: Int,
    val onClick: () -> Unit,
)

val LocalDailiesLeft = staticCompositionLocalOf<suspend () -> List<DailyLeft>> { { emptyList() } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WinDialog(
    title: String,
    accent: Color,
    newBest: Boolean,
    stats: List<WinStat>,
    modifier: Modifier = Modifier,
    message: String? = null,
    titleTag: String? = null,
    lost: Boolean = false,
    onDismissRequest: () -> Unit = {},
    buttons: @Composable () -> Unit,
) {
    val loadDailies = LocalDailiesLeft.current
    val dailies by produceState(emptyList<DailyLeft>()) { value = loadDailies() }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier =
                modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
        ) {
            Column {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(accent.copy(alpha = 0.3f), accent.copy(alpha = 0.08f), Color.Transparent)),
                        )
                        .padding(top = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .scale(pop.value)
                            .size(64.dp)
                            .background(accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (lost) {
                                MenuIcons.Book
                            } else {
                                MenuIcons.Trophy
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                                .then(titleTag?.let { Modifier.testTag(it) } ?: Modifier),
                    )
                    if (newBest) {
                        Text(
                            "NEW BEST",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            modifier =
                                Modifier
                                    .padding(top = 6.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .testTag("new_best"),
                        )
                    }
                }
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                if (stats.isNotEmpty()) {
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.forEach { stat -> StatBox(stat, accent, Modifier.weight(1f)) }
                    }
                }
                if (dailies.isNotEmpty()) {
                    Text(
                        "STILL TO PLAY TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                    FlowRow(
                        Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dailies.forEach { daily -> DailyChip(daily) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    buttons()
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    stat: WinStat,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stat.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stat.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = stat.tag?.let { Modifier.testTag(it) } ?: Modifier,
        )
    }
}

@Composable
private fun DailyChip(
    daily: DailyLeft,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(daily.accent.copy(alpha = 0.15f))
            .border(1.dp, daily.accent.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = daily.onClick)
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(daily.label, style = MaterialTheme.typography.labelLarge)
        Text(
            "${daily.left}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(daily.accent)
                    .padding(horizontal = 6.dp),
        )
        Icon(MenuIcons.Chevron, contentDescription = null, tint = daily.accent, modifier = Modifier.size(16.dp))
    }
}
