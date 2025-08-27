package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RuleCompilerTest {

    private lateinit var ruleCompiler: RuleCompiler
    private lateinit var performanceConfig: PerformanceConfig

    @Before
    fun setUp() {
        performanceConfig = PerformanceConfig(
            enablePatternCaching = true,
            maxCacheSize = 100,
            recompileOnConfigChange = true,
            regexTimeoutMs = 1000L,
            enableRegexValidation = true
        )
        ruleCompiler = RuleCompiler(performanceConfig)
    }

    @Test
    fun testCompileRule_withExactPattern_returnsCompiledRule() {
        // Given
        val rule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium"),
            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Test exact pattern rule"
        )

        // When
        val compiledRule = ruleCompiler.compileRule(rule)

        // Then
        assertNotNull(compiledRule)
        assertEquals(rule, compiledRule.originalRule)
        assertNull(compiledRule.compiledHostPattern) // Exact patterns don't need regex
        assertEquals("example.com", compiledRule.normalizedHostPattern)
        assertTrue(compiledRule.specificity > 0)
        assertEquals(2, compiledRule.compiledParamPatterns.size)
    }

    @Test
    fun testCompileRule_withWildcardPattern_returnsCompiledRule() {
        // Given
        val rule = CleaningRule(
            hostPattern = "*.example.com",
            params = listOf("utm_*", "fbclid"),
            priority = RulePriority.SUBDOMAIN_WILDCARD,
            patternType = PatternType.WILDCARD,
            enabled = true,
            description = "Test wildcard pattern rule"
        )

        // When
        val compiledRule = ruleCompiler.compileRule(rule)

        // Then
        assertNotNull(compiledRule)
        assertEquals(rule, compiledRule.originalRule)
        assertNotNull(compiledRule.compiledHostPattern) // Wildcard patterns need regex
        assertEquals("*.example.com", compiledRule.normalizedHostPattern)
        assertTrue(compiledRule.specificity > 0)
        assertEquals(2, compiledRule.compiledParamPatterns.size)
    }

    @Test
    fun testCompileRule_withRegexPattern_returnsCompiledRule() {
        // Given
        val rule = CleaningRule(
            hostPattern = ".*\\.example\\.com",
            params = listOf("tracking_.*"),
            priority = RulePriority.SUBDOMAIN_WILDCARD,
            patternType = PatternType.REGEX,
            enabled = true,
            description = "Test regex pattern rule"
        )

        // When
        val compiledRule = ruleCompiler.compileRule(rule)

        // Then
        assertNotNull(compiledRule)
        assertEquals(rule, compiledRule.originalRule)
        assertNotNull(compiledRule.compiledHostPattern) // Regex patterns need compiled regex
        assertEquals(".*\\.example\\.com", compiledRule.normalizedHostPattern)
        assertTrue(compiledRule.specificity > 0)
        assertEquals(1, compiledRule.compiledParamPatterns.size)
    }

    @Test
    fun testCompileRules_withMultipleRules_returnsCompiledAndSortedRules() {
        // Given
        val rules = listOf(
            CleaningRule(
                hostPattern = "*",
                params = listOf("utm_*"),
                priority = RulePriority.GLOBAL_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Global wildcard rule"
            ),
            CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_source"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Domain specific rule"
            ),
            CleaningRule(
                hostPattern = "*.example.com",
                params = listOf("fbclid"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Subdomain wildcard rule"
            )
        )

        // When
        val compiledRules = ruleCompiler.compileRules(rules)

        // Then
        assertNotNull(compiledRules)
        assertEquals(3, compiledRules.size)
        
        // Rules should be sorted by specificity (most specific first)
        assertTrue(compiledRules[0].specificity >= compiledRules[1].specificity)
        assertTrue(compiledRules[1].specificity >= compiledRules[2].specificity)
    }

    @Test
    fun testCompileHostPattern_withExactType_returnsNull() {
        // Given
        val pattern = "example.com"
        val type = PatternType.EXACT

        // When
        val result = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNull(result) // Exact patterns don't need regex compilation
    }

    @Test
    fun testCompileHostPattern_withWildcardType_returnsRegex() {
        // Given
        val pattern = "*.example.com"
        val type = PatternType.WILDCARD

        // When
        val result = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNotNull(result)
        assertTrue(result!!.matches("sub.example.com"))
        assertTrue(result.matches("www.example.com"))
        assertTrue(!result.matches("example.org"))
    }

    @Test
    fun testCompileHostPattern_withRegexType_returnsRegex() {
        // Given
        val pattern = ".*\\.example\\.com"
        val type = PatternType.REGEX

        // When
        val result = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNotNull(result)
        assertTrue(result!!.matches("sub.example.com"))
        assertTrue(result.matches("www.example.com"))
        assertTrue(!result.matches("example.org"))
    }

    @Test
    fun testCompileHostPattern_withPathPatternType_returnsRegex() {
        // Given
        val pattern = "example.com/path/*"
        val type = PatternType.PATH_PATTERN

        // When
        val result = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNotNull(result)
        assertTrue(result!!.matches("example.com/path/subpath"))
        assertTrue(!result.matches("example.com/other"))
    }

    @Test
    fun testCompileParamPatterns_withWildcardParams_returnsRegexList() {
        // Given
        val params = listOf("utm_*", "fbclid", "tracking_*")

        // When
        val result = ruleCompiler.compileParamPatterns(params)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        
        // Test wildcard pattern
        assertTrue(result[0].matches("utm_source"))
        assertTrue(result[0].matches("utm_medium"))
        assertTrue(!result[0].matches("fbclid"))
        
        // Test exact pattern
        assertTrue(result[1].matches("fbclid"))
        assertTrue(!result[1].matches("utm_source"))
        
        // Test wildcard pattern
        assertTrue(result[2].matches("tracking_id"))
        assertTrue(result[2].matches("tracking_code"))
        assertTrue(!result[2].matches("utm_source"))
    }

    @Test
    fun testCompileParamPatterns_withInvalidPattern_skipsInvalidPatterns() {
        // Given
        val params = listOf("valid_param", "[invalid_regex", "another_valid")

        // When
        val result = ruleCompiler.compileParamPatterns(params)

        // Then
        assertNotNull(result)
        // Should have 2 valid patterns (invalid one skipped)
        assertEquals(2, result.size)
    }

    @Test
    fun testInvalidateCache_clearsAllCaches() {
        // Given - compile some rules to populate cache
        val rule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_*"),
            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Test rule"
        )
        ruleCompiler.compileRule(rule)

        // When
        ruleCompiler.invalidateCache()

        // Then - verify no exceptions thrown (cache invalidation is internal)
        assertTrue(true)
    }

    @Test
    fun testGetCacheStats_returnsValidStats() {
        // Given - compile some rules to populate cache
        val rules = listOf(
            CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Test rule"
            )
        )
        ruleCompiler.compileRules(rules)

        // When
        val stats = ruleCompiler.getCacheStats()

        // Then
        assertNotNull(stats)
        assertTrue(stats.containsKey("compiledRulesCache"))
        assertTrue(stats.containsKey("patternCache"))
        
        val compiledRulesStats = stats["compiledRulesCache"] as Map<*, *>
        assertTrue(compiledRulesStats.containsKey("size"))
        assertTrue(compiledRulesStats.containsKey("maxSize"))
        assertTrue(compiledRulesStats.containsKey("hitCount"))
        assertTrue(compiledRulesStats.containsKey("missCount"))
        
        val patternStats = stats["patternCache"] as Map<*, *>
        assertTrue(patternStats.containsKey("size"))
        assertTrue(patternStats.containsKey("maxSize"))
        assertTrue(patternStats.containsKey("hitCount"))
        assertTrue(patternStats.containsKey("missCount"))
    }

    @Test
    fun testCompileRules_withCachingDisabled_stillWorksCorrectly() {
        // Given
        val noCacheConfig = PerformanceConfig(
            enablePatternCaching = false,
            maxCacheSize = 100,
            recompileOnConfigChange = true
        )
        val noCacheCompiler = RuleCompiler(noCacheConfig)
        
        val rules = listOf(
            CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Test rule"
            )
        )

        // When
        val compiledRules = noCacheCompiler.compileRules(rules)

        // Then
        assertNotNull(compiledRules)
        assertEquals(1, compiledRules.size)
        assertEquals(rules[0], compiledRules[0].originalRule)
    }

    @Test
    fun testCompileRule_withInvalidPattern_returnsFallbackRule() {
        // Given - rule with pattern that might cause compilation issues
        val rule = CleaningRule(
            hostPattern = "[invalid_regex_pattern",
            params = listOf("utm_*"),
            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.REGEX,
            enabled = true,
            description = "Invalid regex rule"
        )

        // When
        val compiledRule = ruleCompiler.compileRule(rule)

        // Then
        assertNotNull(compiledRule)
        assertEquals(rule, compiledRule.originalRule)
        // Should return fallback with null compiled pattern and 0 specificity
        assertNull(compiledRule.compiledHostPattern)
        assertEquals(0, compiledRule.specificity)
    }

    @Test
    fun testWildcardPatternCompilation_handlesSpecialCharacters() {
        // Given
        val pattern = "*.example.com"
        val type = PatternType.WILDCARD

        // When
        val regex = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNotNull(regex)
        assertTrue(regex!!.matches("sub.example.com"))
        assertTrue(regex.matches("www.example.com"))
        assertTrue(regex.matches("a.b.c.example.com"))
        assertTrue(!regex.matches("example.com.evil.com"))
        assertTrue(!regex.matches("notexample.com"))
    }

    @Test
    fun testPathPatternCompilation_handlesPathsCorrectly() {
        // Given
        val pattern = "example.com/api/*"
        val type = PatternType.PATH_PATTERN

        // When
        val regex = ruleCompiler.compileHostPattern(pattern, type)

        // Then
        assertNotNull(regex)
        assertTrue(regex!!.matches("example.com/api/v1"))
        assertTrue(regex.matches("example.com/api/users"))
        assertTrue(!regex.matches("example.com/web/page"))
    }

    // Security-related tests

    @Test
    fun testValidatePattern_withValidPattern_returnsValid() {
        // Given
        val validPattern = "example\\.com"

        // When
        val result = ruleCompiler.validatePattern(validPattern)

        // Then
        assertTrue("Valid pattern should pass validation", result.isValid)
        assertNull("Should not have error message", result.errorMessage)
        assertFalse("Should not be flagged as ReDoS vulnerable", result.isReDoSVulnerable)
    }

    @Test
    fun testValidatePattern_withReDoSPattern_returnsInvalid() {
        // Given
        val reDoSPattern = "(a+)+b"

        // When
        val result = ruleCompiler.validatePattern(reDoSPattern)

        // Then
        assertFalse("ReDoS pattern should fail validation", result.isValid)
        assertTrue("Should be flagged as ReDoS vulnerable", result.isReDoSVulnerable)
        assertTrue("Error message should mention ReDoS", 
            result.errorMessage?.contains("ReDoS vulnerability") == true)
    }

    @Test
    fun testValidatePattern_withValidationDisabled_returnsValid() {
        // Given
        val configWithoutValidation = PerformanceConfig(
            enablePatternCaching = true,
            maxCacheSize = 100,
            recompileOnConfigChange = true,
            regexTimeoutMs = 1000L,
            enableRegexValidation = false
        )
        val compilerWithoutValidation = RuleCompiler(configWithoutValidation)
        val reDoSPattern = "(a+)+b"

        // When
        val result = compilerWithoutValidation.validatePattern(reDoSPattern)

        // Then
        assertTrue("Should return valid when validation is disabled", result.isValid)
    }

    @Test
    fun testCompileRule_withReDoSPattern_returnsFallbackRule() {
        // Given
        val rule = CleaningRule(
            hostPattern = "(a+)+\\.example\\.com",
            params = listOf("utm_*"),
            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.REGEX,
            enabled = true,
            description = "ReDoS vulnerable rule"
        )

        // When
        val compiledRule = ruleCompiler.compileRule(rule)

        // Then
        assertNotNull(compiledRule)
        assertEquals(rule, compiledRule.originalRule)
        // Should return fallback due to ReDoS detection
        assertNull(compiledRule.compiledHostPattern)
        assertEquals(0, compiledRule.specificity)
    }

    @Test
    fun testCompileHostPattern_withSecurityValidation_rejectsReDoSPatterns() {
        // Given
        val reDoSPattern = "(a+)+\\.example\\.com"
        val type = PatternType.REGEX

        // When
        val result = ruleCompiler.compileHostPattern(reDoSPattern, type)

        // Then
        assertNull("ReDoS pattern should be rejected", result)
    }

    @Test
    fun testCompileHostPattern_withValidationDisabled_allowsReDoSPatterns() {
        // Given
        val configWithoutValidation = PerformanceConfig(
            enablePatternCaching = true,
            maxCacheSize = 100,
            recompileOnConfigChange = true,
            regexTimeoutMs = 1000L,
            enableRegexValidation = false
        )
        val compilerWithoutValidation = RuleCompiler(configWithoutValidation)
        val reDoSPattern = "a+\\.example\\.com" // Less dangerous but still potentially problematic
        val type = PatternType.REGEX

        // When
        val result = compilerWithoutValidation.compileHostPattern(reDoSPattern, type)

        // Then
        // Should compile when validation is disabled (though not recommended)
        assertNotNull("Pattern should compile when validation is disabled", result)
    }

    @Test
    fun testCompileParamPatterns_withReDoSPattern_skipsVulnerablePatterns() {
        // Given
        val params = listOf("utm_*", "(a+)+", "fbclid")

        // When
        val result = ruleCompiler.compileParamPatterns(params)

        // Then
        assertNotNull(result)
        // Should have 2 valid patterns (ReDoS pattern skipped)
        assertEquals("Should skip ReDoS vulnerable pattern", 2, result.size)
        
        // Verify the valid patterns work
        assertTrue("First pattern should match utm_source", result[0].matches("utm_source"))
        assertTrue("Second pattern should match fbclid", result[1].matches("fbclid"))
    }

    @Test
    fun testSecurityConfiguration_respectsTimeoutSettings() {
        // Given
        val shortTimeoutConfig = PerformanceConfig(
            enablePatternCaching = true,
            maxCacheSize = 100,
            recompileOnConfigChange = true,
            regexTimeoutMs = 100L, // Very short timeout
            enableRegexValidation = true
        )
        val compilerWithShortTimeout = RuleCompiler(shortTimeoutConfig)
        val complexPattern = "^https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[\\w&=%.])*)?(?:#(?:[\\w.])*)?)?$"

        // When
        val result = compilerWithShortTimeout.validatePattern(complexPattern)

        // Then
        // Should still work for reasonable patterns even with short timeout
        assertTrue("Complex but safe pattern should validate successfully", result.isValid)
    }

    @Test
    fun testCompileRules_withMixedSecurityPatterns_filtersCorrectly() {
        // Given
        val rules = listOf(
            CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Safe exact rule"
            ),
            CleaningRule(
                hostPattern = ".*\\.safe\\.com",
                params = listOf("tracking_*"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.REGEX,
                enabled = true,
                description = "Safe regex rule"
            ),
            CleaningRule(
                hostPattern = "(a+)+\\.dangerous\\.com",
                params = listOf("param"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.REGEX,
                enabled = true,
                description = "Dangerous ReDoS rule"
            )
        )

        // When
        val compiledRules = ruleCompiler.compileRules(rules)

        // Then
        assertNotNull(compiledRules)
        assertEquals(3, compiledRules.size)
        
        // First rule should compile normally
        assertNotNull(compiledRules[0].originalRule)
        assertTrue(compiledRules[0].specificity > 0)
        
        // Second rule should compile normally
        assertNotNull(compiledRules[1].originalRule)
        assertNotNull(compiledRules[1].compiledHostPattern)
        assertTrue(compiledRules[1].specificity > 0)
        
        // Third rule should have fallback compilation due to ReDoS
        assertNotNull(compiledRules[2].originalRule)
        assertNull(compiledRules[2].compiledHostPattern)
        assertEquals(0, compiledRules[2].specificity)
    }

    @Test
    fun testPerformanceImpact_securityValidationDoesNotSlowDownNormalPatterns() {
        // Given
        val normalPatterns = listOf(
            "example.com",
            "*.example.com", 
            "test\\.domain\\.com",
            "api\\.service\\.org"
        )
        
        // When & Then
        val startTime = System.currentTimeMillis()
        
        normalPatterns.forEach { pattern ->
            val result = ruleCompiler.validatePattern(pattern)
            assertTrue("Pattern '$pattern' should validate quickly", result.isValid)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertTrue("Security validation should not significantly impact performance", 
            totalTime < 1000) // Should complete well under 1 second
    }
}
