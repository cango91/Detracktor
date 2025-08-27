package com.gologlu.detracktor.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for UrlPrivacyAnalyzer that test Android-specific functionality
 * including clipboard integration and URI parsing behavior.
 */
@RunWith(AndroidJUnit4::class)
class UrlPrivacyAnalyzerInstrumentedTest {

    private lateinit var analyzer: UrlPrivacyAnalyzer
    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager

    @Before
    fun setUp() {
        analyzer = UrlPrivacyAnalyzer()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    @Test
    fun testAndroidUriParsingWithCredentials() {
        val urlWithCredentials = "https://user:password@example.com/path"
        
        assertTrue("Should detect credentials in Android URI parsing", 
            analyzer.hasSensitiveCredentials(urlWithCredentials))
        
        assertEquals("Should classify as sensitive URI", 
            ClipboardContentType.SENSITIVE_URI, 
            analyzer.categorizeContent(urlWithCredentials))
        
        assertEquals("Should require blurred display", 
            UrlPrivacyLevel.BLURRED, 
            analyzer.analyzeUrlPrivacy(urlWithCredentials))
    }

    @Test
    fun testAndroidUriParsingWithSecretParams() {
        val urlWithSecrets = "https://example.com/api?access_token=abc123&api_key=secret456"
        
        assertTrue("Should detect secret parameters in Android URI parsing", 
            analyzer.hasSecretQueryParams(urlWithSecrets))
        
        assertEquals("Should classify as sensitive URI", 
            ClipboardContentType.SENSITIVE_URI, 
            analyzer.categorizeContent(urlWithSecrets))
    }

    @Test
    fun testClipboardIntegrationWithSensitiveContent() {
        val sensitiveUrl = "https://admin:secret@internal.company.com/api?token=xyz789"
        
        // Set clipboard content
        val clipData = ClipData.newPlainText("test", sensitiveUrl)
        clipboardManager.setPrimaryClip(clipData)
        
        // Verify clipboard content is detected as sensitive
        val clipboardContent = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        assertNotNull("Clipboard should contain content", clipboardContent)
        
        assertTrue("Clipboard content should have credentials", 
            analyzer.hasSensitiveCredentials(clipboardContent!!))
        
        assertTrue("Clipboard content should have secret params", 
            analyzer.hasSecretQueryParams(clipboardContent))
    }

    @Test
    fun testPrivacySafePreviewWithAndroidUri() {
        val sensitiveUrl = "https://user:pass@example.com/path?secret=value&normal=param"
        
        val safePreview = analyzer.createPrivacySafePreview(sensitiveUrl, 100)
        
        assertFalse("Safe preview should not contain credentials", 
            safePreview.contains("user:pass"))
        
        assertFalse("Safe preview should not contain secret values", 
            safePreview.contains("secret=value"))
        
        assertTrue("Safe preview should contain masked credentials", 
            safePreview.contains("[credentials]"))
    }

    @Test
    fun testNonUriContentHandling() {
        val nonUriContent = "This is just plain text, not a URL"
        
        // Set non-URI content in clipboard
        val clipData = ClipData.newPlainText("test", nonUriContent)
        clipboardManager.setPrimaryClip(clipData)
        
        assertEquals("Should classify as non-URI", 
            ClipboardContentType.NON_URI, 
            analyzer.categorizeContent(nonUriContent))
        
        val settings = PrivacySettings(hideNonUriContent = true)
        assertFalse("Should not display non-URI content when hidden", 
            analyzer.shouldDisplayContent(nonUriContent, ClipboardContentType.NON_URI, settings))
    }

    @Test
    fun testPrivacySettingsIntegration() {
        val sensitiveUrl = "https://example.com/api?access_token=secret123"
        val contentType = ClipboardContentType.SENSITIVE_URI
        
        // Test with blur enabled
        val blurSettings = PrivacySettings(
            blurSensitiveParams = true,
            blurCredentials = true
        )
        
        assertTrue("Should blur sensitive content", 
            analyzer.shouldBlurContent(sensitiveUrl, contentType, blurSettings))
        
        // Test with blur disabled
        val noBlurSettings = PrivacySettings(
            blurSensitiveParams = false,
            blurCredentials = false
        )
        
        assertFalse("Should not blur when disabled", 
            analyzer.shouldBlurContent(sensitiveUrl, contentType, noBlurSettings))
    }

    @Test
    fun testComplexUriScenarios() {
        // Test international domain names
        val internationalUrl = "https://测试.example.com/path?param=value"
        assertEquals("Should handle international domains", 
            ClipboardContentType.URI, 
            analyzer.categorizeContent(internationalUrl))
        
        // Test URL with port
        val urlWithPort = "https://example.com:8080/api?key=secret"
        assertTrue("Should detect secrets in URLs with ports", 
            analyzer.hasSecretQueryParams(urlWithPort))
        
        // Test URL with fragment
        val urlWithFragment = "https://example.com/page#section?token=abc123"
        // Fragment parameters should not be considered for secrets in this context
        assertFalse("Fragment parameters should not trigger secret detection", 
            analyzer.hasSecretQueryParams(urlWithFragment))
    }

    @Test
    fun testEdgeCasesWithAndroidUri() {
        // Empty clipboard
        clipboardManager.setPrimaryClip(ClipData.newPlainText("empty", ""))
        val emptyContent = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        
        assertEquals("Empty content should be non-URI", 
            ClipboardContentType.NON_URI, 
            analyzer.categorizeContent(emptyContent))
        
        // Malformed URLs
        val malformedUrl = "https://example.com:invalid/path"
        assertEquals("Malformed URL should still be categorized as URI", 
            ClipboardContentType.URI, 
            analyzer.categorizeContent(malformedUrl))
        
        // Very long URL
        val longUrl = "https://example.com/path?" + "param=value&".repeat(100)
        val longPreview = analyzer.createPrivacySafePreview(longUrl, 50)
        assertTrue("Long URL preview should be truncated", 
            longPreview.length <= 53) // 50 + "..."
    }

    @Test
    fun testPerformanceWithLargeContent() {
        // Test with large clipboard content
        val largeContent = "https://example.com/api?" + 
            (1..1000).joinToString("&") { "param$it=value$it" }
        
        val startTime = System.currentTimeMillis()
        val result = analyzer.categorizeContent(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertTrue("Large content analysis should complete quickly", 
            endTime - startTime < 1000) // Should complete within 1 second
        
        assertEquals("Large content should be categorized correctly", 
            ClipboardContentType.URI, result)
    }

    @Test
    fun testSecurityWithMaliciousContent() {
        // Test with potentially malicious patterns
        val maliciousPatterns = listOf(
            "javascript:alert('xss')",
            "data:text/html,<script>alert('xss')</script>",
            "file:///etc/passwd",
            "ftp://user:pass@malicious.com/",
            "https://example.com/path?redirect=javascript:alert(1)"
        )
        
        maliciousPatterns.forEach { pattern ->
            val contentType = analyzer.categorizeContent(pattern)
            
            // Should still categorize correctly without crashing
            assertTrue("Should handle malicious pattern safely: $pattern", 
                contentType in listOf(ClipboardContentType.URI, ClipboardContentType.SENSITIVE_URI, ClipboardContentType.NON_URI))
            
            // Should create safe preview without executing anything
            val safePreview = analyzer.createPrivacySafePreview(pattern)
            assertNotNull("Should create safe preview for malicious content", safePreview)
        }
    }

    @Test
    fun testRealWorldUrlScenarios() {
        val realWorldUrls = mapOf(
            // Social media with tracking
            "https://facebook.com/page?fbclid=abc123&utm_source=share" to ClipboardContentType.URI,
            
            // API with token
            "https://api.github.com/user?access_token=ghp_secret123" to ClipboardContentType.SENSITIVE_URI,
            
            // Email with credentials
            "https://user:password@mail.example.com/inbox" to ClipboardContentType.SENSITIVE_URI,
            
            // Shopping with session
            "https://shop.example.com/cart?sessionid=abc123&PHPSESSID=def456" to ClipboardContentType.SENSITIVE_URI,
            
            // Clean URL
            "https://example.com/article?id=123&lang=en" to ClipboardContentType.URI
        )
        
        realWorldUrls.forEach { (url, expectedType) ->
            val actualType = analyzer.categorizeContent(url)
            assertEquals("URL should be categorized correctly: $url", 
                expectedType, actualType)
        }
    }
}
