# Implementation Plan

## Overview
Implement hierarchical pattern matching and comprehensive host normalization for the ShareUntracked Android URL cleaning app to replace the current simple string-based matching system with a robust, performance-optimized solution.

This enhancement addresses critical limitations in the current pattern matching system by introducing rule specificity ordering, comprehensive host normalization (case, port, IDN/Punycode), compiled regex pattern caching, and a breaking-change approach to improve the rule format. The implementation will transform the app from basic wildcard matching to a sophisticated URL cleaning engine capable of handling complex real-world scenarios while maintaining high performance through intelligent caching and pre-compilation strategies.

## Types
Enhanced data structures with rule priority, compiled patterns, and normalization support.

### New Enums and Data Classes

```kotlin
// Rule priority levels for hierarchical matching
enum class RulePriority(val level: Int) {
    EXACT_HOST(1),           // example.com
    SUBDOMAIN_WILDCARD(2),   // *.example.com  
    PATH_SPECIFIC(3),        // example.com/path/*
    GLOBAL_WILDCARD(4)       // *
}

// Pattern types for flexible matching
enum class PatternType {
    EXACT,          // Exact string match
    WILDCARD,       // Simple wildcard (*.domain.com)
    REGEX,          // Full regex pattern
    PATH_PATTERN    // URL path pattern matching
}

// Enhanced cleaning rule with priority and compiled patterns
data class EnhancedCleaningRule(
    val hostPattern: String,
    val params: List<String>,
    val priority: RulePriority,
    val patternType: PatternType = PatternType.WILDCARD,
    val enabled: Boolean = true,
    val description: String? = null
)

// Compiled rule for runtime performance
data class CompiledRule(
    val originalRule: EnhancedCleaningRule,
    val compiledHostPattern: Regex?,
    val compiledParamPatterns: List<Regex>,
    val normalizedHostPattern: String,
    val specificity: Int
)

// Host normalization result
data class NormalizedHost(
    val original: String,
    val normalized: String,
    val port: Int?,
    val isIDN: Boolean,
    val punycode: String?
)
```

### Updated Configuration Structure

```kotlin
// Enhanced app configuration with versioning
data class EnhancedAppConfig(
    val version: Int = 2,
    val removeAllParams: Boolean = false,
    val rules: List<EnhancedCleaningRule> = emptyList(),
    val hostNormalization: HostNormalizationConfig = HostNormalizationConfig(),
    val performance: PerformanceConfig = PerformanceConfig()
)

data class HostNormalizationConfig(
    val enableCaseNormalization: Boolean = true,
    val enablePortNormalization: Boolean = true,
    val enableIDNConversion: Boolean = true,
    val defaultPorts: Map<String, Int> = mapOf("http" to 80, "https" to 443)
)

data class PerformanceConfig(
    val enablePatternCaching: Boolean = true,
    val maxCacheSize: Int = 1000,
    val recompileOnConfigChange: Boolean = true
)
```

## Files
Comprehensive file modifications including new utility classes and enhanced core services.

### New Files to Create
- `app/src/main/java/com/example/shareuntracked/utils/HostNormalizer.kt` - Host normalization engine
- `app/src/main/java/com/example/shareuntracked/utils/RuleCompiler.kt` - Pattern compilation and caching
- `app/src/main/java/com/example/shareuntracked/utils/RuleSpecificity.kt` - Rule priority calculation
- `app/src/main/java/com/example/shareuntracked/data/EnhancedCleaningRule.kt` - New rule data structure
- `app/src/main/java/com/example/shareuntracked/data/CompiledRule.kt` - Compiled rule wrapper
- `app/src/main/java/com/example/shareuntracked/data/EnhancedAppConfig.kt` - Enhanced configuration
- `app/src/main/java/com/example/shareuntracked/migration/ConfigMigration.kt` - Config format migration
- `app/src/main/assets/enhanced_default_rules.json` - New default rules format

### Files to Modify
- `app/src/main/java/com/example/shareuntracked/UrlCleanerService.kt` - Complete refactor with hierarchical matching
- `app/src/main/java/com/example/shareuntracked/ConfigManager.kt` - Enhanced config loading with migration
- `app/src/main/java/com/example/shareuntracked/ConfigActivity.kt` - UI updates for new rule format
- `app/build.gradle.kts` - Add IDN support dependency

