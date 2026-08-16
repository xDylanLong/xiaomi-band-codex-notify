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

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$BUILD_DIR/apk" "$ROOT_DIR/android-companion/releases"

"$JAVA_BIN/javac" -source 8 -target 8 -classpath "$SDK_PLATFORM" -d "$BUILD_DIR/classes" "$APP_DIR"/src/main/java/com/example/bandbridge/*.java
"$SDK_TOOLS/d8" --lib "$SDK_PLATFORM" --output "$BUILD_DIR/dex" $(find "$BUILD_DIR/classes" -name '*.class' -print)
"$SDK_TOOLS/aapt2" compile --dir "$APP_DIR/src/main/res" -o "$BUILD_DIR/compiled-res.zip"
"$SDK_TOOLS/aapt2" link \
  -o "$BUILD_DIR/apk/unsigned.apk" \
  --manifest "$APP_DIR/src/main/AndroidManifest.xml" \
  -I "$SDK_PLATFORM" \
  --min-sdk-version 26 \
  --target-sdk-version 35 \
  --version-code 1 \
  --version-name 0.1.0 \
  --auto-add-overlay \
  -R "$BUILD_DIR/compiled-res.zip"
cp "$BUILD_DIR/apk/unsigned.apk" "$BUILD_DIR/apk/with-dex.apk"
(cd "$BUILD_DIR/dex" && zip -q "$BUILD_DIR/apk/with-dex.apk" classes.dex)

KEYSTORE="$BUILD_DIR/debug.keystore"
if [[ ! -f "$KEYSTORE" ]]; then
  "$JAVA_BIN/keytool" -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -validity 10000 -keyalg RSA -keysize 2048
fi
"$SDK_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/apk/with-dex.apk" "$BUILD_DIR/apk/aligned.apk"
"$SDK_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --v4-signing-enabled false \
  --out "$ROOT_DIR/android-companion/releases/Band10ProBridge-debug.apk" "$BUILD_DIR/apk/aligned.apk"
"$SDK_TOOLS/apksigner" verify "$ROOT_DIR/android-companion/releases/Band10ProBridge-debug.apk"
echo "Built $ROOT_DIR/android-companion/releases/Band10ProBridge-debug.apk"
