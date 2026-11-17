package com.joclegg.puzzles.crossword.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.joclegg.puzzles.crossword.engine.EMPTY
import com.joclegg.puzzles.crossword.engine.GameState
import com.joclegg.puzzles.crossword.engine.Puzzle
import com.joclegg.puzzles.crossword.engine.Word
import com.joclegg.puzzles.crossword.engine.enumeration
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Immutable
data class BoardUi(
    val puzzle: Puzzle,
    val entries: String,
    val selected: Int?,
    val wordCells: Set<Int>,
    val wrong: Set<Int>,
)

@Immutable
data class ClueUi(
    val number: Int,
    val isAcross: Boolean,
    val text: String,
    val enumeration: String,
    val filled: Boolean,
)

fun GameState.toBoardUi(showMistakes: Boolean): BoardUi =
    BoardUi(
        puzzle = puzzle,
        entries = entries,
        selected = selected,
        wordCells = currentWord?.cells.orEmpty().toSet(),
        wrong =
            if (showMistakes) {
                wrongCells
            } else {
                emptySet()
            },
    )

fun GameState.clueUi(
    word: Word,
    clueFor: (String) -> String,
): ClueUi =
    ClueUi(
        number = word.number,
        isAcross = word.isAcross,
        text = clueFor(word.cells.map { puzzle.solution[it] }.joinToString("")),
        enumeration = enumeration(word),
        filled = isWordFilled(word),
    )

@Composable
fun CrosswordBoard(
    board: BoardUi,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCrosswordColors.current
    val size = board.puzzle.size
    Column(
        modifier
            .aspectRatio(1f)
            .background(colors.line)
            .border(1.dp, colors.line)
            .testTag("board"),
    ) {
        for (row in 0 until size) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (col in 0 until size) {
                    val cell = row * size + col
                    val cellModifier = Modifier.weight(1f).fillMaxSize().padding(0.5.dp)
                    if (board.puzzle.isWhite[cell]) {
                        WhiteCell(board, cell, onCellClick, cellModifier)
                    } else {
                        Box(cellModifier.background(colors.block))
                    }
                }
            }
        }
    }
}

@Composable
fun BoardViewport(
    board: BoardUi,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val size = board.puzzle.size
        val side = boardSide(maxWidth, maxHeight, size)
        if (side <= maxHeight) {
            CrosswordBoard(board, onCellClick, Modifier.size(side))
        } else {
            val scroll = rememberScrollState()
            val viewportPx = constraints.maxHeight
            val sidePx = with(LocalDensity.current) { side.roundToPx() }
            val rows = board.wordCells.map { it / size }
            val selectedRow = board.selected?.let { it / size }
            LaunchedEffect(selectedRow, rows, viewportPx, sidePx) {
                if (selectedRow != null) {
                    val wordRows = (rows.minOrNull() ?: selectedRow)..(rows.maxOrNull() ?: selectedRow)
                    scroll.animateScrollTo(boardScroll(scroll.value, viewportPx, sidePx, size, selectedRow, wordRows))
                }
            }
            Box(
                Modifier
                    .width(side)
                    .fillMaxHeight()
                    .verticalScroll(scroll)
                    .testTag("board_viewport"),
            ) {
                CrosswordBoard(board, onCellClick, Modifier.size(side))
            }
        }
    }
}

fun boardSide(
    maxWidth: Dp,
    maxHeight: Dp,
    size: Int,
): Dp = min(maxWidth, max(maxHeight, COMFORTABLE_CELL * size))

internal fun boardScroll(
    current: Int,
    viewport: Int,
    side: Int,
    size: Int,
    selectedRow: Int,
    wordRows: IntRange,
): Int {
    val cell = side.toFloat() / size
    val top = floor(wordRows.first * cell).toInt()
    val bottom = ceil((wordRows.last + 1) * cell).toInt()
    val margin = ((viewport - (bottom - top)) / 2).coerceIn(0, cell.roundToInt())
    val target =
        if (bottom - top <= viewport) {
            current.coerceIn(bottom + margin - viewport, top - margin)
        } else {
            ((selectedRow + 0.5f) * cell - viewport / 2f).roundToInt()
        }
    return target.coerceIn(0, (side - viewport).coerceAtLeast(0))
}

