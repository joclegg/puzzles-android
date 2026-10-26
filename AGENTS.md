# AGENTS.md

Puzzles: an offline Android app with five games: sudoku (classic and killer), Klondike solitaire, Two Tone (red/blue binary grid, like 0h h1), Sightlines (blue dots that count what they see, like 0h n0) Five Letters (a Wordle-style word game), Kakuro (cross sums), Slide Out (like Unblock Me) and Word Wheel (like Word Trip). A launcher screen picks the game.

## Stack

- Kotlin 2.4, Jetpack Compose with Material 3, single activity
- AGP 9 with built-in Kotlin, Gradle version catalog, convention plugins in `build-logic/`
- Room for saved games, DataStore for settings, manual DI in one container per game (`SudokuContainer`, `SolitaireContainer`, `TwoToneContainer`, `SightlinesContainer`, `FiveLettersContainer`, `KakuroContainer`, `SlideOutContainer`, `WordWheelContainer`)
- JUnit 5 with `kotlin.test` for the engines, JUnit 4 with Robolectric for Android and Compose code
- Maestro for e2e flows on the `puzzles-e2e` emulator
- Spotless with ktlint (`ktlint_official`) and the Compose rules, plus Android Lint

## Commands

- `./gradlew spotlessApply`: format the code
- `./gradlew spotlessCheck`: check formatting and the Compose rules
- `./gradlew lintDebug test assembleDebug`: run Android Lint, every unit test and a debug build
- `./gradlew :<game>:engine:test` (`sudoku`, `solitaire`, `twotone`, `sightlines`, `fiveletters`, `kakuro`, `slideout`, `wordwheel`): run only one game's engine tests
- `./gradlew :app:installDebug`: install the debug app on the running emulator
- `e2e/run.sh --shards 3`: run every Maestro flow in parallel on three emulators (boots any that aren't running)
- `e2e/run.sh --fast`: only the flows tagged `fast`, for everyday checks
- `e2e/run.sh e2e/flows/<flow>.yaml`: run one flow
- `e2e/run.sh --part I/N`: run one slice of the flows, as each CI matrix job does
- `./gradlew :sudoku:engine:generateBank -PbankSize=1000`: regenerate the bundled sudoku puzzles

## Structure

Each game has its own modules under `sudoku/`, `solitaire/`, `twotone/`, `sightlines/`, `fiveletters/`, `kakuro/`, `slideout/` and `wordwheel/`, with packages `com.joclegg.puzzles.<game>.*`:

- `<game>/engine/`: pure Kotlin, every game rule
  - sudoku: board model, `Solver`, `Grader`, `Generator`, `GameState` and `GameTimer`
  - solitaire: `Card`, `Board` (piles, legal moves, `BuildOrder`), `GameState` (taps, undo by replay), `BoardCodec`, `MoveCodec` and `GameTimer`
  - twotone and sightlines: `Grid`, `Puzzle`, a logic-only `Solver` whose next step is also the hint, `Generator` (seeded, keeps removing givens while the solver still finishes), `GameState` (tap, paint, undo, hints) and `GameTimer`
  - fiveletters: `Words` (answer and guess lists, the daily word by date), `score` and `GameState` (typing, guesses, hard mode, share text)
  - kakuro: `Puzzle` (layout, runs, clues), `Combinations` (digit sets per sum), `Solver` (counts solutions), `Generator` (layout, low/high-leaning fill, fewest starting digits) and `GameState` (select, digits, notes, undo)
  - slideout: `Board` and `Block`, a packed-number `Solver` (fewest moves, and the hardest position reachable from a set of blocks), `Generator` (hill-climbs over block sets) and `GameState` (slides, undo, hints)
  - wordwheel: `WordList` (valid and common words, British spellings), `Crossword` and `Placement`, `Generator` (lays out a crossword from one word's letters) and `GameState` (guesses, bonus words, shuffle, letter hints)
- `<game>/data/`: Room database and repositories. Sudoku also has `PuzzleBank` and `PuzzleSupply`; solitaire has `DealRef` (random and daily deals)
- `<game>/ui/`: theme, colour tokens (`BoardColors` for sudoku, `TableColors` for solitaire, `TileColors` for twotone and sightlines, `LetterColors` for fiveletters) and shared components
- `<game>/game/`, `<game>/home/`, `<game>/stats/`, `<game>/settings/`: the screens
- `core/testing/`: shared test utilities, such as `TestCompletionSource`
- `app/`: `MainActivity`, `PuzzlesApp.kt` (navigation), `LauncherScreen`, and per game a container (`<Game>Container`), a routes file (`<Game>Routes`) and debug-only e2e fixtures in `src/debug/` (`<Game>Fixtures`)
- `app/src/main/assets/puzzles/`: the generated sudoku bank, one file per mode and level
- `app/src/main/assets/kakuro/`: the generated Kakuro bank, one file per size (`./gradlew :kakuro:engine:generateBank`)
- `app/src/main/assets/slideout/`: the generated Slide Out bank, one file per level (`./gradlew :slideout:engine:generateBank`, which saves each puzzle as it's found and resumes)
- `app/src/main/assets/wordwheel/`: the Word Wheel bank and accepted words (`./gradlew :wordwheel:engine:generateBank`), built from the 12dicts lists in `wordwheel/engine/data/` (see its NOTICE)
- `app/src/main/assets/words/`: Five Letters word lists: `answers.txt` (hand-picked common words, British spelling, no plurals) and `guesses.txt` (the public-domain ENABLE list)
- `e2e/flows/`: Maestro flows, one behaviour per file, named `sudoku_*`, `solitaire_*`, `two_tone_*`, `sightlines_*` or `five_letters_*`, `kakuro_*`, `slide_out_*`, `word_wheel_*`
- `build-logic/`: convention plugins shared by every module

## Rules

Rules marked (lint) are enforced by `spotlessCheck`, Android Lint or the build.

### General

- MUST keep `spotlessCheck`, `lintDebug`, `test`, `assembleDebug` and `e2e/run.sh` passing.
- MUST run `spotlessCheck` in its own Gradle invocation. It races with tasks that write to `build/`.
- MUST format with `spotlessApply`. MUST NOT hand-format. (lint)
- MUST follow the official Kotlin style (`ktlint_official`). (lint)
- MUST NOT write comments, in code, build scripts, flows or scripts, except the Arrange/Act/Assert markers in unit tests. Name things so the code explains itself. (lint: `verifyNoComments`, run by `spotlessCheck`)
- MUST give every `if` and `else` a braced block that opens and closes on its own lines, including `if` used as an expression. MUST NOT write inline branches like `if (x) return` or `if (x) { y }`. (lint: `verifyBracedIfs`, run by `spotlessCheck`)
- MUST write commits in Conventional Commits style (`feat:`, `fix:`, `test:`, `build:`, `docs:`).
- Every merge to `main` publishes a release (`.github/workflows/release.yml`): GitVersion bumps the minor version, and the workflow tags it and attaches the signed APK and app bundle. Put `+semver: major` (or `+semver: patch`) in a commit message to opt in to a different bump. MUST NOT create `v*` tags by hand.

### Offline

- MUST keep the app fully offline. MUST NOT add the `INTERNET` or `ACCESS_NETWORK_STATE` permission. (lint: `verify<Variant>NoInternet`)
- MUST NOT add analytics, crash reporting, ads, cloud sync or any network library.
- MUST keep all data on the device, in Room or DataStore.
- MUST pick daily sudoku puzzles locally from the bundled bank, deal daily solitaire games locally, seeded by date (`DealRef.daily`), generate daily Two Tone and Sightlines puzzles locally, seeded by date (`PuzzleRef.daily`), and pick the daily Five Letters word locally from the bundled list (`Words.daily`). MUST NOT fetch another service's daily word.

### Modules

- MUST put each game rule in that game's `engine` module as a pure function on its `GameState`. ViewModels only orchestrate.
- MUST NOT add Android dependencies to an `engine` module.
- MUST NOT depend on one game's modules from the other game's modules. Only `app` and `core:testing` are shared.
- MUST read sudoku sizes from the puzzle's `Grid` (`puzzle.grid`, `Grid.STANDARD`, `Grid.FAST`). MUST NOT hard-code 9, 81 or 3x3 boxes.
- MUST read solitaire pile counts from `Board.COLUMNS` and `Board.FOUNDATIONS`, and ranks from `Card.ACE`..`Card.KING`.
- MUST put shared build configuration in a `build-logic` convention plugin. Module `build.gradle.kts` files stay a few lines.
- MUST declare every dependency and version in `gradle/libs.versions.toml`.
- MUST NOT depend on another feature module from a feature module.

### Screens

- MUST build each screen as `<X>Route` (wires the ViewModel), a stateless `<X>Screen(state, callbacks, modifier)` and an `<X>ViewModel`.
- MUST expose exactly one `StateFlow<<X>UiState>` from each ViewModel.
- MUST create ViewModels with `viewModel { ... }` from the game's container. MUST NOT add a DI framework.
- MUST give every composable that emits UI a `modifier: Modifier = Modifier` parameter. (lint)
- MUST name callback parameters `on<Action>` in the present tense, e.g. `onCellClick`. (lint)
- MUST wrap callbacks used inside effects in `rememberUpdatedState`. (lint)
- MUST NOT pass mutable state or ViewModels into stateless `<X>Screen` composables. (lint)
- MUST give every screen except the launcher a back arrow (`BackButton`) at the top left, including each game's Home.
- The app theme (System / Light / Dark) lives in sudoku's Settings and applies to the whole app. Each other game's routes file wraps its screens in that game's theme (`SolitaireTheme`, `TwoToneTheme`, `SightlinesTheme`, `FiveLettersTheme`, `KakuroTheme`, `SlideOutTheme`, `WordWheelTheme`).

### Sudoku look

- MUST take colours only from `LocalBoardColors` or `MaterialTheme`.
- MUST show given cells with a grey background. MUST draw given and entered digits in the same text colour.
- MUST draw every highlight in the blue palette: selected cell, same digit, peers, active digit, matching notes.
- MUST use red only for errors (wrong digits, wrong cages and clashing entries) and the trial mark (`trialMark`), a red box the player puts on a cell with the Mark tool.
- MUST use gold and silver (`medalGold`, `medalSilver`) only for daily medals.
- MUST define a dark-mode value for every board colour token.
- MUST NOT add a hint, auto-solve or cage-combination helper.
- MUST put every player preference in Settings, per mode (`ModeSettings`, `Toggle`) unless it only makes sense shared (input mode). Default to on unless there's a reason (grey starting cells are off in killer), and apply changes live.
- MUST make tool buttons icon-only, with a tooltip on long press that says what the button does. The tooltip is also the content description.
- MUST NOT duplicate a Settings choice as a button on the game screens. Cell first or number first lives only in Settings.
- MUST keep Undo and Redo in the game header, next to the ⋮ menu, not on the number pad. The pad holds the digits, Erase, Notes and Mark, six keys to a row. Kakuro uses the same layout.
- MUST put a Settings item in every game's ⋮ menu that opens that game's Settings tab.

### Solitaire look

- MUST take colours only from `LocalTableColors` or `MaterialTheme`.
- MUST draw every highlight (the selected card or run) in blue (`selected`).
- MUST use red only for red suits.
- MUST define a dark-mode value for every table colour token.
- MUST NOT add a hint or auto-solve.
- MUST put every player preference in Settings (`Settings`, `Toggle`). Default to on unless there's a reason, and apply changes live.
- MUST NOT duplicate a Settings choice as a button on the game screen.
- MUST only report a deal as unwinnable when `Solver` has searched every move (`Verdict.UNWINNABLE`). Out of time or budget is `UNKNOWN`, and says nothing.
- MUST run the solver off the main thread with a time limit (`CHECK_LIMIT_MS`).

### Two Tone and Sightlines look

- MUST take colours only from `LocalTileColors` or `MaterialTheme`, with a dark-mode value for every token.
- MUST show hints as the next logical step from `Solver.next`, or the wrong spots when the player has made a mistake. Shade what the rule looks at in `hintLine`, outline the spots to fill in `hintTarget`, and explain it in words.
- MUST offer both input modes in Settings (`InputMode`): Tap to cycle, and Pick a colour (the `ColourPicker` under the board). Painting a spot with its own colour clears it.
- MUST generate every puzzle so `Solver.solveLogically` finishes it: no guessing, and exactly one solution.

### Two Tone and Sightlines games

- MUST keep at most one game in progress per save slot: each size, and one per size per date for the daily puzzles.
- MUST keep undo unlimited by saving every change (`ChangeCodec`) and replaying them.

### Five Letters

- MUST take colours only from `LocalLetterColors` or `MaterialTheme`, with dark and high-contrast values.
- MUST give everyone the same daily word on the same date: `Words.daily` shuffles the answers with a fixed seed and counts days from `Words.FIRST_DAY`. MUST NOT reorder or remove answers once released; only append.
- MUST keep Share to the coloured grid only, never the letters.
- MUST keep one game per date for the daily word (it can't be replayed once finished) and one practice game in progress.

### Kakuro

- MUST take colours only from `LocalKakuroColors` or `MaterialTheme`.
- MUST get puzzles from the bundled bank (`PuzzleBank`), regenerated with `generateBank`, never by hand. The daily puzzle per size is picked from the bank by date.
- MUST give every puzzle exactly one solution, using as few starting digits (`givens`, shown on grey cells) as the generator can find.
- MUST keep the sum helper (`SHOW_COMBINATIONS`) off by default.
- MUST offer the same entry options as sudoku: Cell first or Number first (`InputMode`), and mistake checking Off, Block clashes or Show wrong (`MistakeCheck`), with a mistake counter.
- Database changes MUST come with a Room migration (Kakuro is at version 2, auto-migrated from 1) and a migration test.

### Slide Out

- MUST take colours only from `LocalSlideColors` or `MaterialTheme`.
- MUST get puzzles from the bundled bank, graded by fewest moves (`Level`), with par shown from the solver. Each slide of one block is one move, however far it goes.
- Expert generation is slow: run it on GitHub Actions by pushing to `generate/slide-out`. Each of 16 runners saves to `generate/slide-out-results-<n>` every 10 minutes and resumes from it.

### Word Wheel

- MUST take colours only from `LocalWheelColors` or `MaterialTheme`.
- MUST only use common, British-spelt words from the frequency list for crossword answers, and never words in `wordwheel/engine/data/blocked.txt`. Any word in 3of6game counts as a bonus.
- MUST support both swiping across the wheel and tapping letters then the tick.

### Sudoku games

- MUST keep at most one game in progress per save slot: each mode and level, the daily puzzle per mode, and custom. Starting a game abandons only the game in its own slot.
- MUST offer Resume or New game when a level with a game in progress is tapped.
- MUST keep undo unlimited and save the full move history with the game, so undo works after resuming.
- While the first version is being built on the `app-foundation` branch: MUST NOT write migrations, version bumps, fallbacks or any backwards compatibility. The database stays at version 1 and its schema changes in place, and every install on a device is fresh (`adb uninstall com.joclegg.puzzles` first). Once that branch is merged, schema changes need proper handling again.

### Solitaire games

- MUST keep at most one game in progress per save slot: one for new games, and one per date for the daily deal. Starting a game abandons only the game in its own slot.
- MUST offer Resume or New deal when New game is tapped with a game in progress.
- MUST keep Draw 1 or Draw 3, and Reverse, as Settings preferences, not choices on Home. Save them with each game (`DealRef`), so changes only affect new games.
- MUST keep undo unlimited by saving the deal and the full move list, and replaying them. MUST NOT store board snapshots.
- While the first version is being built on the `app-foundation` branch: MUST NOT write migrations, version bumps, fallbacks or any backwards compatibility. The database stays at version 1 and its schema changes in place, and every install on a device is fresh (`adb uninstall com.joclegg.puzzles` first). Once that branch is merged, schema changes need proper handling again.

### Sudoku puzzles

- MUST grade classic puzzles by the hardest technique the `Grader` needs.
- MUST grade killer puzzles by the grader's score bands (`Generator.killerScoreBands`), with cage profiles per level. Killer has seven levels (`KILLER_LEVELS`), classic five (`CLASSIC_LEVELS`); Fast is 6x6 and listed first in both.
- MUST regenerate the bank with `generateBank`. MUST NOT edit the puzzle files by hand.
- MUST get new puzzles through `PuzzleSupply`: bundled first, then the generated stock, then one made on the spot. Top-ups run in `PuzzleTopUpWorker` only when a level's stock falls below `LOW_WATERMARK`.
- MUST keep custom puzzles out of best times.
- MUST NOT add any screen, dialog or option for entering past best times. Past bests from other apps go straight into the `past_bests` table on the device.

### Tests

- MUST unit test every rule in each game's `engine` with JUnit 5 and `kotlin.test`.
- MUST name unit tests `given <state> when <action> then <outcome>`, in backticks.
- MUST parameterise unit tests that check one rule with several inputs: `@ParameterizedTest` with `@CsvSource`, `@EnumSource` or `@MethodSource` in JUnit 5; the `Parameterized` or `ParameterizedRobolectricTestRunner` runner in JUnit 4.
- MUST split each unit test into `// Arrange`, `// Act` and `// Assert` sections (Arrange may be left out when there is nothing to set up). These markers are the only comments allowed anywhere, and only in `src/test`. (lint: `verifyNoComments`)
- MUST test Compose UI with Robolectric, asserting on semantics (sudoku: `CellHighlightKey`, `CellGivenKey`, `CellWrongKey`, `CageWrongKey`; solitaire: `CardSelectedKey`, `CardFaceUpKey`; Two Tone: `TileKey`, `TileGivenKey`, `TileBrokenKey`, `TileHintKey`; Sightlines: `DotKey`, `DotGivenKey`, `DotBrokenKey`, `DotHintKey`). MUST NOT assert on pixels or colours.
- MUST test timer logic with a fake clock.
- MUST make tests of concurrent code deterministic: background work signals when it is done (a `TestCompletionSource` from `core:testing`, completed by the fake or callback that observes the work) and the test awaits that signal. MUST NOT wait with sleeps, delays, polling or real-time timeouts. The source's timeout is only a safety net that fails the build when a signal never comes.
- MUST pass coroutine dispatchers into classes that switch threads (as `GameRepository` takes `background`), so tests can run that work on the test dispatcher.
- MUST give every element an e2e flow touches a `testTag`: sudoku `cell_r{row}c{col}` (1-based) and `digit_{n}`; Five Letters `letter_r{row}c{col}`, `key_{A-Z}`, `key_enter` and `key_delete`; Two Tone `tile_r{row}c{col}`; Sightlines `dot_r{row}c{col}`; solitaire `card_{rank}{suit}` (e.g. `card_TH`), `tableau_{n}`, `foundation_{n}`, `tableau_{n}_down_{i}`, `stock` and `waste`; snake_case names elsewhere.
- MUST apply `Modifier.dialogTestTags()` to dialogs, so their tags reach Maestro.
- MUST write one behaviour per Maestro flow in `e2e/flows/<game>_<behaviour>.yaml`.
- MUST start each flow with `launchApp: clearState: true`, then `tapOn: game_<game>` on the launcher. Use a debug fixture instead, which opens the game directly, (`arguments: fixture: <name>`, or `draft: <name>` for custom entry) when the flow needs a known board. MUST NOT type long inputs tap by tap; add a fixture.
- MUST keep e2e flows to wiring: screens, navigation and persistence. Rule edge cases belong in unit tests.
- MUST tag quick, core flows `fast`.
- MUST NOT add data backups or other slow steps to `e2e/run.sh`: emulator data is disposable and the suite must stay fast.
- MUST add new fixtures to `app/src/debug/.../<Game>Fixtures.kt`, with names unique across all games. MUST NOT ship fixtures in release.
- MUST assert a cell's digit with `childOf: { id: cell_rXcY }`.
- MUST run e2e flows on the emulator only, never a physical device.
- MUST NOT run e2e flows on the windowed emulator kept for manual testing (`puzzles-e2e`). `e2e/run.sh` uses its own headless `puzzles-test*` emulators; in CI it uses whichever emulator the runner started.
- MUST NOT wait with fixed sleeps in flows. Assert on the expected state instead.

