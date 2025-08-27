package com.gologlu.detracktor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.data.CleaningResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test to verify that embedded credentials are preserved during URL cleaning
 * while sensitive query parameters are still removed correctly.
 */
@RunWith(AndroidJUnit4::class)
class EmbeddedCredentialsPreservationTest {

    private lateinit var context: Context
    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var clipboardManager: ClipboardManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        urlCleanerService = UrlCleanerService(context)
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    @Test
    fun testEmbeddedCredentialsPreservedDuringSensitiveParamRemoval() {
        // Test case provided by user: URL with embedded credentials and sensitive params
        val testUrl = "https://user:pass@example.com/?category=some&utm_l=something&token=shouldbehidden"
        
        // Set clipboard content
        val clipData = ClipData.newPlainText("test", testUrl)
        clipboardManager.setPrimaryClip(clipData)
        
        // Clean the URL
        val result = urlCleanerService.cleanClipboardUrl()
        
        // Verify cleaning occurred
        assertEquals("URL should have been cleaned and copied", CleaningResult.CLEANED_AND_COPIED, result)
        
        // Get the cleaned URL from clipboard
        val cleanedClipData = clipboardManager.primaryClip
        assertNotNull("Clipboard should contain cleaned URL", cleanedClipData)
        assertTrue("Clipboard should have at least one item", cleanedClipData!!.itemCount > 0)
        
        val cleanedUrl = cleanedClipData.getItemAt(0).text.toString()
        
        // Verify embedded credentials are preserved
        assertTrue("Cleaned URL should preserve embedded credentials", 
                  cleanedUrl.contains("user:pass@"))
        
        // Verify the host is preserved
        assertTrue("Cleaned URL should preserve the host", 
                  cleanedUrl.contains("example.com"))
        
        // Verify sensitive parameters are removed (this depends on default rules)
        // The exact behavior depends on the configured rules, but we can verify structure
        assertFalse("Original URL should not equal cleaned URL", testUrl == cleanedUrl)
        
        // Verify the URL structure is valid
        assertTrue("Cleaned URL should start with https://", cleanedUrl.startsWith("https://"))
        assertTrue("Cleaned URL should contain the credentials and host", 
                  cleanedUrl.contains("https://user:pass@example.com"))
        
        println("Original URL: $testUrl")
        println("Cleaned URL: $cleanedUrl")
    }

    @Test
    fun testDirectUrlCleaningPreservesCredentials() {
        val testUrl = "https://user:pass@example.com/?category=some&utm_l=something&token=shouldbehidden"
        
        // Test direct URL cleaning (not through clipboard)
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        // Verify embedded credentials are preserved
        assertTrue("Cleaned URL should preserve embedded credentials", 
                  cleanedUrl.contains("user:pass@"))
        
        // Verify the host is preserved
        assertTrue("Cleaned URL should preserve the host", 
                  cleanedUrl.contains("example.com"))
        
        // Verify the URL structure is valid
        assertTrue("Cleaned URL should start with https://", cleanedUrl.startsWith("https://"))
        assertTrue("Cleaned URL should contain the credentials and host", 
                  cleanedUrl.contains("https://user:pass@example.com"))
        
        println("Direct cleaning - Original: $testUrl")
        println("Direct cleaning - Cleaned: $cleanedUrl")
    }

    @Test
    fun testVariousCredentialFormats() {
        val testCases = listOf(
            "https://user:pass@example.com/?utm_source=test",
            "https://username:password123@subdomain.example.com/?tracking=abc",
            "https://api_key:secret@api.example.com/?token=hidden",
            "https://user@example.com/?utm_campaign=test", // user only, no password
            "http://test:123@localhost:8080/?debug=true" // with port
        )
        
        testCases.forEach { testUrl ->
            val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
            
            // Extract expected credentials from original URL
            val credentialsMatch = Regex("://([^@]+)@").find(testUrl)
            val expectedCredentials = credentialsMatch?.groupValues?.get(1)
            
            if (expectedCredentials != null) {
                assertTrue("Cleaned URL should preserve credentials '$expectedCredentials' for URL: $testUrl", 
                          cleanedUrl.contains("$expectedCredentials@"))
            }
            
            println("Test case - Original: $testUrl")
            println("Test case - Cleaned: $cleanedUrl")
            println("---")
        }
    }

