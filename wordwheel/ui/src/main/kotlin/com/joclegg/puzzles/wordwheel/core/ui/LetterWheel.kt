package com.joclegg.puzzles.wordwheel.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LetterWheel(
    letters: String,
    chosen: List<Int>,
    onChosenChange: (List<Int>) -> Unit,
    onSwipeEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWheelColors.current
    val currentChosen by rememberUpdatedState(chosen)
    val currentOnChange by rememberUpdatedState(onChosenChange)
    val currentOnEnd by rememberUpdatedState(onSwipeEnd)
    var finger by remember { mutableStateOf<Offset?>(null) }
    BoxWithConstraints(modifier.aspectRatio(1f).background(colors.wheel, CircleShape)) {
        val sizePx = with(LocalDensity.current) { maxWidth.toPx() }
        val letterSize = maxWidth * LETTER_FRACTION
        val letterPx = with(LocalDensity.current) { letterSize.toPx() }
        val radius = (sizePx - letterPx) / 2 - with(LocalDensity.current) { 8.dp.toPx() }
        val centres =
            letters.indices.map { index ->
                val angle = 2 * PI * index / letters.length - PI / 2
                Offset((sizePx / 2 + radius * cos(angle)).toFloat(), (sizePx / 2 + radius * sin(angle)).toFloat())
            }

        fun letterAt(point: Offset) = centres.indexOfFirst { hypot(it.x - point.x, it.y - point.y) < letterPx / 2 }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(letters) {
                    detectTapGestures { point ->
                        val index = letterAt(point)
                        if (index >= 0 && index !in currentChosen) {
                            currentOnChange(currentChosen + index)
                        }
                    }
                }.pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { start ->
                            finger = start
                            val index = letterAt(start)
                            currentOnChange(
                                if (index >= 0) {
                                    listOf(index)
                                } else {
                                    emptyList()
                                },
                            )
                        },
                        onDragEnd = {
                            finger = null
                            currentOnEnd()
                        },
                        onDragCancel = { finger = null },
                    ) { change, _ ->
                        finger = change.position
                        val index = letterAt(change.position)
                        val path = currentChosen
                        when {
                            index < 0 || path.lastOrNull() == index -> Unit
                            path.size > 1 && path[path.size - 2] == index -> currentOnChange(path.dropLast(1))
                            index !in path -> currentOnChange(path + index)
                        }
                    }
                },
        ) {
            val stroke = LINE_WIDTH.dp.toPx()
            chosen.zipWithNext().forEach { (from, to) ->
                drawLine(colors.line, centres[from], centres[to], strokeWidth = stroke, cap = StrokeCap.Round)
            }
            val end = finger
            val last = chosen.lastOrNull()
            if (end != null && last != null) {
                drawLine(colors.line, centres[last], end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
        letters.forEachIndexed { index, letter ->
            val selected = index in chosen
            val centre = centres[index]
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .offset(x = (centre.x - letterPx / 2).toDp(), y = (centre.y - letterPx / 2).toDp())
                        .size(letterSize)
                        .background(
                            if (selected) {
                                colors.chosen
                            } else {
                                colors.letter
                            },
                            CircleShape,
                        ).testTag(wheelTag(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (selected) {
                                colors.chosenText
                            } else {
                                colors.letterText
                            },
                    )
                }
            }
        }
    }
}

private const val LETTER_FRACTION = 0.24f
private const val LINE_WIDTH = 10
