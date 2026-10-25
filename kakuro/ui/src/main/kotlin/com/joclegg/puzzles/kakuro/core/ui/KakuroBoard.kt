package com.joclegg.puzzles.kakuro.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.kakuro.engine.Combinations
import com.joclegg.puzzles.kakuro.engine.GameState
import com.joclegg.puzzles.kakuro.engine.Puzzle

@Immutable
data class BoardUi(
    val puzzle: Puzzle,
    val values: List<Int>,
    val notes: List<Int>,
    val selected: Int?,
    val runCells: Set<Int>,
    val wrong: Set<Int>,
    val sameDigit: Set<Int> = emptySet(),
)

fun GameState.toBoardUi(): BoardUi {
    val current = selected
    return BoardUi(
        puzzle = puzzle,
        values = values,
        notes = notes,
        selected = current,
        runCells =
            current
                ?.let { cell -> puzzle.runsOf[cell].orEmpty().flatMap { it.cells } }
                .orEmpty()
                .toSet(),
        wrong = shownWrongCells + setOfNotNull(rejected),
        sameDigit =
            activeDigit
                ?.let { digit -> values.indices.filter { values[it] == digit }.toSet() }
                .orEmpty(),
    )
}

@Composable
fun KakuroBoard(
    board: BoardUi,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKakuroColors.current
    val size = board.puzzle.size
    Column(modifier.aspectRatio(1f).background(colors.line).border(1.dp, colors.line)) {
        for (row in 0 until size) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (col in 0 until size) {
                    val cell = row * size + col
                    val cellModifier = Modifier.weight(1f).fillMaxSize().padding(0.5.dp)
                    if (board.puzzle.isWhite[cell]) {
                        WhiteCell(board, cell, onCellClick, cellModifier)
                    } else {
                        BlockCell(board.puzzle.acrossClues[cell], board.puzzle.downClues[cell], cellModifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteCell(
    board: BoardUi,
    cell: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKakuroColors.current
    val given = cell in board.puzzle.givens
    val wrong = cell in board.wrong
    val background =
        when {
            cell == board.selected -> colors.selected
            cell in board.sameDigit -> colors.sameDigit
            given -> colors.givenCell
            cell in board.runCells -> colors.run
            else -> colors.background
        }
    BoxWithConstraints(
        modifier
            .background(background)
            .clickable { onCellClick(cell) }
            .testTag(cellTag(cell, board.puzzle.size))
            .semantics {
                cellWrong = wrong
                cellGiven = given
            },
        contentAlignment = Alignment.Center,
    ) {
        val textSize = with(LocalDensity.current) { (maxHeight * DIGIT_FRACTION).toSp() }
        val value = board.values[cell]
        if (value != 0) {
            Text(
                value.toString(),
                fontSize = textSize,
                color =
                    if (wrong) {
                        colors.wrong
                    } else {
                        colors.digit
                    },
            )
        } else if (board.notes[cell] != 0) {
            val noteSize = with(LocalDensity.current) { (maxHeight * NOTE_FRACTION).toSp() }
            Column(Modifier.fillMaxSize().padding(1.dp)) {
                for (noteRow in 0 until NOTE_GRID) {
                    Row(Modifier.weight(1f)) {
                        for (noteCol in 0 until NOTE_GRID) {
                            val digit = noteRow * NOTE_GRID + noteCol + 1
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (board.notes[cell] and Combinations.bit(digit) != 0) {
                                    Text(digit.toString(), fontSize = noteSize, color = colors.note)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockCell(
    across: Int?,
    down: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKakuroColors.current
    BoxWithConstraints(modifier.background(colors.block)) {
        val clueSize = with(LocalDensity.current) { (maxHeight * CLUE_FRACTION).toSp() }
        if (across != null || down != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(colors.line, Offset.Zero, Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
        }
        if (across != null) {
            Text(
                across.toString(),
                fontSize = clueSize,
                fontWeight = FontWeight.SemiBold,
                color = colors.clue,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp, top = 1.dp),
            )
        }
        if (down != null) {
            Text(
                down.toString(),
                fontSize = clueSize,
                fontWeight = FontWeight.SemiBold,
                color = colors.clue,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 3.dp, bottom = 1.dp),
            )
        }
    }
}

private const val DIGIT_FRACTION = 0.55f
private const val NOTE_FRACTION = 0.2f
private const val CLUE_FRACTION = 0.3f
private const val NOTE_GRID = 3
