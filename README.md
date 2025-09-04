# Detracktor
![vibe-coded](https://img.shields.io/badge/vibe--coded-✨-blue)
[![CI](https://github.com/cango91/Detracktor/actions/workflows/ci.yml/badge.svg)](https://github.com/cango91/Detracktor/actions/workflows/ci.yml)
[![codecov](https://codecov.io/github/cango91/detracktor/graph/badge.svg?token=00BJ2YAG28)](https://codecov.io/github/cango91/detracktor)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/cango91/Detracktor?include_prereleases)](https://github.com/cango91/Detracktor/releases)

A privacy-focused Android application that removes tracking parameters from URLs to protect user privacy and clean up shared links.

## Overview

Detracktor automatically detects URLs in your clipboard and removes common tracking parameters like UTM codes, Facebook click IDs, Google Analytics parameters, and other privacy-invasive tracking tokens with a single touch. The app provides a clean, intuitive interface for URL cleaning with customizable rules and warning systems.

## Features

- **Simple and fast** 
- **Multiple trigger methods**:
  - Share to Detracktor from any app to get a trackers-cleaned shareable link back.
  - Launch Detracktor to investigate the URL in your clipboard manually.
- **Configurable cleaning rules**
- **Configurable warning rules**

## Configuration

### Default Rules
The app comes with pre-configured rules for common tracking parameters:
- UTM parameters (`utm_source`, `utm_medium`, `utm_campaign`, etc.)
- Social media tracking (`fbclid`, `gclid`, `msclkid`)
- Analytics tokens (`_ga`, `_gl`, `mc_*`)
- Campaign parameters (`campaign_*`, `source`, `medium`)
- Site-specific rules for popular social media apps

### Custom Rules
Users can add custom rules through the settings interface:
- Domain-specific patterns
- Parameter name matching with wildcards
- Subdomain handling options
- Warning configuration

## Testing

Comprehensive test suite with unit and integration tests:
- **Unit Tests** - Core logic testing with Robolectric
- **Integration Tests** - End-to-end testing on real devices
- **UI Tests** - Compose UI testing framework

## Development

### Version Management
This project uses automated version synchronization between git tags and Android app versions:

```bash
# Set up git hooks (one-time setup for contributors)
./scripts/setup-hooks.sh

# Update version for new release
./scripts/sync-version.sh v1.2.3 # or v1.2.3-pre.release for pre-release versions

# Commit and tag
git add . && git commit -m "chore: bump version to 1.2.3"
git tag v1.2.3
git push origin main --tags
```

The pre-push hook automatically validates that git tags match the Android app version declared in `version.properties`.

## Privacy and Security

### Data Handling
- **No Network Access** - All processing happens locally on device
- **No Data Collection** - No analytics, telemetry, or user data collection
- **Clipboard Privacy** - URLs are processed locally and never transmitted

### Privacy-Aware UI
- **Minimal Display** - URL parameter values are hidden by default, including any embedded credentials. Only known tracking parameters are fully visible to maintain privacy while allowing users to identify trackers.

### Warning System
- **Embedded Credentials** - Warns about URLs containing usernames/passwords
- **Sensitive Parameters** - Identifies potentially sensitive query parameters
- **Configurable Alerts** - Users can customize warning behavior

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