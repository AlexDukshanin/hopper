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

## Developer

- AlexDukshanin
