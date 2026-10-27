#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: e2e/run.sh [--fast] [--shards N] [--part I/N] [flow.yaml ...]"
  echo "  --fast      only flows tagged fast"
  echo "  --shards N  run flows in parallel on N emulators (local)"
  echo "  --part I/N  run the I-th of N slices of the flows (one CI matrix job each)"
}

cd "$(dirname "$0")/.."
APP_ID=com.joclegg.puzzles
AVD_PREFIX="${E2E_AVD_PREFIX:-puzzles-test}"
SHARDS=1
PART=""
TAGS=()
FLOWS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --fast) TAGS=(--include-tags=fast); shift ;;
    --shards) SHARDS="$2"; shift 2 ;;
    --part) PART="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) FLOWS+=("$1"); shift ;;
  esac
done

if [[ ${#FLOWS[@]} -eq 0 ]]; then
  ALL=($(ls e2e/flows/*.yaml | sort))
  if [[ -n "$PART" ]]; then
    INDEX="${PART%/*}"
    TOTAL="${PART#*/}"
    for i in "${!ALL[@]}"; do
      [[ $((i % TOTAL)) -eq $((INDEX - 1)) ]] && FLOWS+=("${ALL[$i]}")
    done
  else
    FLOWS=(e2e/flows)
  fi
fi

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

all_emulators() { adb devices | awk '/^emulator-[0-9]+\tdevice$/ {print $1}'; }
running_avd() { adb -s "$1" emu avd name 2>/dev/null | head -1 | tr -d '\r'; }
running() {
  if [[ -n "${CI:-}" ]]; then all_emulators; return; fi
  for serial in $(all_emulators); do
    if [[ "$(running_avd "$serial")" == "$AVD_PREFIX"* ]]; then
      echo "$serial"
    fi
  done
}

avd_name() { if [[ "$1" -eq 1 ]]; then echo "$AVD_PREFIX"; else echo "$AVD_PREFIX-$1"; fi; }

BUSY_AVDS=" "
for serial in $(running); do BUSY_AVDS+="$(running_avd "$serial") "; done
started=$(running | wc -l | tr -d ' ')
n=1
port=5554
while [[ $started -lt $SHARDS ]]; do
  avd="$(avd_name "$n")"
  n=$((n + 1))
  [[ "$BUSY_AVDS" == *" $avd "* ]] && continue
  AVD_NAME="$avd" e2e/setup-avd.sh >/dev/null 2>&1
  while adb devices | grep -q "emulator-$port"; do port=$((port + 2)); done
  emulator -avd "$avd" -port "$port" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect >"/tmp/$avd.log" 2>&1 &
  port=$((port + 2))
  started=$((started + 1))
done

until [[ $(running | wc -l | tr -d ' ') -ge $SHARDS ]]; do sleep 1; done
DEVICES=($(running | awk -v n="$SHARDS" 'NR <= n'))
for serial in "${DEVICES[@]}"; do
  adb -s "$serial" wait-for-device
  until [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do sleep 1; done
  for attempt in $(seq 120); do
    adb -s "$serial" shell pm path android >/dev/null 2>&1 && break
    [[ $attempt -eq 120 ]] && { echo "$serial never came up: no package manager" >&2; exit 1; }
    sleep 1
  done
  for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb -s "$serial" shell settings put global "$setting" 0
  done
  adb -s "$serial" shell settings put global hide_error_dialogs 1
  adb -s "$serial" shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null
done

./gradlew -q :app:assembleDebug
INSTALLS=()
for serial in "${DEVICES[@]}"; do
  adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null &
  INSTALLS+=($!)
done
for pid in "${INSTALLS[@]}"; do wait "$pid"; done

SHARD_ARGS=()
[[ ${#DEVICES[@]} -gt 1 ]] && SHARD_ARGS=(--shard-split "${#DEVICES[@]}")
DEVICE_LOST_MARKER="${E2E_DEVICE_LOST_MARKER:-}"
if maestro --device "$(IFS=,; echo "${DEVICES[*]}")" test ${SHARD_ARGS[@]+"${SHARD_ARGS[@]}"} ${TAGS[@]+"${TAGS[@]}"} "${FLOWS[@]}"; then
  exit 0
fi
for serial in "${DEVICES[@]}"; do
  if [[ "$(adb -s "$serial" get-state 2>/dev/null)" != "device" ]]; then
    echo "$serial was lost during the run" >&2
    [[ -n "$DEVICE_LOST_MARKER" ]] && touch "$DEVICE_LOST_MARKER"
  fi
done
exit 1