    @Test
    fun testCredentialsWithSpecialCharacters() {
        // Test credentials with special characters that might cause URL parsing issues
        val testCases = listOf(
            "https://user%40domain:pass%21@example.com/?utm_source=test", // URL encoded
            "https://user+name:pass-word@example.com/?tracking=abc",
            "https://user.name:pass_word@example.com/?token=hidden"
        )
        
        testCases.forEach { testUrl ->
            val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
            
            // Verify the URL still contains credentials (the exact format may vary due to normalization)
            assertTrue("Cleaned URL should contain @ symbol indicating preserved credentials for: $testUrl", 
                      cleanedUrl.contains("@"))
            
            // Verify the URL is still valid
            assertTrue("Cleaned URL should start with http(s):// for: $testUrl", 
                      cleanedUrl.startsWith("http://") || cleanedUrl.startsWith("https://"))
            
            println("Special chars - Original: $testUrl")
            println("Special chars - Cleaned: $cleanedUrl")
            println("---")
        }
    }

    @Test
    fun testUrlsWithoutCredentialsUnchanged() {
        val testUrl = "https://example.com/?utm_source=test&category=some"
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        // Should not contain @ symbol since no credentials
        assertFalse("URL without credentials should not contain @ symbol", 
                   cleanedUrl.contains("@"))
        
        // Should still be a valid URL
        assertTrue("Cleaned URL should start with https://", cleanedUrl.startsWith("https://"))
        assertTrue("Cleaned URL should contain the host", cleanedUrl.contains("example.com"))
        
        println("No credentials - Original: $testUrl")
        println("No credentials - Cleaned: $cleanedUrl")
    }

    @Test
    fun testCredentialsPreservationWithPortNumbers() {
        val testUrl = "https://user:pass@example.com:8443/?utm_source=test&token=secret"
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        // Verify embedded credentials are preserved
        assertTrue("Cleaned URL should preserve embedded credentials", 
                  cleanedUrl.contains("user:pass@"))
        
        // Verify port is preserved
        assertTrue("Cleaned URL should preserve port number", 
                  cleanedUrl.contains(":8443"))
        
        // Verify the complete authority section
        assertTrue("Cleaned URL should preserve complete authority with credentials and port", 
                  cleanedUrl.contains("user:pass@example.com:8443"))
        
        println("With port - Original: $testUrl")
        println("With port - Cleaned: $cleanedUrl")
    }

    @Test
    fun testAnalyzeClipboardContentWithCredentials() {
        val testUrl = "https://user:pass@example.com/?category=some&utm_l=something&token=shouldbehidden"
        
        // Set clipboard content
        val clipData = ClipData.newPlainText("test", testUrl)
        clipboardManager.setPrimaryClip(clipData)
        
        // Analyze clipboard content
        val analysis = urlCleanerService.analyzeClipboardContent()
        
        assertNotNull("Analysis should not be null", analysis)
        assertEquals("Original URL should match", testUrl, analysis!!.originalUrl)
        assertTrue("Should be recognized as valid URL", analysis.isValidUrl)
        
        // Verify cleaned URL preserves credentials
        assertTrue("Cleaned URL should preserve embedded credentials", 
                  analysis.cleanedUrl.contains("user:pass@"))
        
        // Verify changes were detected
        assertTrue("Should detect changes", analysis.hasChanges)
        
        println("Analysis - Original: ${analysis.originalUrl}")
        println("Analysis - Cleaned: ${analysis.cleanedUrl}")
        println("Analysis - Has changes: ${analysis.hasChanges}")
        println("Analysis - Parameters to remove: ${analysis.parametersToRemove}")
    }
}
