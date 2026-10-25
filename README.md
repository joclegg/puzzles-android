# Puzzles

An offline puzzle app for Android with four games:

- **Sudoku**: classic and killer, from a 6x6 Fast level to Evil, graded by the solving techniques a puzzle needs. Notes, undo, mistake checking, a daily puzzle with medals, best times and history, and custom puzzle entry.
- **Solitaire**: Klondike, Draw 1 or Draw 3, with an optional reverse mode (kings first, columns build up). One tap to move, unlimited undo, a daily deal, stats and history.
- **Two Tone**: fill the grid red and blue by three simple rules (like 0h h1). Sizes 4 to 12, hints, a daily puzzle.
- **Sightlines**: blue dots count the dots they can see; red walls block the view (like 0h n0). Sizes 4 to 9, hints, a daily puzzle.

Fully offline: no account, no ads, no internet permission.

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
