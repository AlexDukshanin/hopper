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

- [HOPPER APK v0.2.0](https://github.com/AlexDukshanin/hopper/releases/tag/apk-v0.2.0)

Included file:

- `HOPPER-universal-release.apk` — универсальный вариант для большинства пользователей
- `HOPPER-arm64-v8a-release.apk` — более легкий вариант для большинства современных телефонов
- `HOPPER-armeabi-v7a-release.apk` — вариант для более старых устройств
- `HOPPER-x86-release.apk` и `HOPPER-x86_64-release.apk` — в основном для совместимости и тестов
- `HOPPER-universal-debug.apk` — запасной debug-вариант для тестирования

Installation notes:

- Release APKs in the GitHub release are installable directly on Android devices
- For most phones the best choice is `HOPPER-arm64-v8a-release.apk`, and if architecture is unknown use `HOPPER-universal-release.apk`
- If Android blocks installation, allow installs from the browser or file manager you used to download the APK
- A dedicated production keystore can be configured later for final public distribution

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

- `app/build/outputs/apk/debug/*.apk`
- `app/build/outputs/apk/release/*.apk`

## Release notes

- [apk-v0.1.0 notes](docs/release-notes/apk-v0.1.0.md)
- [apk-v0.2.0 notes](docs/release-notes/apk-v0.2.0.md)

## Developer

- AlexDukshanin
