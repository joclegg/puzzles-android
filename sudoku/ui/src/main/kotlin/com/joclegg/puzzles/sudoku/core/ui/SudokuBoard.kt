package com.joclegg.puzzles.sudoku.core.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.sudoku.engine.Cage
import com.joclegg.puzzles.sudoku.engine.GameState
import com.joclegg.puzzles.sudoku.engine.Grid
import com.joclegg.puzzles.sudoku.engine.Highlight
import com.joclegg.puzzles.sudoku.engine.NO_CAGE
import com.joclegg.puzzles.sudoku.engine.digitBit

@Immutable
data class CellUi(
    val value: Int,
    val notes: Int = 0,
    val given: Boolean = false,
    val wrong: Boolean = false,
    val highlight: Highlight = Highlight.NONE,
    val numberHighlighted: Boolean = false,
    val marked: Boolean = false,
)

@Immutable
data class BoardUi(
    val cells: List<CellUi>,
    val cages: List<Cage> = emptyList(),
    val wrongCages: Set<Int> = emptySet(),
    val highlightDigit: Int? = null,
    val boldPlacedDigits: Boolean = true,
    val greyGivenCells: Boolean = true,
) {
    val grid: Grid get() = Grid.forCellCount(cells.size)
}

fun GameState.toBoardUi() =
    BoardUi(
        cells =
            values.indices.map { cell ->
                CellUi(
                    value = values[cell],
                    notes = notes[cell],
                    given = isGiven(cell),
                    wrong = isWrong(cell) || rejected?.clashes?.contains(cell) == true,
                    highlight = highlight(cell),
                    numberHighlighted = isNumberHighlighted(cell),
                    marked = cell in marked,
                )
            },
        cages = puzzle.cages,
        wrongCages =
            puzzle.cages.indices
                .filter { isCageWrong(puzzle.cages[it]) }
                .toSet(),
        highlightDigit = noteHighlightDigit,
    )

@Composable
fun SudokuBoard(
    board: BoardUi,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(colors.background),
    ) {
        val grid = board.grid
        val cellSize = maxWidth / grid.size
        val cageOf =
            IntArray(grid.cells) { NO_CAGE }.also { owner ->
                board.cages.forEachIndexed { i, cage -> cage.cells.forEach { owner[it] = i } }
            }
        Column(Modifier.fillMaxSize()) {
            repeat(grid.size) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(grid.size) { col ->
                        val cell = grid.cellAt(row, col)
                        val cage = cageOf[cell].takeIf { it != NO_CAGE }
                        SudokuCell(
                            cell = board.cells[cell],
                            highlightDigit = board.highlightDigit,
                            cellSize = cellSize,
                            noteDigits = grid.digits,
                            digitFraction =
                                if (grid == Grid.STANDARD) {
                                    STANDARD_DIGIT_FRACTION
                                } else {
                                    FAST_DIGIT_FRACTION
                                },
                            boldPlaced = board.boldPlacedDigits,
                            greyGivenCells = board.greyGivenCells,
                            inCage = cage != null,
                            hasCageSum = cage != null && board.cages[cage].cells.min() == cell,
                            cageWrong = cage != null && cage in board.wrongCages,
                            onClick = { onCellClick(cell) },
                            modifier = Modifier.weight(1f).testTag(cellTag(cell, grid)),
                        )
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawGrid(colors, grid)
            board.cages.forEachIndexed { index, cage ->
                drawCage(
                    cage,
                    cageOf,
                    if (index in board.wrongCages) {
                        colors.wrong
                    } else {
                        colors.cageLine
                    },
                    grid,
                )
            }
        }
        board.cages.forEachIndexed { index, cage ->
            val anchor = cage.cells.min()
            CageSum(
                sum = cage.sum,
                wrong = index in board.wrongCages,
                background = cellBackground(board.cells[anchor], colors, board.greyGivenCells).takeOrElse { colors.background },
                cellSize = cellSize,
                modifier =
                    Modifier
                        .offset(
                            x = cellSize * grid.colOf(anchor) + cellSize * CAGE_SUM_OFFSET,
                            y = cellSize * grid.rowOf(anchor) + cellSize * CAGE_SUM_OFFSET,
                        ).testTag("cage_sum_${cellTag(anchor, grid).removePrefix("cell_")}"),
            )
        }
    }
}

