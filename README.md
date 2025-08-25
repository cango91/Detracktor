# Detracktor
![vibe-coded](https://img.shields.io/badge/vibe--coded-✨-blue)
[![CI](https://github.com/cango91/Detracktor/actions/workflows/ci.yml/badge.svg)](https://github.com/cango91/Detracktor/actions/workflows/ci.yml)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/cango91/Detracktor?include_prereleases)](https://github.com/cango91/Detracktor/releases)

A tiny Android app that cleans URLs by removing tracking parameters on demand.

## Features

- **Simple and fast** - Triggered via share menu or manual clipboard cleaning
- **Multiple trigger methods**:
  - Share target (when sharing URLs from other apps)
  - Manual clipboard cleaning via main app
- **Configurable cleaning modes**:
  - Remove all parameters
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

1. **Share Menu**: Share any URL to Detracktor from another app
2. **Manual**: Open the app and tap "Clean Clipboard URL"

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

### Download Release
Download the latest APK from [Releases](https://github.com/cango91/Detracktor/releases) and install on your Android device.

### Build from Source
Build the APK using Android Studio or Gradle:

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Development

### Version Management
This project uses automated version synchronization between git tags and Android app versions:

```bash
# Set up git hooks (one-time setup for contributors)
./scripts/setup-hooks.sh

# Update version for new release
./scripts/sync-version.sh v1.2.3

# Commit and tag
git add . && git commit -m "chore: bump version to 1.2.3"
git tag v1.2.3
git push origin main --tags
```

The pre-push hook automatically validates that git tags match the Android app version declared in `version.properties`.

## Privacy

Detracktor operates entirely offline with no network access, data collection, or analytics. All processing happens locally on your device.

## Acknowledgments

Inspired by and named in homage to the [macOS Detracktor macro](https://monvelasquez.com/articles/2021-09/detracktor) by [Raymond Velasquez](https://github.com/rvelasq). This Android implementation brings similar functionality to mobile devices.

Special thanks to the privacy-focused developer community working to make the web cleaner and more private. Stay sovereign!

## Legal Notice

Detracktor is an independent open-source project for URL cleaning/tracking parameter removal.

**Not affiliated with:**
- Detrack® (logistics tracking software by Detrack Systems Pte. Ltd.)  
- Any commercial tracking, logistics, or delivery management services
- Any signal blocking or faraday cage products

This software serves a completely different function (URL sanitization) for a different market (privacy-focused individuals) than any existing trademarks, to the best of the author's knowledge, at the time of creation.
