package com.joclegg.puzzles.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MenuTile(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
    active: Boolean = false,
    done: Boolean = false,
    enabled: Boolean = true,
    meter: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = tileColor(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = tileColor(),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            ),
        border = tileBorder(accent, highlighted = active || done),
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Box(
                    Modifier.size(40.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                meter?.invoke()
                if (detail != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (active) {
                            Box(Modifier.size(6.dp).background(accent, CircleShape))
                        }
                        Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (trailing != null) {
                trailing()
            } else if (done) {
                DoneBadge(accent, size = 24)
            } else {
                Icon(MenuIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DailyTile(
    label: String,
    done: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource)
    val container =
        if (done) {
            accent.copy(alpha = 0.16f)
        } else {
            tileColor()
        }
    Card(
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = MaterialTheme.colorScheme.onSurface),
        border = tileBorder(accent, highlighted = done),
        interactionSource = interactionSource,
    ) {
        Column(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (done) {
                DoneBadge(accent, size = 18)
            } else {
                Text("Play", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = pressScale(interactionSource)
    Card(
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tileColor(), contentColor = accent),
        border = tileBorder(accent, highlighted = false),
        interactionSource = interactionSource,
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = accent)
        }
    }
}

@Composable
fun LevelMeter(
    rank: Int,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val colour =
                if (index <= rank) {
                    accent
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                }
            Box(Modifier.size(width = 12.dp, height = 6.dp).background(colour, CircleShape))
        }
    }
}

@Composable
fun TileGrid(
    count: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    tile: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until count).chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { index -> tile(index, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DoneBadge(
    accent: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            MenuIcons.Check,
            contentDescription = "Done",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size((size * 2 / 3).dp),
        )
    }
}
