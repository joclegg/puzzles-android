package com.joclegg.puzzles.crossword.feature.game

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.HelpersHint
import com.joclegg.puzzles.core.ui.WinDialog
import com.joclegg.puzzles.core.ui.WinStat
import com.joclegg.puzzles.core.ui.formatDuration
import com.joclegg.puzzles.crossword.feature.R
import com.joclegg.puzzles.crossword.ui.BoardInput
import com.joclegg.puzzles.crossword.ui.BoardViewport
import com.joclegg.puzzles.crossword.ui.ClueBar
import com.joclegg.puzzles.crossword.ui.ClueLists
import com.joclegg.puzzles.crossword.ui.ClueUi
import com.joclegg.puzzles.crossword.ui.CrosswordBoard
import com.joclegg.puzzles.crossword.ui.CrosswordIcons
import com.joclegg.puzzles.crossword.ui.CrosswordInput
import com.joclegg.puzzles.crossword.ui.HintButton
import com.joclegg.puzzles.crossword.ui.HowToPlayButton
import com.joclegg.puzzles.crossword.ui.LocalCrosswordColors
import com.joclegg.puzzles.crossword.ui.ToolButton
import com.joclegg.puzzles.crossword.ui.TourStep
import com.joclegg.puzzles.crossword.ui.boardSide
import com.joclegg.puzzles.crossword.ui.dialogTestTags
import com.joclegg.puzzles.crossword.ui.rememberTourState
import com.joclegg.puzzles.crossword.ui.tourTarget
import kotlinx.coroutines.launch

