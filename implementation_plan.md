# Implementation Plan

## Overview
Complete redesign and simplification of the Detracktor Android app by removing over-engineered analysis components and implementing a simplified regex-based rule engine with smart partial-blur rendering for privacy-focused URL cleaning.

The current implementation has accumulated significant technical debt through scope creep, particularly in privacy analysis features that go far beyond the app's core purpose of removing tracking parameters. This redesign will eliminate the complex heuristic analysis systems while preserving the core functionality and good architectural decisions. The new design focuses on a unified regex-based rule system with numeric priorities, smart partial-blur rendering that treats non-rule-matching parameters as potentially sensitive, and maintains only essential embedded credential detection for user safety.

## Types
Simplified data structures replacing complex analysis types with focused, purpose-built classes.

**New Core Types:**
```kotlin
// Simplified rule with regex pattern and numeric priority
data class CleaningRule(
    val id: String,
    val hostPattern: String,  // Always treated as regex
    val parameterPatterns: List<String>,  // Parameter names/patterns to remove
    val priority: Int,  // Lower number = higher priority
    val enabled: Boolean = true,
    val description: String? = null
)

// Smart string representation for partial blur rendering
data class AnnotatedUrlSegment(
    val text: String,
    val type: SegmentType,
    val shouldBlur: Boolean = false
)

enum class SegmentType {
    PROTOCOL,
    CREDENTIALS,
    HOST,
    PATH,
    PARAM_NAME,
    PARAM_VALUE,
    SEPARATOR
}

// Simplified analysis result
data class UrlAnalysis(
    val originalUrl: String,
    val segments: List<AnnotatedUrlSegment>,
    val hasEmbeddedCredentials: Boolean,
    val matchingRules: List<String>,
    val cleanedUrl: String
)

// Configuration remains similar but simplified
data class AppConfig(
    val version: Int,
    val rules: List<CleaningRule>,
    val removeAllParams: Boolean = false
)
```

**Removed Types:**
- `PatternType` enum (replaced by always-regex approach)
- `RulePriority` enum (replaced by numeric priority)
- `UrlPrivacyLevel`, `ClipboardContentType`, `PrivacySettings`
- `FilteredContent`, `RiskFactor`, `RiskFactorType`, `RiskSeverity`
- `ClipboardAnalysis` (replaced by `UrlAnalysis`)
- `CompiledRule` (simplified compilation)
- All privacy analysis data classes

## Files
Complete removal of over-engineered components and creation of simplified replacements.

**Files to Delete:**
- `app/src/main/java/com/gologlu/detracktor/data/PatternType.kt`
- `app/src/main/java/com/gologlu/detracktor/data/RulePriority.kt`
- `app/src/main/java/com/gologlu/detracktor/data/CompiledRule.kt`
- `app/src/main/java/com/gologlu/detracktor/data/ClipboardAnalysis.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/UrlPrivacyAnalyzer.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/ClipboardContentFilter.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/RegexValidator.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/RuleSpecificity.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/RuleCompiler.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/HostNormalizer.kt`
- `app/src/main/java/com/gologlu/detracktor/data/HostNormalizationConfig.kt`
- `app/src/main/java/com/gologlu/detracktor/data/NormalizedHost.kt`
- `app/src/main/java/com/gologlu/detracktor/data/PerformanceConfig.kt`
- All test files (will be recreated for new implementation)

**Files to Keep and Modify:**
- `app/src/main/java/com/gologlu/detracktor/MainActivity.kt` - Update UI for new analysis
- `app/src/main/java/com/gologlu/detracktor/ConfigActivity.kt` - Simplify rule editing
- `app/src/main/java/com/gologlu/detracktor/ConfigManager.kt` - Update for new config format
- `app/src/main/java/com/gologlu/detracktor/UrlCleanerService.kt` - Implement new cleaning logic
- `app/src/main/java/com/gologlu/detracktor/data/CleaningRule.kt` - Replace with simplified version
- `app/src/main/java/com/gologlu/detracktor/data/AppConfig.kt` - Simplify configuration
- `app/src/main/java/com/gologlu/detracktor/data/CleaningResult.kt` - Keep as-is
- `app/src/main/assets/default_rules.json` - Update for new rule format

**New Files to Create:**
- `app/src/main/java/com/gologlu/detracktor/data/UrlAnalysis.kt`
- `app/src/main/java/com/gologlu/detracktor/data/AnnotatedUrlSegment.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/UrlAnalyzer.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/RuleEngine.kt`
- `app/src/main/java/com/gologlu/detracktor/utils/CredentialDetector.kt`

