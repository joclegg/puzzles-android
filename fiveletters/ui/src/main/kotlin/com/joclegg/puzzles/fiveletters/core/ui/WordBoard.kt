package com.joclegg.puzzles.fiveletters.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joclegg.puzzles.fiveletters.engine.GameState
import com.joclegg.puzzles.fiveletters.engine.MAX_GUESSES
import com.joclegg.puzzles.fiveletters.engine.Mark
import com.joclegg.puzzles.fiveletters.engine.WORD_LENGTH

@Immutable
data class BoardUi(
    val rows: List<String>,
    val marks: List<List<Mark>>,
    val letterMarks: Map<Char, Mark>,
)

fun GameState.toBoardUi(): BoardUi {
    val rows = guesses + listOfNotNull(typing.takeIf { !isOver })
    return BoardUi(rows.take(MAX_GUESSES), marks, letterMarks)
}

private val TileShape = RoundedCornerShape(4.dp)

@Composable
fun WordBoard(
    board: BoardUi,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLetterColors.current
    Column(modifier.aspectRatio(WORD_LENGTH.toFloat() / MAX_GUESSES), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until MAX_GUESSES) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until WORD_LENGTH) {
                    val letter = board.rows.getOrNull(row)?.getOrNull(col)
                    val mark = board.marks.getOrNull(row)?.getOrNull(col)
                    val tileModifier =
                        when {
                            mark != null -> Modifier.background(mark.colour(colors), TileShape)
                            letter != null -> Modifier.border(2.dp, colors.typedBorder, TileShape)
                            else -> Modifier.border(2.dp, colors.emptyBorder, TileShape)
                        }
                    BoxWithConstraints(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(TileShape)
                            .then(tileModifier)
                            .testTag(letterTag(row, col))
                            .semantics {
                                if (mark != null) {
                                    letterMark = mark
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (letter != null) {
                            Text(
                                letter.toString(),
                                fontSize = with(LocalDensity.current) { (maxHeight * LETTER_FRACTION).toSp() },
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (mark != null) {
                                        colors.markedText
                                    } else {
                                        colors.text
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val KeyRows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

@Composable
fun Keyboard(
    letterMarks: Map<Char, Mark>,
    onLetterClick: (Char) -> Unit,
    onEnterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLetterColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        KeyRows.forEachIndexed { index, letters ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (index == KeyRows.lastIndex) {
                    Box(
                        Modifier
                            .weight(WIDE_KEY)
                            .height(KEY_HEIGHT)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.key)
                            .clickable(onClick = onDeleteClick)
                            .testTag("key_delete"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(FiveLettersIcons.Delete, contentDescription = "Delete", tint = colors.keyText)
                    }
                } else if (index == 1) {
                    Box(Modifier.weight(HALF_KEY))
                }
                letters.forEach { letter ->
                    val mark = letterMarks[letter]
                    Key(
                        letter.toString(),
                        mark?.colour(colors) ?: colors.key,
                        if (mark != null) {
                            colors.markedText
                        } else {
                            colors.keyText
                        },
                        { onLetterClick(letter) },
                        Modifier.weight(1f).testTag(keyTag(letter)),
                    )
                }
                if (index == KeyRows.lastIndex) {
                    Key("ENTER", colors.key, colors.keyText, onEnterClick, Modifier.weight(WIDE_KEY).testTag("key_enter"))
                } else if (index == 1) {
                    Box(Modifier.weight(HALF_KEY))
                }
            }
        }
    }
}

@Composable
private fun Key(
    label: String,
    background: Color,
    textColour: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(KEY_HEIGHT)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = textColour,
            fontWeight = FontWeight.SemiBold,
            fontSize =
                if (label.length > 1) {
                    12.sp
                } else {
                    18.sp
                },
        )
    }
}

private fun Mark.colour(colors: LetterColors) =
    when (this) {
        Mark.CORRECT -> colors.correct
        Mark.PRESENT -> colors.present
        Mark.ABSENT -> colors.absent
    }

private val KEY_HEIGHT = 56.dp
private const val WIDE_KEY = 1.5f
private const val HALF_KEY = 0.5f
private const val LETTER_FRACTION = 0.5f
