# Puzzles

Puzzles is an Android app with eight puzzle games. It plays fully offline, has no ads and needs no account. Signing in with Google, Microsoft or email is optional and syncs saved games and settings with the website and the iOS app.

## The games

- **Sudoku**: classic and killer, from a 6x6 Fast level up to Evil, graded by the techniques each puzzle needs. Notes, undo, mistake checking, custom puzzle entry and best times.
- **Solitaire**: Klondike with Draw 1 or Draw 3 and an optional reverse mode. One tap to move and unlimited undo.
- **Two Tone**: fill the grid red and blue by three simple rules.
- **Sightlines**: blue dots count the dots they can see, and red walls block the view.
- **Five Letters**: guess the five-letter word in six tries. Everyone gets the same word each day.
- **Kakuro**: cross sums, with notes and an optional sum helper.
- **Slide Out**: slide the blocks to free the red one, graded by the fewest moves.
- **Word Wheel**: swipe the letters on the wheel to fill a small crossword.

Every game has a daily puzzle with medals, stats and history.

## How it's built

- Kotlin and Jetpack Compose with Material 3, in a single activity.
- Room for saved games and DataStore for settings, wired by hand with one `GameDefinition` per game.
- A module set per game (`engine`, `data`, `ui`, `feature`) plus shared `core` modules for the common screens, data helpers, sync and test utilities.
- Puzzle banks and word lists come from [puzzles-data](https://github.com/joclegg/puzzles-data). The app downloads them in the background, so new puzzles need no release.
- Optional sync through the shared Supabase backend in [puzzles-backend](https://github.com/joclegg/puzzles-backend).
- Gradle with a version catalog and convention plugins in `build-logic/`. Spotless with ktlint and the Compose rules, plus Android Lint.
- Tests: JUnit 5 for the pure game engines, JUnit 4 with Robolectric for Android and Compose code, and Maestro flows for end-to-end checks.

See [`AGENTS.md`](AGENTS.md) for the project layout and conventions.

## Build, run and test

You need a JDK, the Android SDK and, for end-to-end flows, Maestro and an emulator.

```sh
./gradlew assembleDebug            # build the debug app
./gradlew :app:installDebug        # install it on a running emulator
./gradlew spotlessApply            # format the code
./gradlew spotlessCheck            # check formatting and the Compose rules
./gradlew lintDebug test           # Android Lint and every unit test
./gradlew :<game>:engine:test      # one game's engine tests
e2e/run.sh --fast                  # the quick Maestro flows
e2e/run.sh --shards 3              # every Maestro flow, on three emulators
e2e/run.sh e2e/flows/<flow>.yaml   # one flow
e2e/run.sh --part 1/5              # one CI slice, balanced by e2e/flow-times.txt
```

Puzzle generators run as Gradle tasks, for example `./gradlew :kakuro:engine:generateBank`. They write to a local `puzzles-data/` folder; commit bank changes to puzzles-data.

## Credits

Word Wheel's word lists come from 12dicts by Alan Beale (public domain), with frequency data derived from AGID, Copyright 2000 by Kevin Atkinson. See `wordwheel/engine/data/NOTICE.md`.