@Composable
fun GameRoute(
    viewModel: GameViewModel,
    onOpenGame: (Long) -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible()
        onPauseOrDispose { viewModel.onHidden() }
    }
    state?.let {
        GameScreen(
            state = it,
            onCellClick = viewModel::onCellClick,
            onClueClick = viewModel::onClueClick,
            onInput = viewModel::onInput,
            onPreviousClue = viewModel::onPreviousClue,
            onNextClue = viewModel::onNextClue,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onHint = viewModel::onHint,
            onRestart = viewModel::onRestart,
            onRevealLetter = viewModel::onRevealLetter,
            onRevealWord = viewModel::onRevealWord,
            onRevealSolution = viewModel::onRevealSolution,
            onNewPuzzle = { scope.launch { onOpenGame(viewModel.newPuzzle()) } },
            onSettings = onSettings,
            onHome = onHome,
            modifier = modifier,
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    onCellClick: (Int) -> Unit,
    onClueClick: (ClueUi) -> Unit,
    onInput: (BoardInput) -> Unit,
    onPreviousClue: () -> Unit,
    onNextClue: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onRevealLetter: () -> Unit,
    onRevealWord: () -> Unit,
    onRevealSolution: () -> Unit,
    onNewPuzzle: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var revealing by remember { mutableStateOf(false) }
    val tour = rememberTourState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusBoard: () -> Unit = {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (windowFocused) {
            focusBoard()
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.systemBars)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onHome)
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("game_title"))
                Row {
                    if (state.showTimer) {
                        Text(formatDuration(state.elapsedMs), modifier = Modifier.testTag("timer"))
                        Spacer(Modifier.width(12.dp))
                    }
                }
            }
            ToolButton(CrosswordIcons.Undo, stringResource(R.string.undo), onUndo, tag = "undo", enabled = state.canUndo)
            ToolButton(CrosswordIcons.Redo, stringResource(R.string.redo), onRedo, tag = "redo", enabled = state.canRedo)
            if (state.showHints) {
                HintButton(onHint, enabled = !state.solved && !state.revealed, modifier = Modifier.tourTarget(tour, "hint"))
            }
            HowToPlayButton(RULES, TUTORIAL, tour)
            Column {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("game_menu").tourTarget(tour, "menu")) {
                    Icon(CrosswordIcons.More, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.dialogTestTags()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.restart_this_puzzle)) },
                        enabled = !state.solved,
                        onClick = {
                            menuOpen = false
                            onRestart()
                        },
                        modifier = Modifier.testTag("restart"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_puzzle)) },
                        onClick = {
                            menuOpen = false
                            onNewPuzzle()
                        },
                        modifier = Modifier.testTag("new_puzzle"),
                    )
                    if (state.allowReveal && !state.solved) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reveal_letter)) },
                            onClick = {
                                menuOpen = false
                                onRevealLetter()
                            },
                            modifier = Modifier.testTag("menu_reveal_letter"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reveal_word)) },
                            onClick = {
                                menuOpen = false
                                onRevealWord()
                            },
                            modifier = Modifier.testTag("menu_reveal_word"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reveal_puzzle)) },
                            onClick = {
                                menuOpen = false
                                revealing = true
                            },
                            modifier = Modifier.testTag("menu_solution"),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                        modifier = Modifier.testTag("menu_settings"),
                    )
                }
            }
        }
        HelpersHint(onSettings)
        state.hintMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("hint_message"),
            )
        }
        CrosswordInput(onInput, focusRequester, Modifier.size(1.dp))
        val onCell = { cell: Int ->
            focusBoard()
            onCellClick(cell)
        }
        val onClue = { clue: ClueUi ->
            focusBoard()
            onClueClick(clue)
        }
        val window = LocalWindowInfo.current.containerSize
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = window.width > window.height && maxWidth >= WIDE_MIN_WIDTH
            val clueBar =
                @Composable { barModifier: Modifier ->
                    ClueBar(state.currentClue, onPreviousClue, onNextClue, barModifier.tourTarget(tour, "clue"))
                }
            if (wide) {
                val boardWidth = boardSide(maxWidth / 2, maxHeight - 16.dp, state.board.puzzle.size)
                Row(Modifier.fillMaxSize().padding(8.dp)) {
                    BoardViewport(state.board, onCell, Modifier.tourTarget(tour, "board").fillMaxHeight().width(boardWidth))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        clueBar(Modifier.fillMaxWidth())
                        ClueLists(state.acrossClues, state.downClues, state.currentClue, onClue, Modifier.weight(1f))
                    }
                }
            } else if (maxHeight > maxWidth + CLUE_LISTS_MIN_HEIGHT) {
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    CrosswordBoard(state.board, onCell, Modifier.tourTarget(tour, "board").fillMaxWidth())
                    clueBar(Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    ClueLists(state.acrossClues, state.downClues, state.currentClue, onClue, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    BoardViewport(state.board, onCell, Modifier.weight(1f).fillMaxWidth().tourTarget(tour, "board"))
                    clueBar(Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            }
        }
    }
    if (revealing) {
        AlertDialog(
            onDismissRequest = { revealing = false },
            modifier = Modifier.dialogTestTags(),
            title = { Text(stringResource(R.string.show_the_solution)) },
            text = { Text(stringResource(R.string.this_ends_the_puzzle_it_won_t_count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        revealing = false
                        onRevealSolution()
                    },
                    modifier = Modifier.testTag("confirm_reveal"),
                ) { Text(stringResource(R.string.reveal_puzzle)) }
            },
            dismissButton = { TextButton(onClick = { revealing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (state.solved && state.resultSaved) {
        SolvedDialog(state, onNewPuzzle, onHome)
    }
}

private val RULES =
    listOf(
        "Fill the grid with the answers to the clues.",
        "Tap a cell to pick it. Tap it again to switch between across and down.",
        "Type the answer with the keyboard.",
    )

private val TUTORIAL =
    listOf(
        TourStep("board", "Tap a cell to pick it. Tap it again to switch between across and down, then type the answer."),
        TourStep("clue", "This is the clue for the word you're on. The arrows move to the previous or next clue."),
        TourStep("hint", "Stuck? The bulb points at the next step and explains it."),
        TourStep("menu", "Restart, new puzzle, reveal a letter, word or the puzzle, and settings live in this menu."),
    )

private val CLUE_LISTS_MIN_HEIGHT = 120.dp
private val WIDE_MIN_WIDTH = 600.dp

@Composable
private fun SolvedDialog(
    state: GameUiState,
    onNewPuzzle: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinDialog(
        dailyDate = state.dailyDate,
        title = stringResource(R.string.solved),
        accent = LocalCrosswordColors.current.accent,
        dailyGroup = "CROSSWORD",
        newLabel = state.levelLabel,
        newTag = "solved_new_puzzle",
        onNew = onNewPuzzle,
        newBest = state.newBest,
        stats =
            buildList {
                add(WinStat(stringResource(R.string.time), formatDuration(state.elapsedMs), "solved_time"))
            },
        modifier = modifier.dialogTestTags().testTag("solved_dialog"),
    ) {
        TextButton(onClick = onHome, modifier = Modifier.testTag("solved_home")) { Text(stringResource(R.string.home)) }
    }
}