## Functions
Replacement of complex analysis functions with focused, single-purpose implementations.

**New Core Functions:**
- `RuleEngine.compileRules(rules: List<CleaningRule>): List<CompiledPattern>`
- `RuleEngine.matchRules(url: String, compiledRules: List<CompiledPattern>): List<String>`
- `UrlAnalyzer.analyzeUrl(url: String, matchingParams: List<String>): UrlAnalysis`
- `UrlAnalyzer.createSegments(url: String, matchingParams: List<String>): List<AnnotatedUrlSegment>`
- `CredentialDetector.hasEmbeddedCredentials(url: String): Boolean`
- `CredentialDetector.extractCredentials(url: String): Pair<String, String>?`

**Modified Functions:**
- `UrlCleanerService.cleanUrl()` - Simplified logic using new rule engine
- `UrlCleanerService.analyzeClipboardContent()` - Return `UrlAnalysis` instead of complex analysis
- `ConfigManager.loadConfig()` - Handle new simplified config format
- `ConfigManager.validateRule()` - Simplified regex validation only

**Removed Functions:**
- All privacy analysis functions from `UrlPrivacyAnalyzer`
- All content filtering functions from `ClipboardContentFilter`
- Complex regex validation from `RegexValidator`
- Host normalization functions
- Rule specificity calculations
- Pattern compilation complexity

## Classes
Elimination of over-engineered analysis classes and creation of focused replacements.

**New Classes:**
- `UrlAnalyzer` - Core URL analysis with segment creation
- `RuleEngine` - Simplified rule compilation and matching
- `CredentialDetector` - Focused embedded credential detection

**Modified Classes:**
- `CleaningRule` - Simplified to regex pattern + numeric priority
- `UrlCleanerService` - Updated to use new analysis system
- `ConfigManager` - Simplified configuration handling
- `MainActivity` - Updated UI components for new analysis display

**Removed Classes:**
- `UrlPrivacyAnalyzer` - Replaced by focused `UrlAnalyzer`
- `ClipboardContentFilter` - Functionality moved to `UrlAnalyzer`
- `RegexValidator` - Basic validation moved to `ConfigManager`
- `RuleSpecificity` - Replaced by simple numeric priority
- `RuleCompiler` - Simplified compilation in `RuleEngine`
- `HostNormalizer` - Removed unnecessary complexity

## Dependencies
No changes to external dependencies - all current dependencies remain valid.

The existing dependencies (Gson, Compose, ICU4J, Apache Commons) are sufficient for the simplified implementation. The removal of complex analysis features actually reduces dependency usage rather than requiring new ones.

## Testing
Complete recreation of test suite focused on core functionality.

**New Test Files:**
- `UrlAnalyzerTest.kt` - Test URL segmentation and analysis
- `RuleEngineTest.kt` - Test rule compilation and matching
- `CredentialDetectorTest.kt` - Test embedded credential detection
- `ConfigManagerTest.kt` - Updated for new config format
- `UrlCleanerServiceTest.kt` - Updated for new cleaning logic
- `MainActivityTest.kt` - Updated for new UI components

**Test Strategy:**
- Focus on core URL cleaning functionality
- Test regex rule compilation and matching
- Verify embedded credential detection accuracy
- Test partial blur rendering logic
- Validate configuration loading and saving
- Integration tests for two-mode operation (main activity vs pass-through)

## Implementation Order
Logical sequence to minimize conflicts and ensure successful integration.

1. **Delete Over-Engineered Components** - Remove all complex analysis files and tests
2. **Create New Data Structures** - Implement simplified `CleaningRule`, `UrlAnalysis`, and `AnnotatedUrlSegment`
3. **Implement Core Utilities** - Create `RuleEngine`, `UrlAnalyzer`, and `CredentialDetector`
4. **Update Configuration System** - Modify `ConfigManager` and `AppConfig` for new format
5. **Update Default Rules** - Convert `default_rules.json` to new simplified format
6. **Modify Service Layer** - Update `UrlCleanerService` to use new analysis system
7. **Update UI Components** - Modify `MainActivity` and `ConfigActivity` for new analysis display
8. **Create New Tests** - Implement comprehensive test suite for new components
9. **Integration Testing** - Verify two-mode operation and configuration persistence
10. **Final Cleanup** - Remove any remaining references to deleted components
