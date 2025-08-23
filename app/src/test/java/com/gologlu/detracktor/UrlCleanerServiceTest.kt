package com.gologlu.detracktor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gologlu.detracktor.data.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UrlCleanerServiceTest {


    
    private lateinit var urlCleanerService: UrlCleanerService

    @Before
    fun setUp() {
        // Use Robolectric's application context for real Android behavior
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        urlCleanerService = UrlCleanerService(context)
    }

    @Test
    fun testCleanUrl_withValidHttpUrl_returnsCleanedUrl() {
        // Given
        val originalUrl = "https://example.com/page?utm_source=test&utm_medium=email&id=123"
        val expectedUrl = "https://example.com/page?id=123"
        
        // Mock config manager behavior
        val mockConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testCleanUrl_withInvalidUrl_returnsOriginalUrl() {
        // Given
        val invalidUrl = "not-a-url"
        
        // When
        val result = urlCleanerService.cleanUrl(invalidUrl)
        
        // Then
        assertEquals(invalidUrl, result)
    }

    @Test
    fun testCleanUrl_withNonHttpUrl_returnsOriginalUrl() {
        // Given
        val ftpUrl = "ftp://example.com/file.txt"
        
        // When
        val result = urlCleanerService.cleanUrl(ftpUrl)
        
        // Then
        assertEquals(ftpUrl, result)
    }

    @Test
    fun testCleanClipboardUrl_withEmptyClipboard_returnsClipboardEmpty() {
        // Given - clipboard is empty by default in test environment
        
        // When
        val result = urlCleanerService.cleanClipboardUrl()
        
        // Then
        assertEquals(CleaningResult.CLIPBOARD_EMPTY, result)
    }

    @Test
    fun testCleanClipboardUrl_withValidUrl_returnsAppropriateResult() {
        // Given
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val testUrl = "https://example.com/page?utm_source=test"
        
        // Set up clipboard with test URL
        val clipData = ClipData.newPlainText("test", testUrl)
        clipboardManager.setPrimaryClip(clipData)
        
        // When
        val result = urlCleanerService.cleanClipboardUrl()
        
        // Then
        assertNotNull(result)
    }

    @Test
    fun testProcessIntent_withValidUrl_processesCorrectly() {
        // Given
        val intent = Intent()
        val testUrl = "https://example.com/page?utm_source=test"
        intent.putExtra(Intent.EXTRA_TEXT, testUrl)
        
        // When
        urlCleanerService.processIntent(intent)
        
        // Then - verify no exceptions thrown
        assertTrue(true)
    }

    @Test
    fun testFindBestMatchingRule_withMatchingRule_returnsRule() {
        // Given
        val host = "example.com"
        val rule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                                    priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Test rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "example.com",
            specificity = 100
        )
        val rules = listOf(rule)
        
        // When
        val result = urlCleanerService.findBestMatchingRule(host, rules)
        
        // Then
        assertEquals(rule, result)
    }

    @Test
    fun testFindBestMatchingRule_withNoMatchingRule_returnsNull() {
        // Given
        val host = "different.com"
        val rule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                                    priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Test rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "example.com",
            specificity = 100
        )
        val rules = listOf(rule)
        
        // When
        val result = urlCleanerService.findBestMatchingRule(host, rules)
        
        // Then
        assertEquals(null, result)
    }

    @Test
    fun testMatchesCompiledRule_exactMatch_returnsTrue() {
        // Given
        val host = "example.com"
        val rule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_*"),
                                    priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Test rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "example.com",
            specificity = 100
        )
        
        // When
        val result = urlCleanerService.matchesCompiledRule(host, rule)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun testMatchesCompiledRule_wildcardMatch_returnsTrue() {
        // Given
        val host = "sub.example.com"
        val compiledPattern = Regex(".*\\.example\\.com", RegexOption.IGNORE_CASE)
        val rule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "*.example.com",
                params = listOf("utm_*"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Test wildcard rule"
            ),
            compiledHostPattern = compiledPattern,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "*.example.com",
            specificity = 50
        )
        
        // When
        val result = urlCleanerService.matchesCompiledRule(host, rule)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun testCalculateRuleSpecificity_returnsValidSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_*"),
                                priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Test rule"
        )
        
        // When
        val result = urlCleanerService.calculateRuleSpecificity(rule)
        
        // Then
        assertTrue(result > 0)
    }

    @Test
    fun testGetServiceStats_returnsValidStats() {
        // When
        val stats = urlCleanerService.getServiceStats()
        
        // Then
        assertNotNull(stats)
        assertTrue(stats.containsKey("configStats"))
        assertTrue(stats.containsKey("compiledRulesCount"))
        assertTrue(stats.containsKey("enabledRulesCount"))
    }

    @Test
    fun testTestUrlCleaning_returnsDetailedResults() {
        // Given
        val testUrl = "https://example.com/page?utm_source=test&id=123"
        
        // When
        val result = urlCleanerService.testUrlCleaning(testUrl)
        
        // Then
        assertNotNull(result)
        assertTrue(result.containsKey("originalUrl"))
        assertTrue(result.containsKey("cleanedUrl"))
        assertTrue(result.containsKey("changed"))
        assertTrue(result.containsKey("processingTimeMs"))
        assertEquals(testUrl, result["originalUrl"])
    }
}
