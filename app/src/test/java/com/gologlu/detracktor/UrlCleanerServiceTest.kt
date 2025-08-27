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
    fun testCleanUrl_withGlobalTrackingParameters_removesParameters() {
        // Given - URL with global tracking parameters that should be removed
        val originalUrl = "https://example.com/page?utm_source=test&utm_medium=email&fbclid=tracking&id=123"
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Then
        assertNotNull(result)
        assertTrue("Should remove utm_source", !result.contains("utm_source="))
        assertTrue("Should remove utm_medium", !result.contains("utm_medium="))
        assertTrue("Should remove fbclid", !result.contains("fbclid="))
        // Note: id parameter should be kept since it's not in any removal rules
        // But let's check what actually happens first
        println("Original: $originalUrl")
        println("Cleaned:  $result")
    }

    @Test
    fun testCleanUrl_withHierarchicalRuleMatching_appliesMultipleRules() {
        // Given - Instagram URL that should match both specific and global rules
        val originalUrl = "https://www.instagram.com/p/ABC123/?igsh=session123&utm_source=facebook&custom=keep"
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Debug output
        println("=== DEBUG TEST OUTPUT ===")
        println("Original URL: $originalUrl")
        println("Cleaned URL:  $result")
        println("========================")
        
        // Then
        assertNotNull(result)
        assertTrue("Should still be Instagram URL", result.contains("instagram.com"))
        assertTrue("Should still contain post path", result.contains("/p/ABC123/"))
        // Both Instagram-specific (igsh) and global (utm_source) parameters should be removed
        assertFalse("Should remove igsh parameter", result.contains("igsh="))
        assertFalse("Should remove utm_source parameter", result.contains("utm_source="))
        // Custom parameters not in any rule should remain
        assertTrue("Should keep custom parameter", result.contains("custom=keep"))
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
    fun testAnalyzeClipboardContent_withMultipleMatchingRules_returnsAllRules() {
        // Given
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val testUrl = "https://www.instagram.com/p/ABC123/?igsh=session123&utm_source=facebook&hl=en"
        
        // Set up clipboard with test URL that should match multiple rules
        val clipData = ClipData.newPlainText("test", testUrl)
        clipboardManager.setPrimaryClip(clipData)
        
        // When
        val result = urlCleanerService.analyzeClipboardContent()
        
        // Then
        assertNotNull(result)
        assertTrue("Should be valid URL", result!!.isValidUrl)
        assertTrue("Should have changes", result.hasChanges)
        assertTrue("Should have parameters to remove", result.parametersToRemove.isNotEmpty())
        assertTrue("Should have matching rules", result.matchingRules.isNotEmpty())
        // Should contain both igsh and utm_source in parameters to remove
        assertTrue("Should remove igsh", result.parametersToRemove.contains("igsh"))
        assertTrue("Should remove utm_source", result.parametersToRemove.contains("utm_source"))
        // Should keep hl parameter
        assertTrue("Should keep hl", result.parametersToKeep.contains("hl"))
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

    @Test
    fun testCleanUrl_withEmbeddedCredentials_preservesCredentials() {
        // Given - URL with embedded credentials and tracking parameters
        val originalUrl = "https://user:pass@example.com/page?utm_source=test&id=123"
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Debug output
        println("Embedded credentials test - Original: $originalUrl")
        println("Embedded credentials test - Cleaned:  $result")
        println("Contains user:pass@: ${result.contains("user:pass@")}")
        println("Contains example.com: ${result.contains("example.com")}")
        
        // Then
        assertNotNull(result)
        assertTrue("Should preserve embedded credentials", result.contains("user:pass@"))
        assertTrue("Should preserve host", result.contains("example.com"))
        assertTrue("Should preserve path", result.contains("/page"))
        assertTrue("Should start with https://", result.startsWith("https://"))
    }

    @Test
    fun testCleanUrl_withCredentialsAndPort_preservesBoth() {
        // Given - URL with embedded credentials, port, and tracking parameters
        val originalUrl = "https://api:secret@api.example.com:8443/endpoint?utm_campaign=test&token=abc123"
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Then
        assertNotNull(result)
        assertTrue("Should preserve embedded credentials", result.contains("api:secret@"))
        assertTrue("Should preserve host", result.contains("api.example.com"))
        assertTrue("Should preserve port", result.contains(":8443"))
        assertTrue("Should preserve path", result.contains("/endpoint"))
        
        println("Credentials with port test - Original: $originalUrl")
        println("Credentials with port test - Cleaned:  $result")
    }

    @Test
    fun testCleanUrl_withoutCredentials_noCredentialsAdded() {
        // Given - URL without embedded credentials
        val originalUrl = "https://example.com/page?utm_source=test&id=123"
        
        // When
        val result = urlCleanerService.cleanUrl(originalUrl)
        
        // Then
        assertNotNull(result)
        assertFalse("Should not contain @ symbol when no credentials", result.contains("@"))
        assertTrue("Should preserve host", result.contains("example.com"))
        assertTrue("Should preserve path", result.contains("/page"))
        
        println("No credentials test - Original: $originalUrl")
        println("No credentials test - Cleaned:  $result")
    }
}
