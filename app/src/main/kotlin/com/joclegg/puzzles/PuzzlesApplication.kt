package com.joclegg.puzzles

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.joclegg.puzzles.core.sync.SupabaseAuth
import com.joclegg.puzzles.core.sync.SyncEngine
import com.joclegg.puzzles.core.sync.SyncStore
import com.joclegg.puzzles.fiveletters.FiveLettersContainer
import com.joclegg.puzzles.kakuro.KakuroContainer
import com.joclegg.puzzles.sightlines.SightlinesContainer
import com.joclegg.puzzles.slideout.SlideOutContainer
import com.joclegg.puzzles.solitaire.SolitaireContainer
import com.joclegg.puzzles.sudoku.SudokuContainer
import com.joclegg.puzzles.twotone.TwoToneContainer
import com.joclegg.puzzles.wordwheel.WordWheelContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.syncPreferences by preferencesDataStore(name = "sync")

class PuzzlesApplication : Application() {
    val sudoku by lazy { SudokuContainer(this) }
    val solitaire by lazy { SolitaireContainer(this) }
    val twoTone by lazy { TwoToneContainer(this) }
    val sightlines by lazy { SightlinesContainer(this) }
    val fiveLetters by lazy { FiveLettersContainer(this) }
    val kakuro by lazy { KakuroContainer(this) }
    val slideOut by lazy { SlideOutContainer(this) }
    val wordWheel by lazy { WordWheelContainer(this) }
    private val syncStore by lazy { SyncStore(syncPreferences) }
    val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val auth by lazy { SupabaseAuth(syncStore) }
    val banks by lazy { BankStore(this) }
    val sync by lazy {
        SyncEngine(
            sources =
                listOf(
                    sudoku.sync,
                    solitaire.sync,
                    twoTone.sync,
                    sightlines.sync,
                    fiveLetters.sync,
                    kakuro.sync,
                    slideOut.sync,
                    wordWheel.sync,
                ),
            auth = auth,
            store = syncStore,
        )
    }
}
