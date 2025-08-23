# ShareUntracked

A tiny Android app that cleans URLs by removing tracking parameters on demand.

## Features

- **No visible UI when used** - Triggered via Quick Settings tile, home widget, share menu, or shortcut
- **Multiple trigger methods**:
  - Quick Settings tile
  - Home screen widget
  - Share target (when sharing URLs from other apps)
  - Manual clipboard cleaning via main app
- **Configurable cleaning modes**:
  - Remove all parameters (default)
  - Custom rules for specific websites
- **Toast feedback**: Shows "Clipboard empty", "No change", or "Cleaned → copied"
- **JSON configuration** with default rules for popular tracking parameters

## Default Cleaning Rules

When using custom rules mode, the app includes default rules for:

- **Twitter/X**: Removes `t` and `si` parameters
- **Facebook**: Removes `fbclid` and `fb_*` parameters  
- **YouTube**: Removes `si` and `feature` parameters
- **Universal**: Removes `utm_*`, `gclid`, `msclkid`, and `mc_*` parameters

## How to Use

1. **Share Menu**: Share any URL to ShareUntracked from another app
2. **Quick Settings**: Add the "Clean URL" tile to your Quick Settings panel
3. **Home Widget**: Add the ShareUntracked widget to your home screen
4. **Manual**: Open the app and tap "Clean Clipboard URL"

## Configuration

Open the app and tap "Settings" to:
- Toggle between "Remove all parameters" and "Custom rules" mode
- View current cleaning rules
- Reset to default configuration

## Technical Details

- **Target SDK**: 36 (Android 15)
- **Minimum SDK**: 29 (Android 10)
- **Architecture**: Client-only, no network or background services
- **Storage**: JSON configuration files
- **UI**: Modern Compose UI with Material 3 design

## Installation

Build the APK using Android Studio or Gradle:

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Privacy

ShareUntracked operates entirely offline with no network access, data collection, or analytics. All processing happens locally on your device.
