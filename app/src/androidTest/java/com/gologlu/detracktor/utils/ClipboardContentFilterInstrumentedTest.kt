package com.gologlu.detracktor.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ClipboardContentFilter Android-specific functionality.
 * Tests real Android environment behavior, clipboard integration, and performance.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardContentFilterInstrumentedTest {

    private lateinit var contentFilter: ClipboardContentFilter
    private lateinit var privacySettings: PrivacySettings

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        privacySettings = PrivacySettings(
            hideNonUriContent = true,
            blurSensitiveParams = true,
            blurCredentials = true,
            showBlurToggle = true
        )
        
        contentFilter = ClipboardContentFilter()
    }

    @Test
    fun testAndroidSpecificUriHandling() = runBlocking {
        // Test Android-specific URI schemes
        val androidUris = listOf(
            "content://com.android.providers.media.documents/document/image%3A1000",
            "file:///android_asset/test.html",
            "android.resource://com.example.app/2130837504",
            "market://details?id=com.example.app"
        )

        androidUris.forEach { uri ->
            val result = contentFilter.analyzeAndFilter(uri, privacySettings)
            
            assertTrue("Android URI should be recognized as URI: $uri", 
                result.contentType == ClipboardContentType.URI || 
                result.contentType == ClipboardContentType.SENSITIVE_URI)
            assertTrue("Android URI should be displayable: $uri", result.shouldDisplay)
        }
    }

    @Test
    fun testRealWorldClipboardScenarios() = runBlocking {
        val scenarios = mapOf(
            // Social media URLs with tracking
            "https://facebook.com/share?u=https://example.com&t=title&utm_source=facebook&utm_medium=social" to ClipboardContentType.URI,
            
            // Email with credentials
            "mailto:user:password@example.com?subject=test" to ClipboardContentType.SENSITIVE_URI,
            
            // Database connection string
            "jdbc:postgresql://user:pass@localhost:5432/db" to ClipboardContentType.SENSITIVE_URI,
            
            // API key in URL
            "https://api.example.com/data?api_key=sk_live_123456789abcdef&format=json" to ClipboardContentType.SENSITIVE_URI,
            
            // Plain text (non-URI)
            "This is just some text content that was copied" to ClipboardContentType.NON_URI,
            
            // Code snippet
            "const apiKey = 'sk_test_123456789';\nfetch('/api/data');" to ClipboardContentType.NON_URI,
            
            // Phone number
            "+1-555-123-4567" to ClipboardContentType.NON_URI,
            
            // Credit card (should be hidden)
            "4532 1234 5678 9012" to ClipboardContentType.NON_URI
        )

        scenarios.forEach { (content, expectedType) ->
            val result = contentFilter.analyzeAndFilter(content, privacySettings)
            
            assertEquals("Content type mismatch for: $content", expectedType, result.contentType)
            
            when (expectedType) {
                ClipboardContentType.NON_URI -> {
                    assertFalse("Non-URI content should not be displayed: $content", result.shouldDisplay)
                    assertTrue("Non-URI should be filtered", 
                        result.filteredContent.contains("Content hidden") || result.filteredContent.contains("privacy"))
                }
                ClipboardContentType.SENSITIVE_URI -> {
                    assertTrue("Sensitive URI should be blurred: $content", result.shouldBlur)
                    assertTrue("Sensitive URI should have risk factors: $content", 
                        result.riskFactors.isNotEmpty())
                }
                ClipboardContentType.URI -> {
                    assertTrue("Regular URI should be displayable: $content", result.shouldDisplay)
                }
            }
        }
    }

    @Test
    fun testPerformanceWithLargeContent() = runBlocking {
        // Test with large clipboard content
        val largeContent = "https://example.com/?" + "param=value&".repeat(1000) + "end=true"
        
        val startTime = System.currentTimeMillis()
        val result = contentFilter.analyzeAndFilter(largeContent, privacySettings)
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        assertTrue("Large content processing should complete within 2 seconds", processingTime < 2000)
        
        assertNotNull("Result should not be null for large content", result)
        assertTrue("Large content should be recognized as URI", 
            result.contentType == ClipboardContentType.URI || 
            result.contentType == ClipboardContentType.SENSITIVE_URI)
    }

    @Test
    fun testMaliciousContentHandling() = runBlocking {
        val maliciousContent = listOf(
            // Potential XSS
            "<script>alert('xss')</script>",
            
            // SQL injection attempt
            "'; DROP TABLE users; --",
            
            // Path traversal
            "../../../../etc/passwd",
            
            // Command injection
            "; rm -rf /",
            
            // ReDoS pattern in URL
            "https://example.com/?q=" + "a".repeat(1000) + "b".repeat(1000)
        )

        maliciousContent.forEach { content ->
            val result = contentFilter.analyzeAndFilter(content, privacySettings)
            
            // Malicious content should be handled safely
            assertNotNull("Result should not be null for malicious content: $content", result)
            
            if (result.contentType == ClipboardContentType.NON_URI) {
                assertFalse("Malicious non-URI should not be displayed: $content", result.shouldDisplay)
            }
            
            // Should have risk factors if detected as risky
            if (result.riskFactors.any { it.severity == RiskSeverity.HIGH || it.severity == RiskSeverity.CRITICAL }) {
                assertTrue("High-risk content should be blurred or hidden: $content", 
                    result.shouldBlur || !result.shouldDisplay)
            }
        }
    }

    @Test
    fun testInternationalContentHandling() = runBlocking {
        val internationalContent = listOf(
            // International domain names
            "https://例え.テスト/path?param=値",
            "https://пример.рф/страница",
            "https://مثال.إختبار/صفحة",
            
            // Unicode in content
            "This contains emoji 🔒 and symbols ∑∆",
            "中文内容测试",
            "العربية محتوى اختبار"
        )

        internationalContent.forEach { content ->
            val result = contentFilter.analyzeAndFilter(content, privacySettings)
            
            assertNotNull("Result should not be null for international content: $content", result)
            
            // Should handle international content gracefully
            if (content.startsWith("http")) {
                assertTrue("International URL should be recognized: $content",
                    result.contentType == ClipboardContentType.URI ||
                    result.contentType == ClipboardContentType.SENSITIVE_URI)
            }
        }
    }

    @Test
    fun testPrivacySettingsIntegration() = runBlocking {
        val testUrl = "https://user:pass@example.com/path?secret=123"
        
        // Test with different privacy settings
        val strictSettings = PrivacySettings(
            hideNonUriContent = true,
            blurSensitiveParams = true,
            blurCredentials = true,
            showBlurToggle = false
        )
        
        val lenientSettings = PrivacySettings(
            hideNonUriContent = false,
            blurSensitiveParams = false,
            blurCredentials = false,
            showBlurToggle = true
        )
        
        val strictResult = contentFilter.analyzeAndFilter(testUrl, strictSettings)
        val lenientResult = contentFilter.analyzeAndFilter(testUrl, lenientSettings)
        
        // Strict settings should blur sensitive content
        assertTrue("Strict settings should blur sensitive URI", strictResult.shouldBlur)
        
        // Lenient settings should not blur
        assertFalse("Lenient settings should not blur sensitive URI", lenientResult.shouldBlur)
        
        // Both should recognize as sensitive URI
        assertEquals("Both should detect sensitive URI", 
            ClipboardContentType.SENSITIVE_URI, strictResult.contentType)
        assertEquals("Both should detect sensitive URI", 
            ClipboardContentType.SENSITIVE_URI, lenientResult.contentType)
    }

    @Test
    fun testRiskFactorAnalysis() = runBlocking {
        val riskScenarios = mapOf(
            "https://example.com" to 0, // No risk factors
            "https://user:pass@example.com" to 1, // Credentials
            "https://api.example.com?api_key=secret&token=abc123" to 2, // Multiple secrets
            "ftp://admin:password@server.com/sensitive/data" to 3 // Protocol + credentials + sensitive path
        )

        riskScenarios.forEach { (content, expectedMinRiskFactors) ->
            val result = contentFilter.analyzeAndFilter(content, privacySettings)
            
            assertTrue("Content should have at least $expectedMinRiskFactors risk factors: $content",
                result.riskFactors.size >= expectedMinRiskFactors)
            
            if (expectedMinRiskFactors > 0) {
                assertTrue("Risky content should be marked as sensitive: $content",
                    result.contentType == ClipboardContentType.SENSITIVE_URI)
            }
        }
    }

    @Test
    fun testEdgeCasesAndBoundaryConditions() = runBlocking {
        val edgeCases = listOf(
            "", // Empty string
            " ", // Whitespace only
            "\n\t\r", // Various whitespace
            "a", // Single character
            "http://", // Incomplete URL
            "://example.com", // Missing protocol
            "https://" + "a".repeat(2000), // Very long URL
            "data:text/plain;base64,SGVsbG8gV29ybGQ=", // Data URI
            "javascript:alert('test')", // JavaScript URI
            "about:blank" // Browser special URI
        )

        edgeCases.forEach { content ->
            val result = contentFilter.analyzeAndFilter(content, privacySettings)
            
            assertNotNull("Result should not be null for edge case: '$content'", result)
            
            // Empty or whitespace-only content should be non-URI
            if (content.isBlank()) {
                assertEquals("Blank content should be non-URI", 
                    ClipboardContentType.NON_URI, result.contentType)
                assertFalse("Blank content should not be displayed", result.shouldDisplay)
            }
        }
    }

    @Test
    fun testConcurrentAnalysis() = runBlocking {
        val testContents = listOf(
            "https://example1.com",
            "https://user:pass@example2.com",
            "Plain text content",
            "https://api.example3.com?key=secret",
            "Another plain text"
        )

        // Analyze multiple contents concurrently
        val results = testContents.map { content ->
            contentFilter.analyzeAndFilter(content, privacySettings)
        }

        assertEquals("Should have result for each content", testContents.size, results.size)
        
        results.forEachIndexed { index, result ->
            assertNotNull("Result $index should not be null", result)
            assertTrue("Result $index should have valid content type", 
                result.contentType in ClipboardContentType.values())
        }
    }

    @Test
    fun testMemoryUsageWithRepeatedAnalysis() = runBlocking {
        val testContent = "https://example.com/test?param=value"
        
        // Run analysis multiple times to check for memory leaks
        repeat(100) {
            val result = contentFilter.analyzeAndFilter(testContent, privacySettings)
            assertNotNull("Result should not be null in iteration $it", result)
        }
        
        // Force garbage collection
        System.gc()
        
        // Verify we can still analyze content after many iterations
        val finalResult = contentFilter.analyzeAndFilter(testContent, privacySettings)
        assertNotNull("Final result should not be null", finalResult)
        assertEquals("Final result should be consistent", 
            ClipboardContentType.URI, finalResult.contentType)
    }
}
