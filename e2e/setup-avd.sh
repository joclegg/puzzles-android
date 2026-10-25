#!/usr/bin/env bash
set -euo pipefail

AVD_NAME="${AVD_NAME:-puzzles-e2e}"
API="${E2E_API:-35}"
ABI="${E2E_ABI:-$( [[ "$(uname -m)" == "arm64" ]] && echo arm64-v8a || echo x86_64 )}"
IMAGE="system-images;android-${API};google_apis;${ABI}"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --install "emulator" "platform-tools" "$IMAGE" >/dev/null
if ! emulator -list-avds | grep -qx "$AVD_NAME"; then
  echo "no" | avdmanager create avd --name "$AVD_NAME" --package "$IMAGE" --device "pixel_7" --force
fi
CONFIG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
sed -i.bak -e 's/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/' -e 's/^hw.gpu.mode=.*/hw.gpu.mode=host/' "$CONFIG" && rm -f "$CONFIG.bak"
echo "AVD ready: $AVD_NAME ($IMAGE)"
