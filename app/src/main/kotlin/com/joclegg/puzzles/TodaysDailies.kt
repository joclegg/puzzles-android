package com.joclegg.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.joclegg.puzzles.core.ui.DailyLeft
import com.joclegg.puzzles.fiveletters.FiveLettersRoutes
import com.joclegg.puzzles.fiveletters.core.data.WordRef
import com.joclegg.puzzles.kakuro.KakuroRoutes
import com.joclegg.puzzles.sightlines.SightlinesRoutes
import com.joclegg.puzzles.slideout.SlideOutRoutes
import com.joclegg.puzzles.solitaire.SolitaireRoutes
import com.joclegg.puzzles.solitaire.core.data.DealRef
import com.joclegg.puzzles.sudoku.SudokuRoutes
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.Medal
import com.joclegg.puzzles.sudoku.core.data.dailyDays
import com.joclegg.puzzles.twotone.TwoToneRoutes
import com.joclegg.puzzles.wordwheel.WordWheelRoutes
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import com.joclegg.puzzles.fiveletters.core.data.GameStatus as FiveLettersStatus
import com.joclegg.puzzles.kakuro.core.data.GameStatus as KakuroStatus
import com.joclegg.puzzles.kakuro.core.data.PuzzleRef as KakuroRef
import com.joclegg.puzzles.kakuro.engine.SIZES as KAKURO_SIZES
import com.joclegg.puzzles.sightlines.core.data.GameStatus as SightlinesStatus
import com.joclegg.puzzles.sightlines.core.data.PuzzleRef as SightlinesRef
import com.joclegg.puzzles.sightlines.engine.SIZES as SIGHTLINES_SIZES
import com.joclegg.puzzles.slideout.core.data.GameStatus as SlideOutStatus
import com.joclegg.puzzles.slideout.core.data.PuzzleRef as SlideOutRef
import com.joclegg.puzzles.slideout.engine.Level as SlideOutLevel
import com.joclegg.puzzles.solitaire.core.data.GameStatus as SolitaireStatus
import com.joclegg.puzzles.twotone.core.data.GameStatus as TwoToneStatus
import com.joclegg.puzzles.twotone.core.data.PuzzleRef as TwoToneRef
import com.joclegg.puzzles.twotone.engine.SIZES as TWO_TONE_SIZES
import com.joclegg.puzzles.wordwheel.core.data.GameStatus as WordWheelStatus
import com.joclegg.puzzles.wordwheel.core.data.PuzzleRef as WordWheelRef
import com.joclegg.puzzles.wordwheel.engine.Level as WordWheelLevel

@Composable
fun rememberTodaysDailies(
    app: PuzzlesApplication,
    darkTheme: Boolean,
    navController: NavHostController,
): suspend () -> List<DailyLeft> {
    val accents = SettingsTab.entries.associateWith { settingsAccent(it, darkTheme) }
    val current by rememberUpdatedState(accents)
    return { todaysDailies(app, current, navController) }
}

private suspend fun todaysDailies(
    app: PuzzlesApplication,
    accents: Map<SettingsTab, Color>,
    navController: NavHostController,
): List<DailyLeft> {
    val sudokuMedal =
        dailyDays(app.sudoku.games.history.first(), ZoneId.systemDefault())[app.solitaire.today()]?.medal ?: Medal.NONE
    val solitaireDaily = DealRef.dailyId(app.solitaire.today())
    val twoTone = app.twoTone.games.history.first()
    val sightlines = app.sightlines.games.history.first()
    val kakuro = app.kakuro.games.history.first()
    val slideOut = app.slideOut.games.history.first()
    val wordWheel = app.wordWheel.games.history.first()
    val fiveLettersDaily = WordRef.dailyId(app.fiveLetters.today())
    val fiveLettersDone =
        app.fiveLetters.games.history.first().any {
            it.ref.id == fiveLettersDaily && (it.status == FiveLettersStatus.WON || it.status == FiveLettersStatus.LOST)
        }
    val left =
        listOf(
            Triple(
                SettingsTab.SUDOKU,
                when (sudokuMedal) {
                    Medal.GOLD -> 0
                    Medal.SILVER -> GameMode.entries.size - 1
                    Medal.NONE -> GameMode.entries.size
                },
                SudokuRoutes.HOME,
            ),
            Triple(
                SettingsTab.SOLITAIRE,
                if (app.solitaire.games.history.first().any { it.deal.id == solitaireDaily && it.status == SolitaireStatus.WON }) {
                    0
                } else {
                    1
                },
                SolitaireRoutes.HOME,
            ),
            Triple(
                SettingsTab.TWO_TONE,
                TWO_TONE_SIZES.count { size ->
                    twoTone.none { it.ref.id == TwoToneRef.dailyId(app.twoTone.today(), size) && it.status == TwoToneStatus.SOLVED }
                },
                TwoToneRoutes.HOME,
            ),
            Triple(
                SettingsTab.SIGHTLINES,
                SIGHTLINES_SIZES.count { size ->
                    sightlines.none {
                        it.ref.id == SightlinesRef.dailyId(app.sightlines.today(), size) && it.status == SightlinesStatus.SOLVED
                    }
                },
                SightlinesRoutes.HOME,
            ),
            Triple(
                SettingsTab.FIVE_LETTERS,
                if (fiveLettersDone) {
                    0
                } else {
                    1
                },
                FiveLettersRoutes.HOME,
            ),
            Triple(
                SettingsTab.KAKURO,
                KAKURO_SIZES.count { size ->
                    kakuro.none { it.ref.id == KakuroRef.dailyId(app.kakuro.today(), size) && it.status == KakuroStatus.SOLVED }
                },
                KakuroRoutes.HOME,
            ),
            Triple(
                SettingsTab.SLIDE_OUT,
                SlideOutLevel.entries.count { level ->
                    slideOut.none { it.ref.id == SlideOutRef.dailyId(app.slideOut.today(), level) && it.status == SlideOutStatus.SOLVED }
                },
                SlideOutRoutes.HOME,
            ),
            Triple(
                SettingsTab.WORD_WHEEL,
                WordWheelLevel.entries.count { level ->
                    wordWheel.none {
                        it.ref.id == WordWheelRef.dailyId(app.wordWheel.today(), level) && it.status == WordWheelStatus.SOLVED
                    }
                },
                WordWheelRoutes.HOME,
            ),
        )
    return left
        .filter { (_, count, _) -> count > 0 }
        .map { (tab, count, route) ->
            DailyLeft(tab.label, accents.getValue(tab), count) { navController.navigate(route) }
        }
}