### Files to Update for Testing
- `app/src/test/java/com/example/shareuntracked/UrlCleanerServiceTest.kt` - Comprehensive test coverage
- `app/src/test/java/com/example/shareuntracked/ConfigManagerTest.kt` - Migration and loading tests
- `app/src/test/java/com/example/shareuntracked/utils/HostNormalizerTest.kt` - New test file
- `app/src/test/java/com/example/shareuntracked/utils/RuleCompilerTest.kt` - New test file

## Functions
Core algorithmic functions for pattern matching, host normalization, and rule compilation.

### HostNormalizer Functions
```kotlin
// Primary normalization function
fun normalizeHost(host: String, scheme: String = "https"): NormalizedHost

// Individual normalization components
fun normalizeCasing(host: String): String
fun normalizePort(host: String, port: Int?, scheme: String): String
fun convertToIDN(host: String): String
fun convertFromIDN(host: String): String
fun extractPort(hostWithPort: String): Pair<String, Int?>
```

### RuleCompiler Functions
```kotlin
// Compile rules with caching
fun compileRules(rules: List<EnhancedCleaningRule>): List<CompiledRule>
fun compileRule(rule: EnhancedCleaningRule): CompiledRule
fun invalidateCache()
fun getCachedRules(): List<CompiledRule>?

// Pattern compilation
fun compileHostPattern(pattern: String, type: PatternType): Regex?
fun compileParamPatterns(params: List<String>): List<Regex>
```

### Enhanced UrlCleanerService Functions
```kotlin
// New hierarchical matching system
fun findBestMatchingRule(normalizedHost: String, rules: List<CompiledRule>): CompiledRule?
fun calculateRuleSpecificity(rule: EnhancedCleaningRule): Int
fun matchesCompiledRule(host: String, rule: CompiledRule): Boolean

// Enhanced URL cleaning with normalization
fun cleanUrlWithHierarchicalRules(uri: Uri, rules: List<CompiledRule>): String
fun normalizeUrlHost(uri: Uri): Uri
fun applyRuleToParameters(uri: Uri, rule: CompiledRule): Uri
```

### ConfigManager Functions
```kotlin
// Enhanced configuration management
fun loadEnhancedConfig(): EnhancedAppConfig
fun migrateConfig(oldConfig: AppConfig): EnhancedAppConfig
fun saveEnhancedConfig(config: EnhancedAppConfig)
fun getCompiledRules(): List<CompiledRule>
fun recompileRules()
```

## Classes
Object-oriented design with separation of concerns and performance optimization.

### New Utility Classes
```kotlin
// Host normalization engine
class HostNormalizer(private val config: HostNormalizationConfig) {
    private val idnConverter: IDNConverter
    private val portNormalizer: PortNormalizer
    
    fun normalize(host: String, scheme: String): NormalizedHost
    fun denormalize(normalizedHost: NormalizedHost): String
}

// Rule compilation and caching system
class RuleCompiler(private val config: PerformanceConfig) {
    private val compiledRulesCache: LRUCache<String, List<CompiledRule>>
    private val patternCache: LRUCache<String, Regex>
    
    fun compile(rules: List<EnhancedCleaningRule>): List<CompiledRule>
    fun invalidateCache()
}

// Rule specificity calculator
class RuleSpecificity {
    companion object {
        fun calculate(rule: EnhancedCleaningRule): Int
        fun compare(rule1: CompiledRule, rule2: CompiledRule): Int
        fun sortBySpecificity(rules: List<CompiledRule>): List<CompiledRule>
    }
}
```

### Enhanced Core Classes
```kotlin
// Refactored URL cleaner service
class EnhancedUrlCleanerService(private val context: Context) {
    private val configManager: EnhancedConfigManager
    private val hostNormalizer: HostNormalizer
    private val ruleCompiler: RuleCompiler
    private var cachedCompiledRules: List<CompiledRule>? = null
    
    fun cleanUrl(url: String): String
    fun processIntent(intent: Intent)
    fun cleanClipboardUrl(): CleaningResult
    private fun getCompiledRules(): List<CompiledRule>
    private fun findBestMatchingRule(host: String): CompiledRule?
}

// Enhanced configuration manager
class EnhancedConfigManager(private val context: Context) {
    private val gson: Gson
    private val configFile: File
    private val migrator: ConfigMigration
    
    fun loadConfig(): EnhancedAppConfig
    fun saveConfig(config: EnhancedAppConfig)
    fun migrateFromLegacy(): EnhancedAppConfig
    fun getCompiledRules(): List<CompiledRule>
}
```

## Dependencies
Additional libraries required for IDN support and enhanced pattern matching.

