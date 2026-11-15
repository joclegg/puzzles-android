#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
BASE=https://joclegg.github.io/puzzles-data
TARGET=e2e/banks/puzzles-data
PUZZLES_PER_BANK=200
LOCAL="${PUZZLES_DATA_DIR:-}"

fetch() {
  if [[ -n "$LOCAL" ]]; then
    cat "$LOCAL/$1"
  else
    curl -fsSL "$BASE/$1"
  fi
}

rm -rf e2e/banks
mkdir -p "$TARGET"
if [[ -n "$LOCAL" ]]; then
  (cd "$LOCAL" && python3 scripts/manifest.py "$OLDPWD/$TARGET/manifest.json")
else
  curl -fsSL "$BASE/manifest.json" -o "$TARGET/manifest.json"
fi
python3 -c 'import json,sys; print("\n".join(json.load(open(sys.argv[1]))["files"]))' "$TARGET/manifest.json" |
  while read -r file; do
    mkdir -p "$TARGET/$(dirname "$file")"
    case "$file" in
      words/* | wordwheel/* | crossword/clues.txt) fetch "$file" > "$TARGET/$file" ;;
      *) fetch "$file" | { head -n "$PUZZLES_PER_BANK" > "$TARGET/$file"; cat > /dev/null; } ;;
    esac
  done
