# Codex · Xiaomi Bound Logo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current product logo and Android notification icon with a flat Apple-style Codex · Xiaomi mark connected by one band, while preserving the existing notification and LAN bridge behavior.

**Architecture:** Generate one transparent full-color PNG for product surfaces and synchronize it byte-for-byte across the web app, Android launcher, and plugin assets. Add a deterministic monochrome Android notification glyph for `smallIcon`, load the color logo as `largeIcon`, and keep all existing notification payload, channel, bridge, and pairing behavior unchanged.

**Tech Stack:** OpenAI built-in image generation, PNG assets, Android Java `Notification.Builder`, Android drawable XML/PNG, Gradle Android build, shell-based asset verification.

## Global Constraints

- Style is flat, restrained, rounded geometric, Apple-inspired, with clear whitespace and no 3D modeling, complex shadows, or glossy lighting.
- Codex and Xiaomi symbols must both appear and be connected by one continuous band.
- The product logo is transparent-background, square, full-color, and shared by product, Android launcher, and plugin surfaces.
- The notification glyph is a high-contrast monochrome silhouette suitable for Android `smallIcon`.
- Do not change `applicationId`, package name, LAN protocol, pairing flow, notification permissions, or existing notification payload behavior.
- Do not claim that Mi Fitness or the Xiaomi band will necessarily display the color logo; distinguish code/resource verification from real-device verification.

## Files and Responsibilities

- Create: `android-companion/app/src/main/res/drawable/ic_notification.xml` — deterministic white notification silhouette.
- Modify: `assets/logo.png` — full-color product logo.
- Modify: `android-companion/app/src/main/res/drawable/logo.png` — byte-identical Android launcher logo.
- Modify: `plugins/xiaomi-band-codex-notify/assets/logo.png` — byte-identical plugin logo.
- Modify: `android-companion/app/src/main/java/com/example/bandbridge/NotificationPublisher.java` — project `smallIcon` and color `largeIcon` wiring.
- Modify: `docs/usage.md` and `README.md` — only if current wording needs to describe the new icon boundary; keep claims factual.
- Test/build: `android-companion/` — existing Gradle build and APK inspection.

### Task 1: Generate and select the full-color logo

**Files:**
- Create in workspace: `assets/logo.png`
- Synchronize: `android-companion/app/src/main/res/drawable/logo.png`
- Synchronize: `plugins/xiaomi-band-codex-notify/assets/logo.png`

**Interfaces:**
- Produces a transparent PNG that all product surfaces consume.
- The final file must contain both Codex and Xiaomi symbols and one connecting band.

- [x] Generate a square, transparent, flat Apple-style logo with no text, watermark, extra device, or background scene.
- [x] Inspect the generated image for both marks, the connecting band, clean alpha edges, and legibility at small size.
- [x] Copy the selected image to all three product paths and verify the files are byte-identical.

### Task 2: Add the Android notification glyph

**Files:**
- Create: `android-companion/app/src/main/res/drawable/ic_notification.xml`

**Interfaces:**
- Produces a valid Android drawable resource named `ic_notification`.
- The resource is a transparent, white, high-contrast silhouette derived from the same two-symbol-and-band concept.

- [x] Define a 24dp vector drawable with a solid white silhouette, rounded geometry, and no color-dependent detail.
- [x] Keep the glyph simple enough to survive Android status-bar masking and Mi Fitness notification forwarding.
- [x] Run Android resource validation through the existing manual Android build.

### Task 3: Wire notification publishing to project assets

**Files:**
- Modify: `android-companion/app/src/main/java/com/example/bandbridge/NotificationPublisher.java`

**Interfaces:**
- `serviceNotification(Context)` continues returning the foreground-service `Notification`.
- `publish(Context, String, String, String, String)` keeps the existing signature and payload behavior.

- [x] Replace both `android.R.drawable.ic_dialog_info` references with a project resource lookup for `ic_notification`, preserving compatibility with the manual build's lack of generated `R.java`.
- [x] Decode the `logo` resource through a private helper and pass it to `setLargeIcon` without changing title/body/style logic.
- [x] Keep BigText, BigPicture, pending intent, category, priority, channel IDs, and notification IDs unchanged.
- [x] Compile the Android module and build the APK successfully.

### Task 4: Verify product/resource integration

**Files:**
- Inspect: `index.html`, `MainActivity.java`, `AndroidManifest.xml`, plugin metadata, README/usage docs.

**Interfaces:**
- All existing logo consumers resolve to the synchronized main logo.
- Android application launcher remains `@drawable/logo`.

- [x] Search for stale logo paths, default info icon references, and duplicate old logo files outside release APKs.
- [x] Verify the three main PNG files have the same SHA-256 and preserve alpha.
- [x] Verify the notification Java source references only the project glyph for `smallIcon`.
- [x] Confirm no documentation change is needed beyond the design/implementation records.

### Task 5: Build and report the real verification boundary

**Files:**
- Build output under `android-companion/app/build/` using the existing Gradle configuration.

**Interfaces:**
- Produces a successfully built debug APK without changing package/application identity.

- [x] Run the existing Android build command from `android-companion/`.
- [x] Inspect the APK manifest for the unchanged application ID and app label.
- [x] Confirm the APK contains `logo` and `ic_notification` resources and no build errors.
- [x] Report separately what was verified statically, what was verified by APK build, and what still requires a physical Android phone, Mi Fitness, and Xiaomi band.

## Commit Checkpoints

1. Commit the generated/synchronized logo assets.
2. Commit the notification glyph and Java integration.
3. Commit documentation adjustments and verified build output references if needed.

## Self-Review

- Spec coverage: visual direction, shared product asset, Android monochrome constraint, `largeIcon`, non-regression boundaries, and real-device verification are covered by Tasks 1–5.
- Placeholder scan: no TBD/TODO or unspecified implementation step is required.
- Type/resource consistency: Java references `R.drawable.ic_notification` and `R.drawable.logo`, both defined in the Android module; public method signatures remain unchanged.
