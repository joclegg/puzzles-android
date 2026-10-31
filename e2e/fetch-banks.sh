#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
BASE=https://joclegg.github.io/puzzles-data
TARGET=e2e/banks/puzzles-data
PUZZLES_PER_BANK=200

rm -rf e2e/banks
mkdir -p "$TARGET"
curl -fsSL "$BASE/manifest.json" -o "$TARGET/manifest.json"
python3 -c 'import json,sys; print("\n".join(json.load(open(sys.argv[1]))["files"]))' "$TARGET/manifest.json" |
  while read -r file; do
    mkdir -p "$TARGET/$(dirname "$file")"
    case "$file" in
      words/* | wordwheel/*) curl -fsSL "$BASE/$file" -o "$TARGET/$file" ;;
      *) curl -fsSL "$BASE/$file" | { head -n "$PUZZLES_PER_BANK" > "$TARGET/$file"; cat > /dev/null; } ;;
    esac
  done
