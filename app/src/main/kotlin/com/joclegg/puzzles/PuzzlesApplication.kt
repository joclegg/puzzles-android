package com.joclegg.puzzles

import android.app.Application
import com.joclegg.puzzles.sightlines.SightlinesContainer
import com.joclegg.puzzles.solitaire.SolitaireContainer
import com.joclegg.puzzles.sudoku.SudokuContainer
import com.joclegg.puzzles.twotone.TwoToneContainer

class PuzzlesApplication : Application() {
    val sudoku by lazy { SudokuContainer(this) }
    val solitaire by lazy { SolitaireContainer(this) }
    val twoTone by lazy { TwoToneContainer(this) }
    val sightlines by lazy { SightlinesContainer(this) }
}
