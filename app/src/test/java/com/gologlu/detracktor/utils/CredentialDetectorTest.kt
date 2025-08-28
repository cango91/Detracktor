package com.gologlu.detracktor.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for the CredentialDetector.
 * Focuses on embedded credential detection for user safety.
 */
class CredentialDetectorTest {
    
    private val credentialDetector = CredentialDetector()
    
    @Test
    fun testHasEmbeddedCredentials_basicAuth() {
        val url = "https://user:password@example.com/page"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_apiKey() {
        val url = "https://api.example.com/data?api_key=secret123"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_token() {
        val url = "https://api.example.com/data?access_token=abc123def456"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_password() {
        val url = "https://example.com/login?password=mypassword"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_secret() {
        val url = "https://api.example.com/data?client_secret=verysecret"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_noCredentials() {
        val url = "https://example.com/page?param=value"
        assertFalse(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_falsePositive_test() {
        val url = "https://api.example.com/data?api_key=test"
        assertFalse(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_falsePositive_demo() {
        val url = "https://api.example.com/data?token=demo"
        assertFalse(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_falsePositive_short() {
        val url = "https://api.example.com/data?key=abc"
        assertFalse(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_falsePositive_placeholder() {
        val url = "https://api.example.com/data?api_key=your_key"
        assertFalse(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testExtractCredentials_basicAuth() {
        val url = "https://user:password@example.com/page"
        val credentials = credentialDetector.extractCredentials(url)
        
        assertNotNull(credentials)
        assertEquals("user", credentials!!.first)
        assertEquals("password", credentials.second)
    }
    
    @Test
    fun testExtractCredentials_basicAuth_falsePositive() {
        val url = "https://user:test@example.com/page"
        val credentials = credentialDetector.extractCredentials(url)
        
        assertNull(credentials) // Should be null due to false positive password
    }
    
    @Test
    fun testExtractCredentials_noCredentials() {
        val url = "https://example.com/page"
        val credentials = credentialDetector.extractCredentials(url)
        
        assertNull(credentials)
    }
    
    @Test
    fun testExtractCredentials_emptyPassword() {
        val url = "https://user:@example.com/page"
        val credentials = credentialDetector.extractCredentials(url)
        
        assertNull(credentials)
    }
    
    @Test
    fun testExtractCredentials_emptyUsername() {
        val url = "https://:password@example.com/page"
        val credentials = credentialDetector.extractCredentials(url)
        
        assertNull(credentials)
    }
    
    @Test
    fun testHasEmbeddedCredentials_caseInsensitive() {
        val url = "https://api.example.com/data?API_KEY=secret123"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_underscoreVariations() {
        val urls = listOf(
            "https://api.example.com/data?api_key=secret123",
            "https://api.example.com/data?apikey=secret123",
            "https://api.example.com/data?access_token=secret123",
            "https://api.example.com/data?auth_token=secret123"
        )
        
        urls.forEach { url ->
            assertTrue("URL should contain credentials: $url", credentialDetector.hasEmbeddedCredentials(url))
        }
    }
    
    @Test
    fun testHasEmbeddedCredentials_multipleParameters() {
        val url = "https://api.example.com/data?param1=value1&api_key=secret123&param2=value2"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
    
    @Test
    fun testHasEmbeddedCredentials_complexUrl() {
        val url = "https://user:pass@api.example.com:8080/v1/data?api_key=secret&other=value#fragment"
        assertTrue(credentialDetector.hasEmbeddedCredentials(url))
    }
}
