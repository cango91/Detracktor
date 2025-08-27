# Implementation Plan

## Overview
Fix two critical security and privacy issues in the Detracktor Android app before Play Store release: ReDoS vulnerability in regex compilation and clipboard content exposure for non-URI data.

The Detracktor app is a URL cleaning utility that removes tracking parameters from URLs. It uses a sophisticated rule-based system with regex pattern matching and displays clipboard content analysis in the UI. Two security hardening issues have been identified that need to be resolved in a single PR to prepare for production release.

## Types
Enhanced security validation and privacy protection through input sanitization and content filtering.

**New Types:**
- `RegexValidator` - Utility class for validating regex patterns against ReDoS vulnerabilities
- `RegexTimeoutException` - Custom exception for regex timeout scenarios
- `ClipboardContentFilter` - Utility for filtering sensitive clipboard content display

**Enhanced Types:**
- `ClipboardAnalysis` - Add `shouldDisplayContent` boolean field to control content visibility
- `PerformanceConfig` - Add `regexTimeoutMs` and `enableRegexValidation` configuration options

## Files
Security hardening through validation utilities and privacy-conscious content display.

**New Files:**
- `app/src/main/java/com/gologlu/detracktor/utils/RegexValidator.kt` - ReDoS pattern detection and validation
- `app/src/main/java/com/gologlu/detracktor/utils/ClipboardContentFilter.kt` - Privacy-safe content filtering

**Modified Files:**
- `app/src/main/java/com/gologlu/detracktor/utils/RuleCompiler.kt` - Add regex validation and timeout protection
- `app/src/main/java/com/gologlu/detracktor/ConfigActivity.kt` - Add regex validation in rule creation dialog
- `app/src/main/java/com/gologlu/detracktor/MainActivity.kt` - Hide non-URI clipboard content display
- `app/src/main/java/com/gologlu/detracktor/data/ClipboardAnalysis.kt` - Add content display control field
- `app/src/main/java/com/gologlu/detracktor/data/PerformanceConfig.kt` - Add regex security configuration
- `app/src/main/java/com/gologlu/detracktor/UrlCleanerService.kt` - Update clipboard analysis with content filtering

## Functions
Comprehensive security validation and privacy protection functions.

**New Functions:**
- `RegexValidator.validatePattern(pattern: String): ValidationResult` - Detect ReDoS patterns
- `RegexValidator.compileWithTimeout(pattern: String, timeoutMs: Long): Regex?` - Safe regex compilation
- `RegexValidator.isReDoSVulnerable(pattern: String): Boolean` - ReDoS vulnerability detection
- `ClipboardContentFilter.shouldDisplayContent(analysis: ClipboardAnalysis): Boolean` - Content display decision
- `ClipboardContentFilter.getSafeDisplayText(content: String): String` - Safe content representation

**Modified Functions:**
- `RuleCompiler.compileHostPatternInternal()` - Add validation and timeout protection
- `RuleCompiler.compileParamPattern()` - Add validation and timeout protection
- `ConfigActivity.validateRule()` - Enhanced regex validation with ReDoS detection
- `MainActivity.ClipboardPreviewCard()` - Conditional content display based on privacy settings
- `UrlCleanerService.analyzeClipboardContent()` - Include content display decision in analysis

## Classes
Enhanced security and privacy protection through new utility classes and modified existing classes.

**New Classes:**
- `RegexValidator` (utils/RegexValidator.kt) - Static utility methods for regex security validation
- `ClipboardContentFilter` (utils/ClipboardContentFilter.kt) - Privacy-focused content filtering utilities

**Modified Classes:**
- `RuleCompiler` - Enhanced with regex validation and timeout protection mechanisms
- `ClipboardAnalysis` - Extended with `shouldDisplayContent` field for privacy control
- `PerformanceConfig` - Extended with regex security configuration options

## Dependencies
No new external dependencies required - leveraging existing Android and Kotlin standard libraries.

All security enhancements use built-in Android/Kotlin capabilities:
- `kotlin.time.Duration` for timeout handling
- `kotlinx.coroutines` for timeout operations (already in project)
- Standard regex pattern analysis using existing Kotlin Regex class
- Android's existing LruCache and logging utilities

## Testing
Comprehensive test coverage for security vulnerabilities and privacy protection.

**New Test Files:**
- `app/src/test/java/com/gologlu/detracktor/utils/RegexValidatorTest.kt` - ReDoS detection and validation tests
- `app/src/test/java/com/gologlu/detracktor/utils/ClipboardContentFilterTest.kt` - Privacy filtering tests

**Modified Test Files:**
- `app/src/test/java/com/gologlu/detracktor/utils/RuleCompilerTest.kt` - Add timeout and validation tests
- `app/src/test/java/com/gologlu/detracktor/ConfigManagerTest.kt` - Add regex validation tests
- `app/src/test/java/com/gologlu/detracktor/MainActivityTest.kt` - Add clipboard privacy tests

**Test Scenarios:**
- ReDoS pattern detection (nested quantifiers, catastrophic backtracking)
- Regex compilation timeout handling
- Clipboard content privacy filtering
- Invalid regex pattern rejection
- Performance impact of security measures

## Implementation Order
Sequential implementation to minimize conflicts and ensure proper integration testing.

1. **Create RegexValidator utility** - Foundation for all regex security validation
2. **Create ClipboardContentFilter utility** - Privacy protection foundation
3. **Update data classes** - Add new fields to ClipboardAnalysis and PerformanceConfig
4. **Enhance RuleCompiler** - Add validation and timeout protection to regex compilation
5. **Update ConfigActivity** - Add regex validation to rule creation dialog
6. **Update MainActivity** - Implement privacy-conscious clipboard content display
7. **Update UrlCleanerService** - Integrate content filtering in clipboard analysis
8. **Create comprehensive tests** - Validate security measures and privacy protection
9. **Integration testing** - Ensure all components work together without performance degradation
10. **Documentation update** - Update inline documentation for security measures
