#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_SCRIPT="$ROOT_DIR/android-companion/build-local.sh"
GITIGNORE="$ROOT_DIR/.gitignore"

grep -q 'BAND_BRIDGE_SIGNING_DIR' "$BUILD_SCRIPT"
grep -q 'BAND_BRIDGE_KEYSTORE' "$BUILD_SCRIPT"
grep -q -- '--java "$BUILD_DIR/generated"' "$BUILD_SCRIPT"
grep -q '"$BUILD_DIR/generated"' "$BUILD_SCRIPT"
if grep -q 'KEYSTORE="$BUILD_DIR/debug.keystore"' "$BUILD_SCRIPT"; then
  echo "signing keystore must not live in the disposable build directory" >&2
  exit 1
fi
grep -q '^android-companion/.signing/$' "$GITIGNORE"

echo "Signing configuration uses a persistent, ignored keystore path."
