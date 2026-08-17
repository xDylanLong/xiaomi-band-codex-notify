#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK}"
BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-34.0.0}"
BUILD_DIR="${TMPDIR:-/tmp}/band10pro-bridge-build"
APP_DIR="$ROOT_DIR/android-companion/app"
JAVA_BIN="$JAVA_HOME/bin"
SDK_PLATFORM="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
SDK_TOOLS="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS"
NOTIFICATION_ICON="$APP_DIR/src/main/res/drawable/ic_notification.png"
VERSION_CODE="${BAND_BRIDGE_VERSION_CODE:-8}"
VERSION_NAME="${BAND_BRIDGE_VERSION_NAME:-0.3.5}"
SIGNING_DIR="${BAND_BRIDGE_SIGNING_DIR:-$ROOT_DIR/android-companion/.signing}"
KEYSTORE="${BAND_BRIDGE_KEYSTORE:-$SIGNING_DIR/release.keystore}"
KEYSTORE_PASSWORD="${BAND_BRIDGE_KEYSTORE_PASSWORD:-android}"
KEY_PASSWORD="${BAND_BRIDGE_KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
KEY_ALIAS="${BAND_BRIDGE_KEY_ALIAS:-xiaomi-band-codex-notify}"
BASELINE_APK="${BAND_BRIDGE_BASELINE_APK:-$ROOT_DIR/android-companion/releases/小米手环Codex通知-v0.3.2.apk}"
APK_PATH="$ROOT_DIR/android-companion/releases/小米手环Codex通知-v${VERSION_NAME}.apk"

if [[ ! -f "$NOTIFICATION_ICON" ]]; then
  echo "Refusing to build: notification icon is missing at $NOTIFICATION_ICON." >&2
  exit 1
fi

certificate_from_apk() {
  "$SDK_TOOLS/apksigner" verify --print-certs "$1" 2>/dev/null \
    | awk -F': ' '/SHA-256 digest:/ {print tolower($2); exit}' \
    | tr -d ':'
}

certificate_from_keystore() {
  "$JAVA_BIN/keytool" -list -v -keystore "$KEYSTORE" \
    -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" 2>/dev/null \
    | awk -F': ' '/SHA256:/ {print tolower($2); exit}' \
    | tr -d ':'
}

rm -rf "$BUILD_DIR"
umask 077
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$BUILD_DIR/apk" "$BUILD_DIR/generated" \
  "$ROOT_DIR/android-companion/releases" "$(dirname "$KEYSTORE")"

"$SDK_TOOLS/aapt2" compile --dir "$APP_DIR/src/main/res" -o "$BUILD_DIR/compiled-res.zip"
"$SDK_TOOLS/aapt2" link \
  -o "$BUILD_DIR/apk/unsigned.apk" \
  --java "$BUILD_DIR/generated" \
  --manifest "$APP_DIR/src/main/AndroidManifest.xml" \
  -I "$SDK_PLATFORM" \
  --min-sdk-version 26 \
  --target-sdk-version 35 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay \
  -R "$BUILD_DIR/compiled-res.zip"
"$JAVA_BIN/javac" -source 8 -target 8 -classpath "$SDK_PLATFORM" -d "$BUILD_DIR/classes" \
  "$BUILD_DIR/generated"/com/example/bandbridge/*.java \
  "$APP_DIR"/src/main/java/com/example/bandbridge/*.java
"$SDK_TOOLS/d8" --lib "$SDK_PLATFORM" --output "$BUILD_DIR/dex" $(find "$BUILD_DIR/classes" -name '*.class' -print)
cp "$BUILD_DIR/apk/unsigned.apk" "$BUILD_DIR/apk/with-dex.apk"
(cd "$BUILD_DIR/dex" && zip -q "$BUILD_DIR/apk/with-dex.apk" classes.dex)

if [[ ! -f "$KEYSTORE" ]]; then
  if [[ -f "$BASELINE_APK" || "${BAND_BRIDGE_ALLOW_NEW_KEY:-0}" != "1" ]]; then
    echo "Refusing to generate a new signing key for an upgrade build." >&2
    echo "Restore the original keystore, or explicitly set BAND_BRIDGE_ALLOW_NEW_KEY=1 for a new-install release." >&2
    exit 1
  fi
  "$JAVA_BIN/keytool" -genkeypair -keystore "$KEYSTORE" \
    -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" \
    -alias "$KEY_ALIAS" -dname "CN=Xiaomi Band Codex Notify,O=Dylan,C=US" \
    -validity 10000 -keyalg RSA -keysize 2048 -noprompt
fi
chmod 600 "$KEYSTORE"

if [[ -f "$BASELINE_APK" ]]; then
  BASELINE_CERT="$(certificate_from_apk "$BASELINE_APK")"
  KEYSTORE_CERT="$(certificate_from_keystore)"
  if [[ -z "$BASELINE_CERT" || -z "$KEYSTORE_CERT" ]]; then
    echo "Unable to read the signing certificate from $BASELINE_APK or $KEYSTORE." >&2
    exit 1
  fi
  if [[ "$BASELINE_CERT" != "$KEYSTORE_CERT" ]]; then
    echo "Refusing to build: signing certificate does not match $BASELINE_APK." >&2
    echo "Expected: $BASELINE_CERT" >&2
    echo "Current:  $KEYSTORE_CERT" >&2
    echo "Use the original keystore; never create a new key for an upgrade release." >&2
    exit 1
  fi
fi

"$SDK_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/apk/with-dex.apk" "$BUILD_DIR/apk/aligned.apk"
"$SDK_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASSWORD" \
  --key-pass "pass:$KEY_PASSWORD" --ks-key-alias "$KEY_ALIAS" \
  --v4-signing-enabled false --out "$APK_PATH" "$BUILD_DIR/apk/aligned.apk"
"$SDK_TOOLS/apksigner" verify "$APK_PATH"
if [[ -f "$BASELINE_APK" ]]; then
  OUTPUT_CERT="$(certificate_from_apk "$APK_PATH")"
  if [[ "$OUTPUT_CERT" != "$BASELINE_CERT" ]]; then
    echo "Refusing to publish: final APK certificate does not match $BASELINE_APK." >&2
    echo "Expected: $BASELINE_CERT" >&2
    echo "Current:  $OUTPUT_CERT" >&2
    exit 1
  fi
fi
echo "Built $APK_PATH"
