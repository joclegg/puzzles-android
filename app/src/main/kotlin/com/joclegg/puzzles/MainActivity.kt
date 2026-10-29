package com.joclegg.puzzles

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.joclegg.puzzles.core.sync.SignInProvider
import com.joclegg.puzzles.fiveletters.FiveLettersFixtures
import com.joclegg.puzzles.kakuro.KakuroFixtures
import com.joclegg.puzzles.sightlines.SightlinesFixtures
import com.joclegg.puzzles.slideout.SlideOutFixtures
import com.joclegg.puzzles.solitaire.SolitaireFixtures
import com.joclegg.puzzles.sudoku.SudokuFixtures
import com.joclegg.puzzles.sudoku.core.data.ThemeMode
import com.joclegg.puzzles.sudoku.core.ui.SudokuTheme
import com.joclegg.puzzles.twotone.TwoToneFixtures
import com.joclegg.puzzles.wordwheel.WordWheelFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val openGame = mutableStateOf<OpenGame?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as PuzzlesApplication
        if (savedInstanceState == null) {
            openFixture(app)
            completeSignIn(intent)
        }
        setContent {
            val settings by app.sudoku.settings.settings
                .collectAsStateWithLifecycle(initialValue = null)
            val darkTheme =
                when (settings?.theme ?: ThemeMode.SYSTEM) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            DisposableEffect(darkTheme) {
                val style =
                    if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            val account by app.auth.email.collectAsStateWithLifecycle(initialValue = null)
            SudokuTheme(darkTheme = darkTheme) {
                PuzzlesApp(
                    app,
                    darkTheme,
                    openGame = openGame.value,
                    onConsumeOpenGame = { openGame.value = null },
                    account = account,
                    onSignInClick = ::signIn,
                    onSignOutClick = { app.background.launch { app.auth.signOut() } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        completeSignIn(intent)
    }

    override fun onStart() {
        super.onStart()
        syncInBackground()
    }

    override fun onStop() {
        super.onStop()
        syncInBackground()
    }

    private fun syncInBackground() {
        val app = application as PuzzlesApplication
        app.background.launch { app.sync.sync() }
    }

    private fun signIn(provider: SignInProvider) {
        val app = application as PuzzlesApplication
        lifecycleScope.launch {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.auth.signInUrl(provider))))
        }
    }

    private fun completeSignIn(intent: Intent) {
        val data = intent.data ?: return
        val code = data.getQueryParameter("code")
        if (data.scheme != AUTH_SCHEME || code == null) {
            return
        }
        val app = application as PuzzlesApplication
        app.background.launch {
            if (app.auth.complete(code)) {
                app.sync.sync()
            }
        }
    }

    private fun openFixture(app: PuzzlesApplication) {
        val name = intent.getStringExtra(FIXTURE_EXTRA)
        val draft = SudokuFixtures.draft(intent.getStringExtra(SudokuFixtures.DRAFT_EXTRA))
        val sudoku = SudokuFixtures.puzzle(name)
        val solitaire = SolitaireFixtures.deal(name)
        val twoTone = TwoToneFixtures.puzzle(name)
        val sightlines = SightlinesFixtures.puzzle(name)
        val fiveLetters = FiveLettersFixtures.word(name)
        val kakuro = KakuroFixtures.puzzle(name)
        val slideOut = SlideOutFixtures.puzzle(name)
        val wordWheel = WordWheelFixtures.puzzle(name)
        lifecycleScope.launch {
            if (draft != null) {
                app.sudoku.settings.setCustomDraft(draft)
                openGame.value = OpenGame(PuzzleGame.SUDOKU, id = null)
            }
            if (sudoku != null) {
                val mistakeCheck =
                    app.sudoku.settings.settings
                        .first()
                        .mistakeCheckFor(sudoku.mode)
                openGame.value =
                    OpenGame(
                        PuzzleGame.SUDOKU,
                        app.sudoku.games
                            .start(sudoku, mistakeCheck)
                            .id,
                    )
            }
            if (solitaire != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.SOLITAIRE,
                        app.solitaire.games
                            .start(solitaire)
                            .id,
                    )
            }
            if (twoTone != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.TWO_TONE,
                        app.twoTone.games
                            .start { twoTone }
                            .id,
                    )
            }
            if (sightlines != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.SIGHTLINES,
                        app.sightlines.games
                            .start { sightlines }
                            .id,
                    )
            }
            if (fiveLetters != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.FIVE_LETTERS,
                        app.fiveLetters.games
                            .start(fiveLetters, hardMode = false)
                            .id,
                    )
            }
            if (kakuro != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.KAKURO,
                        app.kakuro.games
                            .start { kakuro }
                            .id,
                    )
            }
            if (slideOut != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.SLIDE_OUT,
                        app.slideOut.games
                            .start { slideOut }
                            .id,
                    )
            }
            if (wordWheel != null) {
                openGame.value =
                    OpenGame(
                        PuzzleGame.WORD_WHEEL,
                        app.wordWheel.games
                            .start { wordWheel }
                            .id,
                    )
            }
        }
    }

    private companion object {
        const val FIXTURE_EXTRA = "fixture"
        const val AUTH_SCHEME = "com.joclegg.puzzles"
    }
}