@Composable
private fun WhiteCell(
    board: BoardUi,
    cell: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCrosswordColors.current
    val wrong = cell in board.wrong
    val isSelected = cell == board.selected
    val inWord = cell in board.wordCells
    val background =
        when {
            isSelected -> colors.selected
            inWord -> colors.word
            else -> colors.background
        }
    val letter = board.entries[cell]
    BoxWithConstraints(
        modifier
            .background(background)
            .clickable { onCellClick(cell) }
            .testTag(cellTag(cell, board.puzzle.size))
            .semantics {
                cellWrong = wrong
                cellSelected = isSelected
                cellInWord = inWord
                contentDescription =
                    if (letter == EMPTY) {
                        ""
                    } else {
                        letter.toString()
                    }
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        board.puzzle.numbers[cell]?.let { number ->
            Text(
                number.toString(),
                fontSize = with(density) { (maxHeight * NUMBER_FRACTION).toSp() },
                color = colors.number,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 1.dp),
            )
        }
        if (letter != EMPTY) {
            Text(
                letter.toString(),
                fontSize = with(density) { (maxHeight * LETTER_FRACTION).toSp() },
                fontWeight = FontWeight.Medium,
                color =
                    if (wrong) {
                        colors.wrong
                    } else {
                        colors.letter
                    },
                modifier = Modifier.padding(top = maxHeight * NUMBER_FRACTION / 2),
            )
        }
    }
}

fun clueLabel(clue: ClueUi) = "${clue.text} ${clue.enumeration}"

@Composable
fun ClueBar(
    clue: ClueUi?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCrosswordColors.current
    Surface(color = colors.word, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.testTag("clue_previous")) {
                Icon(CrosswordIcons.Previous, contentDescription = stringResource(R.string.previous_clue))
            }
            Text(
                clue
                    ?.let {
                        val direction =
                            if (it.isAcross) {
                                stringResource(R.string.across_short)
                            } else {
                                stringResource(R.string.down_short)
                            }
                        "${it.number}$direction  ${clueLabel(it)}"
                    }.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.letter,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).testTag("clue_bar"),
            )
            IconButton(onClick = onNext, modifier = Modifier.testTag("clue_next")) {
                Icon(CrosswordIcons.Next, contentDescription = stringResource(R.string.next_clue))
            }
        }
    }
}

@Composable
fun ClueLists(
    across: List<ClueUi>,
    down: List<ClueUi>,
    current: ClueUi?,
    onClueClick: (ClueUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val acrossTitle = stringResource(R.string.across)
    val downTitle = stringResource(R.string.down)
    LazyColumn(modifier.testTag("clue_lists"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { ClueHeading(acrossTitle) }
        items(across, key = { "a${it.number}" }) { ClueRow(it, it == current, onClueClick) }
        item { ClueHeading(downTitle) }
        items(down, key = { "d${it.number}" }) { ClueRow(it, it == current, onClueClick) }
    }
}

@Composable
private fun ClueHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun ClueRow(
    clue: ClueUi,
    current: Boolean,
    onClueClick: (ClueUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCrosswordColors.current
    Row(
        modifier
            .fillMaxWidth()
            .background(
                if (current) {
                    colors.word
                } else {
                    colors.background.copy(alpha = 0f)
                },
            ).clickable { onClueClick(clue) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(clueTag(clue.number, clue.isAcross))
            .semantics { selected = current },
    ) {
        Text("${clue.number}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Text(
            clueLabel(clue),
            color =
                if (clue.filled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}

private val COMFORTABLE_CELL = 32.dp
private const val LETTER_FRACTION = 0.55f
private const val NUMBER_FRACTION = 0.28f
