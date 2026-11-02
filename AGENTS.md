# AGENTS.md

## Structure

Puzzles is a single-activity Android app with eight games: Sudoku (`sudoku`), Solitaire (`solitaire`), Two Tone (`twotone`), Sightlines (`sightlines`), Five Letters (`fiveletters`), Kakuro (`kakuro`), Slide Out (`slideout`) and Word Wheel (`wordwheel`). Every game has the same four modules and the same files in each:

- `<game>/engine/`: pure Kotlin, package `com.joclegg.puzzles.<game>.engine`. Every game rule: the puzzle model, `GameState` (moves, undo, hints), and where the game has them a `Solver`, `Generator` and `generateBank` task. Sudoku also has its `Grader`, Solitaire its `Board`, `Card` and codecs, Five Letters its `Words`, Word Wheel its `WordList` and `Crossword`.
- `<game>/data/`: package `com.joclegg.puzzles.<game>.core.data` (kept so the Room schema exports in `<game>/data/schemas/` stay as they are). `GameDatabase` and `GameDao` (Room), `GameRepository` (saved games, history, stats), `SettingsRepository` (DataStore), `Models.kt`, and the game's `PuzzleBank` where it reads a downloaded bank. Sudoku also has `PuzzleSupply` and its top-up stock; Solitaire has `DealRef` and `WinnableDeals`; Five Letters has `Daily2WordSource`.
- `<game>/ui/`: package `com.joclegg.puzzles.<game>.ui`. `Theme.kt` with `<Game>Colors` and `Local<Game>Colors`, `<Game>Icons`, `<Game>GameIcon`, `Format.kt`, `Help.kt`, `TestTags.kt`, `Controls.kt` and the board composables.
- `<game>/feature/`: package `com.joclegg.puzzles.<game>.feature.<screen>`, one package per screen: `game` (`GameScreen`, `GameViewModel`), `home` (`HomeScreen`, `HomeViewModel`) and `settings` (`SettingsScreen`, `SettingsViewModel`). Sudoku's `game` also holds custom puzzle entry (`EntryScreen`, `EntryViewModel`).
- `app/.../<game>/`: `<Game>Definition.kt` (the game's `GameDefinition`: database, repositories, sync source, bank folder, home route, nav graph, accent and settings tab), `<Game>Routes.kt` (the game's screens in the nav graph, wrapped in its theme), `<Game>Sync.kt` (its `SyncSource`, rows and settings to and from the JSON puzzles-web and puzzles-ios use) and `<Game>Stats.kt` (its stats table and history filters for the shared screens). Debug-only e2e fixtures live in `app/src/debug/.../<game>/<Game>Fixtures.kt`.
- `app/`: `MainActivity`, `PuzzlesApplication` (the list of `GameDefinition`s, sync, bank downloads), `PuzzlesApp.kt` (navigation), `GameDefinition.kt`, `LauncherScreen`, `SettingsHub`, `BankGate` and `BankDownloadScreen`, daily calendars and medals (`TodaysDailies`, `DailyCalendarRoute`), `CombinedHistory` and sign-in.
- `core/ui/`: shared Compose code. `home/` (`GameHomeScaffold`, `StandardHomeSections`, `rememberGameLauncher`), `stats/` (`StatsRoute`, `HistoryRoute`, `StatsViewModel`, `StatsSource`) and the menu pieces (`GameHero`, `ContinueCard`, `MenuSection`, `MenuTile`, `DailyTile`, `TileGrid`, `LevelMeter`, `MenuIcons`, `BackButton`, `WinDialog`, `DailyCalendarButton`, `formatDuration`).
- `core/data/`: pure Kotlin data helpers shared by the games: `TextPuzzleBank` (one puzzle per line, cached per key) and `GameTimer`.
- `core/sync/`: optional sign-in and sync with the Supabase backend (`SupabaseAuth`, `SyncEngine`, `SyncStore`, `StampedPreferences`) and `BankStore`, which downloads the puzzle banks published by puzzles-data.
- `core/testing/`: shared test utilities, such as `TestCompletionSource`.
- `e2e/`: Maestro flows (`e2e/flows/<game>_<behaviour>.yaml`), `run.sh`, `fetch-banks.sh` and `flow-times.txt`.
- `build-logic/`: convention plugins for every module, including the `verifyNoComments` and `verifyBracedIfs` checks run by `spotlessCheck`.
- `puzzles-data/` (gitignored): where generator tasks write banks by default. The real banks live in [joclegg/puzzles-data](https://github.com/joclegg/puzzles-data).

## Conventions

- MUST keep `spotlessCheck`, `lintDebug`, `test`, `assembleDebug` and `e2e/run.sh` passing.
- MUST run `spotlessCheck` in its own Gradle invocation, because it races with tasks that write to `build/`.
- MUST format with `spotlessApply` and follow the official Kotlin style (`ktlint_official`).
- MUST NOT write comments in code, build scripts, flows or scripts, except the `// Arrange`, `// Act` and `// Assert` markers in unit tests.
- MUST give every `if` and `else` a braced block that opens and closes on its own lines, including `if` used as an expression.
- MUST write commits in Conventional Commits style (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `docs:`).
- MUST NOT create `v*` tags by hand: every merge to `main` publishes a release through `.github/workflows/release.yml`.
- MUST give each game exactly one `GameDefinition`, listed in `PuzzlesApplication.games`, and reach the game's routes, sync, banks, accent and settings through it rather than through per-game switches.
- MUST keep the same modules, packages, file names and roles in every game, as described in Structure.
- MUST use the shared screens and helpers in `core/ui` and `core/data` (stats, history, home scaffold, puzzle bank, timer) instead of per-game copies, and move code to `core` when a second game needs it.
- MUST NOT depend on one game's modules from another game's modules. Only `app` and the `core` modules are shared.
- MUST NOT depend on a `feature` module from another `feature` module.
- MUST NOT add Android dependencies to an `engine` module or to `core/data`.
- MUST put each game rule in that game's `engine` as a pure function on its `GameState`; ViewModels only orchestrate.
- MUST put shared build configuration in a `build-logic` convention plugin and declare every dependency and version in `gradle/libs.versions.toml`.
- MUST follow unidirectional data flow: state flows down from the ViewModel, events flow up as `on<Action>` callbacks.
- MUST build each screen as `<X>Route` (wires the ViewModel), a stateless `<X>Screen(state, callbacks, modifier)` and an `<X>ViewModel`, hoisting state out of the screen.
- MUST NOT put business logic in composables, or pass mutable state or ViewModels into stateless `<X>Screen` composables.
- MUST expose exactly one immutable `StateFlow<<X>UiState>` from each ViewModel and keep its `MutableStateFlow` private.
- MUST create ViewModels with `viewModel { ... }` from the game's `GameDefinition`. MUST NOT add a DI framework.
- MUST give every composable that emits UI a `modifier: Modifier = Modifier` parameter, and wrap callbacks used inside effects in `rememberUpdatedState`.
- MUST pass coroutine dispatchers into classes that switch threads, with the real dispatcher as the default, so tests can inject a test dispatcher.
- MUST NOT use `GlobalScope`, or `runBlocking` outside tests and debug e2e fixtures.
- MUST put new and changed user-facing text in string resources.
- MUST take colours only from the game's `Local<Game>Colors` or `MaterialTheme`, with a dark-mode value for every colour token.
- MUST NOT change a Room entity's persisted shape, a DataStore key or a sync JSON field without a migration plan shared with puzzles-web and puzzles-ios.
- MUST keep every game playable offline and signed out. MUST NOT add network calls beyond sync, bank downloads and the Five Letters Daily puzzle 2 word.
- MUST NOT add analytics, crash reporting, ads or any network library; use the JDK's `HttpURLConnection`.
- MUST NOT add the `ACCESS_NETWORK_STATE` permission.
- MUST read every puzzle bank through `BankStore.read`, and change banks in puzzles-data by regenerating them with `generateBank`, never by hand.
- MUST give every saved game a `uuid` and an `updatedAt`, and save through `GameDao.save`, which keeps the row's `uuid`.
- MUST keep at most one game in progress per save slot, and keep undo unlimited by saving the full move history.
- MUST pick or generate daily puzzles locally, seeded by date, so everyone gets the same daily puzzle.
- MUST NOT reorder or remove released Five Letters answers; only append.
- MUST NOT add a hint, auto-solve or cage-combination helper to Sudoku, or a hint or auto-solve to Solitaire beyond Finish.
- MUST put every player preference in Settings, with helpers off by default, and MUST NOT duplicate a Settings choice as a button on a game screen.
- MUST give every screen except the launcher a `BackButton` at the top left, and each game's Home a `DailyCalendarButton`.
- MUST write unit tests for shared `core` code and for every rule in each game's `engine` (JUnit 5 with `kotlin.test`).
- MUST name unit tests `given <state> when <action> then <outcome>`, in backticks, and parameterise tests that check one rule with several inputs.
- MUST test Compose UI with Robolectric, asserting on semantics, never pixels or colours.
- MUST test timer logic with a fake clock, and make concurrent tests deterministic with `TestCompletionSource` instead of sleeps, delays or polling.
- MUST NOT change existing `testTag`s or Maestro ids. MUST give every element an e2e flow touches a `testTag`, and apply `Modifier.dialogTestTags()` to dialogs.
- MUST write one behaviour per Maestro flow in `e2e/flows/<game>_<behaviour>.yaml`, start it with `launchApp: clearState: true`, and keep flows to wiring: screens, navigation and persistence.
- MUST NOT wait with fixed sleeps in flows, or run them on a physical device or on the windowed `puzzles-e2e` emulator.
- MUST add new e2e fixtures to `app/src/debug/.../<Game>Fixtures.kt` with names unique across games. MUST NOT ship fixtures in release.
