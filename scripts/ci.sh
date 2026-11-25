#!/usr/bin/env bash
# Local pipeline, in order, stopping at the first failure:
#   1. Spotless + ktlint   2. unit tests   3. Compose UI tests   4. release build (R8)   5. build and install the debug app
#
# Usage:
#   ./scripts/ci.sh          check formatting (fails on violations)
#   ./scripts/ci.sh --fix    format the code first, then run the rest
#
# Needs a running emulator (or a connected device) for steps 3 and 5.
# The app itself needs the backend running to show data; the tests don't.
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="adb"
if ! command -v adb >/dev/null 2>&1; then
  ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
fi

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }

# Fail fast: no point running format and unit tests for minutes if the UI tests can't run.
if [[ "$("$ADB" get-state 2>/dev/null || true)" != "device" ]]; then
  echo "No emulator or device connected. Start one (e.g. from Android Studio's Device Manager) and retry." >&2
  exit 1
fi

if [[ "${1:-}" == "--fix" ]]; then
  step "1/5 Format (spotlessApply)"
  ./gradlew spotlessApply
else
  step "1/5 Format and lint (spotlessCheck)"
  ./gradlew spotlessCheck
fi

step "2/5 Unit tests, every module"
./gradlew testAndroidHostTest --continue

step "3/5 Compose UI tests on the device"
./gradlew connectedAndroidDeviceTest

# Builds only: catches R8 and shrinking problems the debug build never sees. Unsigned, never installed.
step "4/5 Release build (R8)"
./gradlew :androidApp:assembleRelease

step "5/5 Build and install the debug app"
./gradlew :androidApp:installDebug

printf '\n\033[1;32mPipeline passed.\033[0m Reports: <module>/build/reports/tests and androidTests.\n'
