# Puzzles

A puzzle app for Android that plays offline, with eight games:

- **Sudoku**: classic and killer, from a 6x6 Fast level to Evil, graded by the solving techniques a puzzle needs. Notes, undo, mistake checking, a daily puzzle with medals, best times and history, and custom puzzle entry.
- **Solitaire**: Klondike, Draw 1 or Draw 3, with an optional reverse mode (kings first, columns build up). One tap to move, unlimited undo, a daily deal, stats and history.
- **Two Tone**: fill the grid red and blue by three simple rules (like 0h h1). Sizes 4 to 12, hints, a daily puzzle.
- **Sightlines**: blue dots count the dots they can see; red walls block the view (like 0h n0). Sizes 4 to 9, hints, a daily puzzle.
- **Five Letters**: guess the five-letter word in six tries (like Wordle). Everyone with the app gets the same word each day, so you can share your grid. Practice words, hard mode, stats.
- **Kakuro**: cross sums from 6x6 to 12x12, with notes, an optional sum helper and a daily puzzle per size.
- **Slide Out**: slide the blocks to free the red one (like Unblock Me). Beginner to Expert, graded by the fewest moves, with par, hints and undo.
- **Word Wheel**: swipe the letters on the wheel to fill a small crossword (like Word Trip). Bonus words, shuffle and letter hints.

Plays fully offline: no account, no ads. The one exception is Five Letters' NY Times daily, which fetches that day's Wordle answer.

See [`AGENTS.md`](AGENTS.md) for commands and coding rules.

## Setup (macOS)

```sh
brew install openjdk@21 mobile-dev-inc/tap/maestro
brew install --cask android-commandlinetools
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0" "emulator"
```

Then `./gradlew :app:installDebug` with an emulator running, or `e2e/run.sh` to create and boot one and run the e2e flows.

## Credits

Word Wheel's word lists come from 12dicts by Alan Beale (public domain), with frequency data derived from AGID, Copyright 2000 by Kevin Atkinson. See `wordwheel/engine/data/NOTICE.md`.

