# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Detracktor** is a privacy-focused Android application that removes tracking parameters from URLs. It operates entirely offline using a rule-based system to clean URLs while protecting user privacy.

## Architecture

The codebase follows **Clean Architecture** with three main layers:

- **Domain Layer** (`/domain/`): Core business logic, models, and service interfaces
- **Application Layer** (`/application/`): Use cases, service implementations, and error handling  
- **Runtime Layer** (`/runtime/android/`): Android-specific implementations, UI components, and platform integrations

**Key Components:**
- `UrlParser`: Parses URLs into structured components
- `RuleEngine`: Matches and applies cleaning rules
- `SettingsService`: Manages user configurations and cleaning rules
- Jetpack Compose UI with Material 3 design

## Build System

**Technology Stack:**
- **Target SDK**: 36, **Min SDK**: 29
- **Kotlin**: 2.2.10, **AGP**: 8.13.0
- **Java Version**: 11
- **Compose BOM**: 2025.08.01

**Dependency Management:** Uses version catalog in `gradle/libs.versions.toml`

## Common Commands

**Setup (one-time):**
```bash
./scripts/setup-hooks.sh
```

**Building:**
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew clean build           # Clean build
```

**Testing:**
```bash
./gradlew test                   # Run unit tests (Robolectric)
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew jacocoTestReport      # Generate coverage report
./gradlew lint                  # Run code analysis
```

**Version Management:**
```bash
./scripts/sync-version.sh v1.2.3    # Update version across all files
git tag v1.2.3 && git push origin main --tags
```

## Testing Setup

- **Unit Tests**: `/app/src/test/` - Uses Robolectric for Android components
- **Instrumented Tests**: `/app/src/androidTest/` - Real device testing with Compose UI testing
- **Coverage**: Jacoco with Codecov integration
- **Framework**: JUnit 5, MockK, Espresso, Compose Testing

## Development Workflow

**Version Source of Truth:** `version.properties` - All version numbers sync from this file

**Dependency Injection:** Manual composition root pattern in `Composition.kt` - no DI framework used

**State Management:** Reactive Compose UI with file observers for real-time settings updates

**Privacy-First Design:** No network permissions, local-only processing, credential detection warnings

## Key Files

- `app/build.gradle.kts` - Main build configuration
- `version.properties` - Single source of truth for version numbers
- `app/src/main/java/com/gologlu/detracktor/runtime/android/MainActivity.kt` - Main UI and application logic
- `app/src/main/java/com/gologlu/detracktor/runtime/android/Composition.kt` - Dependency injection setup