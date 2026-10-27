package com.joclegg.puzzles.fiveletters.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

data class TourStep(
    val target: String,
    val text: String,
)

@Stable
class TourState {
    val bounds = mutableStateMapOf<String, Rect>()
}

@Composable
fun rememberTourState(): TourState = remember { TourState() }

fun Modifier.tourTarget(
    tour: TourState,
    key: String,
): Modifier = onGloballyPositioned { tour.bounds[key] = it.boundsInWindow() }

@Composable
fun HintButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.testTag("hint")) {
        Icon(HelpIcons.Hint, contentDescription = "Show a hint")
    }
}

@Composable
fun HowToPlayButton(
    rules: List<String>,
    steps: List<TourStep>,
    tour: TourState,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier.testTag("rules")) {
        Icon(HelpIcons.Rules, contentDescription = "Show the rules")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            modifier = Modifier.dialogTestTags(),
            title = { Text("How to play") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rules.forEach { Text(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }, modifier = Modifier.testTag("rules_close")) { Text("Got it") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        open = false
                        walking = true
                    },
                    modifier = Modifier.testTag("walkthrough_start"),
                ) { Text("Walk me through it") }
            },
        )
    }
    if (walking) {
        Walkthrough(steps, tour, onClose = { walking = false })
    }
}

@Composable
private fun Walkthrough(
    steps: List<TourStep>,
    tour: TourState,
    onClose: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val last = index == steps.lastIndex
    val highlight = MaterialTheme.colorScheme.primary
    Popup(popupPositionProvider = WindowOrigin, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        Box(Modifier.fillMaxSize().dialogTestTags()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.Black.copy(alpha = 0.4f))
                tour.bounds[step.target]?.let { target ->
                    drawRoundRect(
                        color = highlight,
                        topLeft = Offset(target.left - PAD, target.top - PAD),
                        size = Size(target.width + PAD * 2, target.height + PAD * 2),
                        cornerRadius = CornerRadius(PAD * 2),
                        style = Stroke(width = PAD),
                    )
                }
            }
            Card(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("walkthrough"),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Step ${index + 1} of ${steps.size}", style = MaterialTheme.typography.labelSmall)
                    Text(step.text, modifier = Modifier.testTag("walkthrough_text"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClose, modifier = Modifier.testTag("walkthrough_close")) {
                            Text(
                                if (last) {
                                    "Close"
                                } else {
                                    "Skip"
                                },
                            )
                        }
                        if (index > 0) {
                            TextButton(onClick = { index-- }, modifier = Modifier.testTag("walkthrough_back")) { Text("Back") }
                        }
                        if (!last) {
                            TextButton(onClick = { index++ }, modifier = Modifier.testTag("walkthrough_next")) { Text("Next") }
                        }
                    }
                }
            }
        }
    }
}

private const val PAD = 12f

private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

private object HelpIcons {
    val Hint = outline("help_hint", "M9 18h6", "M10 21h4", "M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z")
    val Rules =
        outline("help_rules", "M12 21a9 9 0 1 0 0-18a9 9 0 1 0 0 18z", "M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6", "M12 17h.01")

    private fun outline(
        name: String,
        vararg paths: String,
    ): ImageVector =
        ImageVector
            .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                for (path in paths) {
                    addPath(
                        pathData = addPathNodes(path),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()
}