@Composable
private fun CageSum(
    sum: Int,
    wrong: Boolean,
    background: Color,
    cellSize: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    val fontSize = cellSize.toSp(CAGE_SUM_FONT_FRACTION)
    Text(
        text = sum.toString(),
        style =
            TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Medium,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        color =
            if (wrong) {
                colors.wrong
            } else {
                colors.cageSum
            },
        modifier = modifier.background(background).padding(horizontal = 1.5.dp, vertical = 0.5.dp),
    )
}

private fun cellBackground(
    cell: CellUi,
    colors: BoardColors,
    greyGivenCells: Boolean,
): Color {
    val grey = cell.given && greyGivenCells
    return when (cell.highlight) {
        Highlight.SELECTED -> {
            colors.selected
        }

        Highlight.SAME_DIGIT, Highlight.NOTE_MATCH -> {
            colors.sameDigit
        }

        Highlight.PEER -> {
            if (grey) {
                lerp(colors.givenCell, colors.peer, 0.5f)
            } else {
                colors.peer
            }
        }

        Highlight.NONE -> {
            if (grey) {
                colors.givenCell
            } else {
                Color.Unspecified
            }
        }
    }
}

@Composable
private fun SudokuCell(
    cell: CellUi,
    highlightDigit: Int?,
    cellSize: Dp,
    noteDigits: IntRange,
    digitFraction: Float,
    boldPlaced: Boolean,
    greyGivenCells: Boolean,
    inCage: Boolean,
    hasCageSum: Boolean,
    cageWrong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current

    val background = cellBackground(cell, colors, greyGivenCells).takeOrElse { Color.Transparent }
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .then(
                if (cell.marked) {
                    Modifier.padding(1.dp).border(2.5.dp, colors.trialMark, RoundedCornerShape(3.dp))
                } else {
                    Modifier
                },
            ).clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                cellHighlight = cell.highlight
                cellGiven = cell.given
                cellWrong = cell.wrong
                cellMarked = cell.marked
                this.cageWrong = cageWrong
            },
        contentAlignment = Alignment.Center,
    ) {
        if (cell.value != 0) {
            Text(
                text = cell.value.toString(),
                fontSize = cellSize.toSp(digitFraction),
                fontWeight =
                    when {
                        cell.given -> FontWeight.Normal
                        boldPlaced -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                color =
                    when {
                        cell.wrong -> colors.wrong
                        cell.numberHighlighted -> colors.highlightedNote
                        else -> colors.enteredDigit
                    },
            )
        } else if (cell.notes != 0) {
            NotesGrid(
                cell.notes,
                highlightDigit,
                cellSize,
                noteDigits,
                inset =
                    if (inCage) {
                        CAGE_NOTE_INSET_FRACTION
                    } else {
                        NOTE_INSET_FRACTION
                    },
                topInset =
                    if (inCage) {
                        CAGE_SUM_NOTE_TOP_FRACTION
                    } else {
                        NOTE_INSET_FRACTION
                    },
                fontFraction =
                    if (inCage) {
                        CAGE_NOTE_FONT_FRACTION
                    } else {
                        NOTE_FONT_FRACTION
                    },
            )
        }
    }
}

