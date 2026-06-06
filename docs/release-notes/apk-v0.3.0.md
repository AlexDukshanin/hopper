# HOPPER APK v0.3.0

Camera, import, and journal-state update for HOPPER.

## Added

- Camera zoom control with pinch gesture and slider.
- Ultra-wide camera support where the device exposes a usable lens.
- New wagon states: `БР` and `КЛ`, alongside `ПР` and `ГР`.
- Import conflict flow for Hopper files: keep original, create a copy, or replace an existing collection.
- Adaptive text helpers for better layout with larger Android font settings.

## Improved

- Hopper file association for `.hopper`, ZIP, and octet-stream imports.
- Legacy file import permission handling on older Android versions.
- Collection export format now stores the full wagon condition instead of only loaded/empty state.
- Journal counters and state chips now reflect all wagon conditions.
- Camera overlay layout, hint button placement, and scan controls.

## Fixed

- More reliable opening of shared Hopper files from file managers and messengers.
- Better preservation of wagon state when exporting and importing collections.
- Reduced text overflow in dense journal controls on large-font devices.

## APK in this release

- `HOPPER-universal-release.apk`
- `HOPPER-arm64-v8a-release.apk`
- `HOPPER-armeabi-v7a-release.apk`
- `HOPPER-x86-release.apk`
- `HOPPER-x86_64-release.apk`
- `HOPPER-universal-debug.apk`

## Installation

- Download the APK from the release assets.
- If you are not sure which one to choose, use `HOPPER-universal-release.apk`.
- For most modern phones, `HOPPER-arm64-v8a-release.apk` is the best lightweight option.
- Install it over the existing HOPPER build to keep collections and settings.

## Important note

- App version is now `0.3.0 (3)`.
- The app database migrates to version `6`; install over the existing app to keep user data.
- Release APKs are signed with the local debug key for direct installation and testing convenience.
