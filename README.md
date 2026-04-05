# HOPPER

HOPPER is an Android app for railway wagon accounting. It helps photograph wagons, recognize wagon numbers from photos, organize them into collections, add notes, and share full journals with or without images.

## Main features

- Create separate collections for tracks, shifts, or work sessions
- Capture wagon photos directly inside the app
- Recognize wagon numbers with OCR
- Store wagon order, notes, directions, and wagon states (`PR` / `GR`)
- Reorder cards manually and keep a fixed journal order
- Search by collection name, journal description, wagon number, and notes
- Share wagon lists as text
- Export and import full Hopper collections with or without photos
- Adjust photo compression, theme, icon, and card appearance

## Built with

- Kotlin
- Jetpack Compose
- CameraX
- ML Kit Text Recognition
- Room
- Coil

## Package

- `com.alex.hopper`

## Download

Latest APK release:

- [HOPPER APK v0.1.0](https://github.com/AlexDukshanin/hopper/releases/tag/apk-v0.1.0)

Included file:

- `HOPPER-universal-debug.apk`

Installation notes:

- This is a universal debug APK and it can be installed directly on Android devices
- If Android blocks installation, allow installs from the browser or file manager you used to download the APK
- A signed release APK can be added later when a release keystore is configured

## Build locally

Build debug APK:

```bash
./gradlew assembleDebug
```

Build release APK:

```bash
./gradlew assembleRelease
```

Generated files:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Release notes

- [apk-v0.1.0 notes](docs/release-notes/apk-v0.1.0.md)

## Developer

- AlexDukshanin
