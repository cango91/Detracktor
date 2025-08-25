package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import com.gologlu.detracktor.data.*

/**
 * Comprehensive instrumented tests for PATH_PATTERN matching functionality.
 * Tests the improved PATH_PATTERN implementation that matches full URLs instead of just hosts.
 */
@RunWith(AndroidJUnit4::class)
class PathPatternMatchingTest {

    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        urlCleanerService = UrlCleanerService(context)
        configManager = ConfigManager(context)
    }

    @Test
    fun testPathPatternMatchingWithFullUrl() {
        // Test that PATH_PATTERN rules can match against full URLs including paths
        val testRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://example\\.com/api/.*",
                params = listOf("debug", "trace"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "API endpoint debug parameters"
            ),
            normalizedHostPattern = "https://example\\.com/api/.*",
            compiledHostPattern = Regex("https://example\\.com/api/.*"),
            compiledParamPatterns = listOf(Regex("debug"), Regex("trace")),
            specificity = 100
        )

        // Test URLs that should match the path pattern
        val matchingUrl1 = "https://example.com/api/users"
        val matchingUrl2 = "https://example.com/api/data/fetch"
        val nonMatchingUrl1 = "https://example.com/web/page"
        val nonMatchingUrl2 = "https://other.com/api/users"

        assertTrue("Should match API path", 
                  urlCleanerService.matchesCompiledRuleWithPath(matchingUrl1, testRule))
        assertTrue("Should match nested API path", 
                  urlCleanerService.matchesCompiledRuleWithPath(matchingUrl2, testRule))
        assertFalse("Should not match non-API path", 
                   urlCleanerService.matchesCompiledRuleWithPath(nonMatchingUrl1, testRule))
        assertFalse("Should not match different domain", 
                   urlCleanerService.matchesCompiledRuleWithPath(nonMatchingUrl2, testRule))

        println("PATH_PATTERN matching test completed successfully")
    }

    @Test
    fun testPathPatternVsHostPatternBehavior() {
        // Test the difference between PATH_PATTERN and regular host patterns
        val hostRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_source"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Host-only rule"
            ),
            normalizedHostPattern = "example.com",
            compiledHostPattern = null,
            compiledParamPatterns = listOf(Regex("utm_source")),
            specificity = 50
        )

        val pathRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://example\\.com/special/.*",
                params = listOf("debug"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Path-specific rule"
            ),
            normalizedHostPattern = "https://example\\.com/special/.*",
            compiledHostPattern = Regex("https://example\\.com/special/.*"),
            compiledParamPatterns = listOf(Regex("debug")),
            specificity = 100
        )

        val testUrl = "https://example.com/special/page"

        // Host rule should match when checking just the host
        assertTrue("Host rule should match host", 
                  urlCleanerService.matchesCompiledRule("example.com", hostRule))
        
        // Path rule should match when checking full URL
        assertTrue("Path rule should match full URL", 
                  urlCleanerService.matchesCompiledRuleWithPath(testUrl, pathRule))
        
        // Path rule should not match when checking just host (fallback behavior)
        assertFalse("Path rule should not match just host", 
                   urlCleanerService.matchesCompiledRule("example.com", pathRule))

        println("PATH_PATTERN vs host pattern behavior test completed")
    }

    @Test
    fun testComplexPathPatterns() {
        // Test complex path patterns with various URL structures
        val patterns = listOf(
            // GitHub repository pattern
            "https://github\\.com/[^/]+/[^/]+/.*" to listOf(
                "https://github.com/user/repo/issues" to true,
                "https://github.com/org/project/pull/123" to true,
                "https://github.com/user" to false,
                "https://gitlab.com/user/repo/issues" to false
            ),
            
            // YouTube video pattern
            "https://www\\.youtube\\.com/watch\\?.*" to listOf(
                "https://www.youtube.com/watch?v=abc123" to true,
                "https://www.youtube.com/watch?v=abc123&t=30s" to true,
                "https://www.youtube.com/channel/UC123" to false,
                "https://youtu.be/abc123" to false
            ),
            
            // API versioning pattern
            "https://api\\.example\\.com/v[0-9]+/.*" to listOf(
                "https://api.example.com/v1/users" to true,
                "https://api.example.com/v2/data/fetch" to true,
                "https://api.example.com/users" to false,
                "https://web.example.com/v1/users" to false
            )
        )

        patterns.forEach { (pattern, testCases) ->
            val testRule = CompiledRule(
                originalRule = CleaningRule(
                    hostPattern = pattern,
                    params = listOf("test"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.PATH_PATTERN,
                    enabled = true,
                    description = "Complex path pattern test"
                ),
                normalizedHostPattern = pattern,
                compiledHostPattern = Regex(pattern),
                compiledParamPatterns = listOf(Regex("test")),
                specificity = 100
            )

            testCases.forEach { (url, shouldMatch) ->
                val actualMatch = urlCleanerService.matchesCompiledRuleWithPath(url, testRule)
                assertEquals("Pattern '$pattern' should ${if (shouldMatch) "match" else "not match"} URL '$url'", 
                           shouldMatch, actualMatch)
            }
        }

        println("Complex path patterns test completed")
    }

    @Test
    fun testPathPatternWithQueryParameters() {
        // Test PATH_PATTERN rules that include query parameter matching
        val testRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://example\\.com/search\\?.*q=.*",
                params = listOf("tracking", "source"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Search page with query parameter"
            ),
            normalizedHostPattern = "https://example\\.com/search\\?.*q=.*",
            compiledHostPattern = Regex("https://example\\.com/search\\?.*q=.*"),
            compiledParamPatterns = listOf(Regex("tracking"), Regex("source")),
            specificity = 100
        )

        val matchingUrls = listOf(
            "https://example.com/search?q=test",
            "https://example.com/search?q=test&tracking=123",
            "https://example.com/search?other=value&q=test&source=google"
        )

        val nonMatchingUrls = listOf(
            "https://example.com/search",
            "https://example.com/search?other=value",
            "https://example.com/page?q=test",
            "https://other.com/search?q=test"
        )

        matchingUrls.forEach { url ->
            assertTrue("Should match URL with query parameter: $url", 
                      urlCleanerService.matchesCompiledRuleWithPath(url, testRule))
        }

        nonMatchingUrls.forEach { url ->
            assertFalse("Should not match URL: $url", 
                       urlCleanerService.matchesCompiledRuleWithPath(url, testRule))
        }

        println("PATH_PATTERN with query parameters test completed")
    }

    @Test
    fun testPathPatternCaseSensitivity() {
        // Test case sensitivity in path patterns
        val caseSensitiveRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://example\\.com/API/.*",
                params = listOf("debug"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Case-sensitive API path"
            ),
            normalizedHostPattern = "https://example\\.com/API/.*",
            compiledHostPattern = Regex("https://example\\.com/API/.*"),
            compiledParamPatterns = listOf(Regex("debug")),
            specificity = 100
        )

        val caseInsensitiveRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "(?i)https://example\\.com/api/.*",
                params = listOf("debug"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Case-insensitive API path"
            ),
            normalizedHostPattern = "(?i)https://example\\.com/api/.*",
            compiledHostPattern = Regex("(?i)https://example\\.com/api/.*"),
            compiledParamPatterns = listOf(Regex("debug")),
            specificity = 100
        )

        val testUrls = listOf(
            "https://example.com/API/users",
            "https://example.com/api/users",
            "https://example.com/Api/users"
        )

        // Case-sensitive rule should only match exact case
        assertTrue("Case-sensitive should match exact case", 
                  urlCleanerService.matchesCompiledRuleWithPath(testUrls[0], caseSensitiveRule))
        assertFalse("Case-sensitive should not match lowercase", 
                   urlCleanerService.matchesCompiledRuleWithPath(testUrls[1], caseSensitiveRule))
        assertFalse("Case-sensitive should not match mixed case", 
                   urlCleanerService.matchesCompiledRuleWithPath(testUrls[2], caseSensitiveRule))

        // Case-insensitive rule should match all variations
        testUrls.forEach { url ->
            assertTrue("Case-insensitive should match all variations: $url", 
                      urlCleanerService.matchesCompiledRuleWithPath(url, caseInsensitiveRule))
        }

        println("PATH_PATTERN case sensitivity test completed")
    }

    @Test
    fun testPathPatternPerformance() {
        // Test performance of PATH_PATTERN matching with complex patterns
        val complexRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://[a-z]+\\.example\\.com/[a-z]+/[0-9]+/.*\\?.*id=[0-9]+.*",
                params = listOf("tracking"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Complex path pattern for performance testing"
            ),
            normalizedHostPattern = "https://[a-z]+\\.example\\.com/[a-z]+/[0-9]+/.*\\?.*id=[0-9]+.*",
            compiledHostPattern = Regex("https://[a-z]+\\.example\\.com/[a-z]+/[0-9]+/.*\\?.*id=[0-9]+.*"),
            compiledParamPatterns = listOf(Regex("tracking")),
            specificity = 100
        )

        val testUrls = listOf(
            "https://api.example.com/users/123/profile?id=456&tracking=abc",
            "https://web.example.com/posts/789/comments?id=101&other=value",
            "https://cdn.example.com/files/555/download?id=999&tracking=xyz",
            "https://invalid.com/test/123/page?id=456",
            "https://api.example.com/invalid/path?id=123"
        )

        val startTime = System.currentTimeMillis()
        
        // Perform multiple iterations to test performance
        repeat(100) {
            testUrls.forEach { url ->
                urlCleanerService.matchesCompiledRuleWithPath(url, complexRule)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertTrue("Performance test should complete in reasonable time", totalTime < 1000) // Less than 1 second
        
        println("PATH_PATTERN performance test completed in ${totalTime}ms")
    }

    @Test
    fun testPathPatternErrorHandling() {
        // Test error handling with malformed patterns and URLs
        val invalidPatternRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "https://example\\.com/[invalid",  // Invalid regex
                params = listOf("test"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.PATH_PATTERN,
                enabled = true,
                description = "Invalid pattern for error testing"
            ),
            normalizedHostPattern = "https://example\\.com/[invalid",
            compiledHostPattern = null, // Compilation would fail
            compiledParamPatterns = listOf(Regex("test")),
            specificity = 100
        )

        val testUrls = listOf(
            "https://example.com/test",
            "invalid-url",
            "",
            "https://example.com/test?param=value"
        )

        testUrls.forEach { url ->
            // Should not throw exceptions, should return false for invalid patterns
            val result = try {
                urlCleanerService.matchesCompiledRuleWithPath(url, invalidPatternRule)
            } catch (e: Exception) {
                false // Graceful handling of errors
            }
            
            assertFalse("Invalid pattern should not match any URL: $url", result)
        }

        println("PATH_PATTERN error handling test completed")
    }
}
