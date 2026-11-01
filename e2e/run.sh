#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: e2e/run.sh [--fast] [--shards N] [--part I/N] [flow.yaml ...]"
  echo "  --fast      only flows tagged fast"
  echo "  --shards N  run flows in parallel on N emulators (local)"
  echo "  --part I/N  run the I-th of N slices of the flows (one CI matrix job each), balanced by e2e/flow-times.txt"
  echo "  E2E_APK=path installs that APK instead of building one; E2E_LIST_ONLY=1 prints the flows and exits"
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

is_fast() { awk '/^---/ {exit} /^  - fast$/ {found=1} END {exit !found}' "$1"; }

partition() {
  local index="$1" total="$2"
  shift 2
  for flow in "$@"; do
    echo "$(basename "$flow" .yaml) $flow"
  done | awk -v index_="$index" -v total="$total" '
    NR == FNR { known[$1] = $2; next }
    { names[++n] = $1; paths[n] = $2 }
    END {
      count = 0
      for (name in known) { sorted[++count] = known[name] }
      for (i = 2; i <= count; i++) {
        value = sorted[i]
        for (j = i - 1; j >= 1 && sorted[j] > value; j--) { sorted[j + 1] = sorted[j] }
        sorted[j + 1] = value
      }
      median = count ? sorted[int((count + 1) / 2)] : 1
      for (i = 1; i <= n; i++) { cost[i] = (names[i] in known) ? known[names[i]] : median }
      for (i = 2; i <= n; i++) {
        c = cost[i]; nm = names[i]; pt = paths[i]
        for (j = i - 1; j >= 1 && (cost[j] < c || (cost[j] == c && names[j] > nm)); j--) {
          cost[j + 1] = cost[j]; names[j + 1] = names[j]; paths[j + 1] = paths[j]
        }
        cost[j + 1] = c; names[j + 1] = nm; paths[j + 1] = pt
      }
      for (part = 1; part <= total; part++) { load[part] = 0 }
      for (i = 1; i <= n; i++) {
        best = 1
        for (part = 2; part <= total; part++) { if (load[part] < load[best]) { best = part } }
        load[best] += cost[i]
        if (best == index_) { print paths[i] }
      }
    }' e2e/flow-times.txt -
}

if [[ ${#FLOWS[@]} -eq 0 ]]; then
  if [[ -n "$PART" ]]; then
    ALL=()
    for flow in $(ls e2e/flows/*.yaml | sort); do
      if [[ ${#TAGS[@]} -eq 0 ]] || is_fast "$flow"; then
        ALL+=("$flow")
      fi
    done
    FLOWS=($(partition "${PART%/*}" "${PART#*/}" "${ALL[@]}"))
    if [[ ${#FLOWS[@]} -eq 0 ]]; then
      echo "No flows in part $PART"
      exit 0
    fi
  else
    FLOWS=(e2e/flows)
  fi
fi

if [[ -n "${E2E_LIST_ONLY:-}" ]]; then
  printf '%s\n' "${FLOWS[@]}"
  exit 0
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

APK="${E2E_APK:-}"
if [[ -z "$APK" ]]; then
  e2e/fetch-banks.sh
  ./gradlew -q :app:assembleDebug
  APK=app/build/outputs/apk/debug/app-debug.apk
fi
INSTALLS=()
for serial in "${DEVICES[@]}"; do
  adb -s "$serial" install -r "$APK" >/dev/null &
  INSTALLS+=($!)
done
for pid in "${INSTALLS[@]}"; do wait "$pid"; done

SHARD_ARGS=()
[[ ${#DEVICES[@]} -gt 1 ]] && SHARD_ARGS=(--shard-split "${#DEVICES[@]}")
FLOWS_FAILED_MARKER="${E2E_FLOWS_FAILED_MARKER:-}"
MAESTRO_OUTPUT="$(mktemp)"
trap 'rm -f "$MAESTRO_OUTPUT"' EXIT
if maestro --device "$(IFS=,; echo "${DEVICES[*]}")" test ${SHARD_ARGS[@]+"${SHARD_ARGS[@]}"} ${TAGS[@]+"${TAGS[@]}"} "${FLOWS[@]}" 2>&1 | tee "$MAESTRO_OUTPUT"; then
  exit 0
fi
if grep -q "MaestroDriverStartupException" "$MAESTRO_OUTPUT"; then
  echo "The Maestro driver did not start on the emulator" >&2
  exit 1
fi
for serial in "${DEVICES[@]}"; do
  if [[ "$(adb -s "$serial" get-state 2>/dev/null)" != "device" ]]; then
    echo "$serial was lost during the run" >&2
    exit 1
  fi
done
[[ -n "$FLOWS_FAILED_MARKER" ]] && touch "$FLOWS_FAILED_MARKER"
exit 1
