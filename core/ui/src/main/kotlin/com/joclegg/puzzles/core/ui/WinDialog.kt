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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val onClick: () -> Unit,
)

private val TILE_ROW_MAX_HEIGHT = 216.dp
private val TILE_MAX_WIDTH = 120.dp
private const val TILES_PER_ROW = 3

val LocalDailiesLeft = staticCompositionLocalOf<suspend (String) -> List<DailyLeft>> { { emptyList() } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WinDialog(
    title: String,
    accent: Color,
    dailyGroup: String,
    newBest: Boolean,
    stats: List<WinStat>,
    modifier: Modifier = Modifier,
    message: String? = null,
    titleTag: String? = null,
    lost: Boolean = false,
    newLabel: String? = null,
    newTag: String = "win_new",
    onNew: (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    buttons: @Composable () -> Unit,
) {
    val loadDailies = LocalDailiesLeft.current
    val dailies by produceState(emptyList<DailyLeft>()) { value = loadDailies(dailyGroup) }
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
                        ).padding(top = 24.dp, bottom = 12.dp),
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
                if (dailies.isNotEmpty() || onNew != null) {
                    Text(
                        "PLAY NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .heightIn(max = TILE_ROW_MAX_HEIGHT)
                            .verticalScroll(rememberScrollState()),
                        maxItemsInEachRow = TILES_PER_ROW,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dailies.forEach { daily ->
                            WinTile("Daily", daily.label, accent, daily.onClick, Modifier.weight(1f).widthIn(max = TILE_MAX_WIDTH))
                        }
                        if (onNew != null) {
                            WinTile("New", newLabel.orEmpty(), accent, onNew, Modifier.weight(1f).widthIn(max = TILE_MAX_WIDTH).testTag(newTag))
                        }
                        repeat((TILES_PER_ROW - (dailies.size + listOfNotNull(onNew).size) % TILES_PER_ROW) % TILES_PER_ROW) {
                            Spacer(Modifier.weight(1f))
                        }
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
private fun WinTile(
    caption: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
