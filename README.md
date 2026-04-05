# HOPPER

HOPPER is an Android app for photographing rail wagons, recognizing wagon numbers from photos, and organizing them into collections with notes, direction labels, and export options.

## What the app does

- Creates separate collections for different tracks, tasks, or shifts
- Takes wagon photos directly inside the app
- Recognizes wagon numbers with OCR
- Stores wagon order, notes, states (`PR` / `GR`), and journal descriptions
- Lets you copy, share, and import full Hopper collections with or without photos
- Supports manual card reordering, search, themes, and photo compression settings

## Built with

- Kotlin
- Jetpack Compose
- CameraX
- ML Kit Text Recognition
- Room
- Coil

## Current package

- `com.alex.hopper`

## Build

Debug APK:

```bash
./gradlew assembleDebug
```

Generated APK:

- `app/build/outputs/apk/debug/app-debug.apk`

## Download APK

Ready-to-install APK in this repository:

- `downloads/HOPPER-universal-debug.apk`

Notes:

- This is a universal debug build that can be downloaded and installed directly
- If Android blocks installation, allow install from the browser or file manager
- A signed release APK can be added later when a release keystore is prepared

## Developer

- AlexDukshanin
