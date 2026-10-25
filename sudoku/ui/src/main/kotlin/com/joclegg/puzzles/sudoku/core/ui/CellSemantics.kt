package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import com.joclegg.puzzles.sudoku.engine.Grid
import com.joclegg.puzzles.sudoku.engine.Highlight

val CellHighlightKey = SemanticsPropertyKey<Highlight>("CellHighlight")
var SemanticsPropertyReceiver.cellHighlight by CellHighlightKey

val CellGivenKey = SemanticsPropertyKey<Boolean>("CellGiven")
var SemanticsPropertyReceiver.cellGiven by CellGivenKey

val CellWrongKey = SemanticsPropertyKey<Boolean>("CellWrong")
var SemanticsPropertyReceiver.cellWrong by CellWrongKey

val CageWrongKey = SemanticsPropertyKey<Boolean>("CageWrong")
var SemanticsPropertyReceiver.cageWrong by CageWrongKey

fun cellTag(
    cell: Int,
    grid: Grid = Grid.STANDARD,
) = "cell_r${grid.rowOf(cell) + 1}c${grid.colOf(cell) + 1}"

fun digitTag(digit: Int) = "digit_$digit"
