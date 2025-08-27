package com.gologlu.detracktor.utils

import org.junit.Test
import org.junit.Assert.*

// Import the required classes
import com.gologlu.detracktor.utils.UrlPrivacyLevel
import com.gologlu.detracktor.utils.ClipboardContentType
import com.gologlu.detracktor.utils.PrivacySettings

/**
 * Pure unit tests for UrlPrivacyAnalyzer that don't depend on Android APIs.
 * Android-dependent tests are in UrlPrivacyAnalyzerInstrumentedTest.
 */
class UrlPrivacyAnalyzerTest {
    
    private val analyzer = UrlPrivacyAnalyzer()
    
    @Test
    fun testHasSensitiveCredentials() {
        assertTrue(analyzer.hasSensitiveCredentials("https://user:pass@example.com"))
        assertTrue(analyzer.hasSensitiveCredentials("http://admin:secret123@api.example.com/path"))
        assertFalse(analyzer.hasSensitiveCredentials("https://example.com"))
        assertFalse(analyzer.hasSensitiveCredentials("https://example.com/path"))
    }
    
    @Test
    fun testShouldDisplayContent() {
        val settings = PrivacySettings(hideNonUriContent = true)
        
        assertTrue(analyzer.shouldDisplayContent("https://example.com", ClipboardContentType.URI, settings))
        assertTrue(analyzer.shouldDisplayContent("https://user:pass@example.com", ClipboardContentType.SENSITIVE_URI, settings))
        assertFalse(analyzer.shouldDisplayContent("some text", ClipboardContentType.NON_URI, settings))
    }
    
    @Test
    fun testShouldBlurContent() {
        val settings = PrivacySettings(blurSensitiveParams = true, blurCredentials = true)
        
        assertFalse(analyzer.shouldBlurContent("https://example.com", ClipboardContentType.URI, settings))
        assertTrue(analyzer.shouldBlurContent("https://user:pass@example.com", ClipboardContentType.SENSITIVE_URI, settings))
        assertTrue(analyzer.shouldBlurContent("https://user:pass@example.com", ClipboardContentType.URI, settings))
    }
    
    @Test
    fun testCreatePrivacySafePreview_nonUri() {
        val nonUriContent = "This is sensitive data"
        val result = analyzer.createPrivacySafePreview(nonUriContent)
        assertEquals("[Non-URI content hidden for privacy]", result)
    }
    
    @Test
    fun testCreatePrivacySafePreview_normalUrl() {
        val normalUrl = "https://example.com/page"
        val urlResult = analyzer.createPrivacySafePreview(normalUrl)
        assertEquals(normalUrl, urlResult)
    }
    
    @Test
    fun testCreatePrivacySafePreview_longUrl() {
        val longUrl = "https://example.com/very/long/path/that/exceeds/the/maximum/preview/length/and/should/be/truncated/properly"
        val result = analyzer.createPrivacySafePreview(longUrl, 50)
        assertTrue(result.length <= 53) // 50 + "..."
        assertTrue(result.endsWith("..."))
    }
    
    @Test
    fun testUriPatternMatching() {
        // Test URI pattern matching (doesn't require Android Uri.parse)
        val uriPattern = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
        
        assertTrue(uriPattern.containsMatchIn("https://example.com"))
        assertTrue(uriPattern.containsMatchIn("http://example.com"))
        assertTrue(uriPattern.containsMatchIn("ftp://example.com"))
        assertFalse(uriPattern.containsMatchIn("just some text"))
        assertFalse(uriPattern.containsMatchIn("example.com"))
    }
}
