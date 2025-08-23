# Testing Guide for ShareUntracked

This document describes the testing approach and setup for the ShareUntracked Android application.

## Overview

The testing infrastructure has been completely refactored to provide reliable unit and integration tests with proper dependency injection and mocking setup.

## Test Structure

### Unit Tests (`app/src/test/`)
- **ConfigManagerTest.kt** - Tests configuration management functionality
- **UrlCleanerServiceTest.kt** - Tests URL cleaning logic
- **Test Utilities** - Helper classes for consistent test data and mocking

### Integration Tests (`app/src/androidTest/`)
- **IntegrationTest.kt** - End-to-end tests that run on actual Android devices/emulators

## Key Features

### 1. Robolectric Integration
- Uses Robolectric 4.11.1 for Android component testing without requiring a device
- Provides real Android Context for unit tests
- Configured via `app/src/test/resources/robolectric.properties`

### 2. Test Utilities
- **TestContextProvider** - Creates properly configured mock Android contexts
- **TestDataBuilder** - Provides consistent test data across all tests
- Minimal dependencies approach - only adds test-specific dependencies

### 3. Simplified Mocking
- Focuses on testing actual functionality rather than complex mock interactions
- Uses real Android Context where possible via Robolectric
- Avoids brittle mocking that breaks easily

## Dependencies Added

### Test-Only Dependencies
```kotlin
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("androidx.test:core:1.5.0")
```

These dependencies are only used for testing and do not affect the production app.

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### Integration Tests (requires connected device/emulator)
```bash
./gradlew connectedAndroidTest
```

### All Tests
```bash
./gradlew check
```

## Test Coverage

### ConfigManager Tests
- ✅ Default configuration loading
- ✅ Configuration persistence (save/load cycle)
- ✅ Default rules retrieval
- ✅ Reset to default functionality
- ✅ Data structure validation

### UrlCleanerService Tests
- ✅ Invalid URL handling
- ✅ URLs without parameters
- ✅ HTTPS/HTTP URL processing
- ✅ Non-HTTP URL handling (FTP, etc.)
- ✅ URL parameter cleaning logic
- ✅ Clipboard operations
- ✅ Service instantiation

### Integration Tests
- ✅ Application context validation
- ✅ ConfigManager integration with real Android context
- ✅ UrlCleanerService integration with real Android context
- ✅ MainActivity launch verification
- ✅ End-to-end URL cleaning workflow
- ✅ File operations testing
- ✅ Real clipboard integration

## Best Practices

### 1. Keep Tests Simple
- Focus on testing behavior, not implementation details
- Use real Android components via Robolectric when possible
- Avoid complex mocking that makes tests brittle

### 2. Test Data Management
- Use TestDataBuilder for consistent test data
- Create reusable test scenarios
- Keep test data close to the tests that use it

### 3. Error Handling
- Test both success and failure scenarios
- Verify graceful handling of invalid inputs
- Test edge cases and boundary conditions

## Troubleshooting

### Common Issues

1. **NullPointerException in tests**
   - Ensure Robolectric is properly configured
   - Check that @RunWith(RobolectricTestRunner::class) is present
   - Verify ApplicationProvider.getApplicationContext() is used

2. **Asset loading failures**
   - Check that test assets are in the correct location
   - Verify JSON format is valid
   - Use fallback behavior for missing assets

3. **Integration test failures**
   - Ensure Android device/emulator is connected
   - Check that app permissions are properly configured
   - Verify test APK is properly built and installed

## Future Improvements

1. **Enhanced Test Coverage**
   - Add more edge case testing
   - Test configuration file corruption scenarios
   - Add performance testing for large URL lists

2. **Test Automation**
   - Set up CI/CD pipeline integration
   - Add automated test reporting
   - Configure test coverage reporting

3. **Mock Improvements**
   - Add more sophisticated clipboard testing
   - Test network-related scenarios
   - Add UI component testing with Compose testing framework

## Conclusion

The testing infrastructure now provides a solid foundation for maintaining code quality and preventing regressions. The combination of Robolectric for unit tests and real device testing for integration tests ensures comprehensive coverage while keeping the setup simple and maintainable.
