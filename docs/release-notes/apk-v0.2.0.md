# HOPPER APK v0.2.0

Major feature update for HOPPER with collection workflow improvements, QR exchange, adaptive UI polish, and camera/journal usability fixes.

## Added

- QR-code sharing for Hopper collections without photos
- QR-code import directly inside the app
- Camera-based scan frame editor with drag and resize controls
- Quick access from the camera screen to scan frame settings
- Empty wagon cards that can be created without taking a photo first
- Adaptive UI metrics for better scaling on different phone sizes
- New dark theme presets: `Raycast dark`, `Composio dark`, `NVIDIA dark`, `MongoDB dark`

## Improved

- Collection-specific journal settings and direction labels
- Search screens and search result presentation
- App icon picker with real launcher icon previews
- Light and dark theme contrast for buttons, cards, and controls
- Journal action buttons and visibility toggle readability on dark themes
- Journal and wagon note saving flow with keyboard dismissal and confirmation snackbars
- Hopper file import flow from the home screen
- Default file picker opening in `Downloads`
- Release versioning for update installation over previous builds

## Fixed

- QR import crash on devices without stable Google Play Services support
- Duplicate collection creation after QR import
- First camera launch and route restoration issues
- Wrong collection receiving new photos during navigation between journals
- Photo orientation after Hopper file export/import
- Search crashes from the home screen
- Layout issues with icon previews, settings panels, and journal controls

## APK in this release

- `HOPPER-universal-release.apk`
- `HOPPER-arm64-v8a-release.apk`
- `HOPPER-armeabi-v7a-release.apk`
- `HOPPER-x86-release.apk`
- `HOPPER-x86_64-release.apk`
- `HOPPER-universal-debug.apk`

## Installation

- Download the APK from the release assets
- If you are not sure which one to choose, use `HOPPER-universal-release.apk`
- For most modern phones, `HOPPER-arm64-v8a-release.apk` is the best lightweight option
- Install it over the existing HOPPER build to keep collections and settings
- If Android blocks installation, allow installs from the browser or file manager you used

## Important note

- Release APKs are signed with the local debug key for direct installation and testing convenience
- User data is preserved on update as long as the app is installed over the existing package and not removed first