@Composable
private fun NotesGrid(
    notes: Int,
    highlightDigit: Int?,
    cellSize: Dp,
    noteDigits: IntRange,
    inset: Float,
    topInset: Float,
    fontFraction: Float,
) {
    val colors = LocalBoardColors.current
    val fontSize = cellSize.toSp(fontFraction)
    val noteStyle =
        TextStyle(
            fontSize = fontSize,
            lineHeight = fontSize,
            textAlign = TextAlign.Center,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
    Column(
        Modifier.fillMaxSize().padding(
            start = cellSize * inset,
            end = cellSize * inset,
            bottom = cellSize * inset,
            top =
                cellSize * topInset,
        ),
    ) {
        repeat(NOTE_ROWS) { row ->
            Row(Modifier.weight(1f)) {
                repeat(NOTE_COLUMNS) { col ->
                    val digit = row * NOTE_COLUMNS + col + 1
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (digit in noteDigits && notes and digitBit(digit) != 0) {
                            Text(
                                text = digit.toString(),
                                style = noteStyle,
                                color =
                                    if (digit == highlightDigit) {
                                        colors.highlightedNote
                                    } else {
                                        colors.noteDigit
                                    },
                                fontWeight =
                                    if (digit == highlightDigit) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val NOTE_FONT_FRACTION = 0.26f
private const val NOTE_INSET_FRACTION = 0.06f
private const val STANDARD_DIGIT_FRACTION = 0.72f
private const val FAST_DIGIT_FRACTION = 0.64f
private const val CAGE_NOTE_INSET_FRACTION = 0.13f
private const val CAGE_NOTE_FONT_FRACTION = 0.21f
private const val CAGE_SUM_NOTE_TOP_FRACTION = 0.27f
private const val CAGE_SUM_FONT_FRACTION = 0.24f
private const val CAGE_SUM_OFFSET = 0.02f
private const val CAGE_LINE_INSET_FRACTION = 0.06f
private const val CAGE_DASH_FRACTION = 0.06f
private const val CAGE_GAP_FRACTION = 0.05f
private const val NOTE_ROWS = 3
private const val NOTE_COLUMNS = 3

@Composable
private fun Dp.toSp(fraction: Float): TextUnit = with(LocalDensity.current) { (this@toSp * fraction).toSp() }

private fun DrawScope.drawGrid(
    colors: BoardColors,
    grid: Grid,
) {
    val step = size.width / grid.size

    fun line(thick: Boolean) =
        (
            if (thick) {
                colors.thickLine
            } else {
                colors.thinLine
            }
        ) to (
            if (thick) {
                2.dp.toPx()
            } else {
                1.dp.toPx()
            }
        )
    for (i in 0..grid.size) {
        val (verticalColor, verticalWidth) = line(i % grid.boxCols == 0)
        drawLine(verticalColor, Offset(i * step, 0f), Offset(i * step, size.height), verticalWidth)
        val (horizontalColor, horizontalWidth) = line(i % grid.boxRows == 0)
        drawLine(horizontalColor, Offset(0f, i * step), Offset(size.width, i * step), horizontalWidth)
    }
}

private fun DrawScope.drawCage(
    cage: Cage,
    cageOf: IntArray,
    color: Color,
    grid: Grid,
) {
    val step = size.width / grid.size
    val inset = step * CAGE_LINE_INSET_FRACTION
    val owner = cageOf[cage.cells.first()]
    val dash = PathEffect.dashPathEffect(floatArrayOf(step * CAGE_DASH_FRACTION, step * CAGE_GAP_FRACTION))
    val width = 1.dp.toPx()

    fun same(
        row: Int,
        col: Int,
    ) = row in 0 until grid.size && col in 0 until grid.size && cageOf[grid.cellAt(row, col)] == owner
    for (cell in cage.cells) {
        val r = grid.rowOf(cell)
        val c = grid.colOf(cell)
        val left = c * step
        val top = r * step
        val x0 =
            if (same(r, c - 1)) {
                left
            } else {
                left + inset
            }
        val x1 =
            if (same(r, c + 1)) {
                left + step
            } else {
                left + step - inset
            }
        val y0 =
            if (same(r - 1, c)) {
                top
            } else {
                top + inset
            }
        val y1 =
            if (same(r + 1, c)) {
                top + step
            } else {
                top + step - inset
            }
        if (!same(r - 1, c)) {
            drawLine(color, Offset(x0, y0), Offset(x1, y0), width, pathEffect = dash)
        }
        if (!same(r + 1, c)) {
            drawLine(color, Offset(x0, y1), Offset(x1, y1), width, pathEffect = dash)
        }
        if (!same(r, c - 1)) {
            drawLine(color, Offset(x0, y0), Offset(x0, y1), width, pathEffect = dash)
        }
        if (!same(r, c + 1)) {
            drawLine(color, Offset(x1, y0), Offset(x1, y1), width, pathEffect = dash)
        }
    }
}