### New Dependencies in app/build.gradle.kts
```kotlin
dependencies {
    // Existing dependencies...
    
    // IDN and internationalization support
    implementation("com.ibm.icu:icu4j:72.1")
    
    // Enhanced regex and pattern matching
    implementation("org.apache.commons:commons-lang3:3.12.0")
    
    // LRU Cache for performance optimization
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // Testing dependencies for new functionality
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}
```

### Gradle Configuration Updates
```kotlin
android {
    // Existing configuration...
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enable desugaring for newer Java features
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    // Core library desugaring for compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
```

## Testing
Comprehensive test strategy covering normalization, pattern matching, and performance.

### New Test Files
```kotlin
// Host normalization tests
class HostNormalizerTest {
    @Test fun testCaseNormalization()
    @Test fun testPortNormalization()
    @Test fun testIDNConversion()
    @Test fun testComplexHostNormalization()
}

// Rule compilation tests  
class RuleCompilerTest {
    @Test fun testPatternCompilation()
    @Test fun testCacheInvalidation()
    @Test fun testPerformanceOptimization()
}

// Rule specificity tests
class RuleSpecificityTest {
    @Test fun testSpecificityCalculation()
    @Test fun testRuleOrdering()
    @Test fun testHierarchicalMatching()
}

// Integration tests for enhanced functionality
class EnhancedUrlCleanerServiceTest {
    @Test fun testHierarchicalRuleMatching()
    @Test fun testHostNormalizationIntegration()
    @Test fun testPerformanceWithLargeRuleSets()
    @Test fun testEdgeCasesAndErrorHandling()
}
```

### Enhanced Existing Tests
- Update `UrlCleanerServiceTest.kt` with comprehensive hierarchical matching scenarios
- Enhance `ConfigManagerTest.kt` with migration testing and new config format validation
- Add performance benchmarks and memory usage tests
- Include internationalization test cases with various IDN domains

### Test Data and Scenarios
```kotlin
// Comprehensive test data builder
class EnhancedTestDataBuilder {
    fun createNormalizationTestCases(): Map<String, NormalizedHost>
    fun createHierarchicalRuleTestCases(): List<RuleMatchingScenario>
    fun createPerformanceTestUrls(): List<String>
    fun createIDNTestDomains(): Map<String, String>
}
```

## Implementation Order
Sequential implementation steps to minimize conflicts and ensure successful integration.

### Phase 1: Foundation (Steps 1-3)
1. **Create New Data Structures**
   - Implement `EnhancedCleaningRule`, `CompiledRule`, `NormalizedHost`
   - Create enhanced configuration classes
   - Add new enums for priority and pattern types

2. **Implement Host Normalization**
   - Create `HostNormalizer` class with full IDN support
   - Implement case, port, and punycode normalization
   - Add comprehensive unit tests for normalization logic

3. **Build Rule Compilation System**
   - Implement `RuleCompiler` with pattern caching
   - Create `RuleSpecificity` calculator
   - Add LRU cache for performance optimization

### Phase 2: Core Integration (Steps 4-6)
4. **Enhance ConfigManager**
   - Add migration logic from old to new config format
   - Implement enhanced config loading and saving
   - Create default enhanced rules configuration

5. **Refactor UrlCleanerService**
   - Replace simple pattern matching with hierarchical system
   - Integrate host normalization into URL processing
   - Implement rule caching and compilation integration

6. **Update Configuration UI**
   - Modify `ConfigActivity` to support new rule format
   - Add UI elements for rule priority and pattern types
   - Implement rule validation and error handling

### Phase 3: Testing and Optimization (Steps 7-9)
7. **Comprehensive Testing**
   - Create test suites for all new functionality
   - Add performance benchmarks and memory usage tests
   - Implement integration tests with real-world scenarios

8. **Performance Optimization**
   - Profile and optimize rule compilation performance
   - Fine-tune cache sizes and invalidation strategies
   - Optimize memory usage for large rule sets

9. **Migration and Deployment**
   - Implement seamless config migration for existing users
   - Add fallback mechanisms for migration failures
   - Create comprehensive documentation and examples

### Phase 4: Validation and Polish (Steps 10-12)
10. **Edge Case Handling**
    - Handle malformed URLs and invalid patterns gracefully
    - Implement comprehensive error recovery mechanisms
    - Add logging and debugging capabilities

11. **Documentation and Examples**
    - Update README with new functionality examples
    - Create migration guide for existing configurations
    - Add inline code documentation and usage examples

12. **Final Integration Testing**
    - End-to-end testing with various Android versions
    - Performance testing on different device configurations
    - User acceptance testing with complex rule scenarios
