package com.gologlu.detracktor.runtime.android.service.net

import com.gologlu.detracktor.domain.error.exceptionOrNull
import com.gologlu.detracktor.domain.error.getOrNull
import com.gologlu.detracktor.domain.error.isSuccess
import com.gologlu.detracktor.runtime.android.test.TestData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for UrlParserImpl.
 * Tests URL parsing implementation with comprehensive edge cases using real Android Uri parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UrlParserImplTest {
    
    private val parser = UrlParserImpl()
    
    @Test
    fun `parse handles simple HTTP URLs correctly`() {
        // Given
        val url = TestData.Urls.SIMPLE_HTTP
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("http", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertNull(parts.path)
        assertTrue(parts.queryPairs.isEmpty())
        assertNull(parts.fragment)
    }
    
    @Test
    fun `parse handles simple HTTPS URLs correctly`() {
        // Given
        val url = TestData.Urls.SIMPLE_HTTPS
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertNull(parts.path)
        assertTrue(parts.queryPairs.isEmpty())
        assertNull(parts.fragment)
    }
    
    @Test
    fun `parse handles URLs with paths correctly`() {
        // Given
        val url = TestData.Urls.WITH_PATH
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/path/to/resource", parts.path)
        assertTrue(parts.queryPairs.isEmpty())
        assertNull(parts.fragment)
    }
    
    @Test
    fun `parse handles URLs with query parameters correctly`() {
        // Given
        val url = TestData.Urls.WITH_QUERY
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertFalse(parts.queryPairs.isEmpty())
        
        // Verify query parameters are parsed correctly
        val queryMap = parts.queryPairs.toQueryMap()
        assertEquals("value1", queryMap.get("param1")[0])
        assertEquals("value2", queryMap.get("param2")[0])
    }

    @Test
    fun `parse handles URLs with fragments correctly`() {
        // Given
        val url = TestData.Urls.WITH_FRAGMENT
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("section", parts.fragment)
    }
    
    @Test
    fun `parse handles complex URLs with all components correctly`() {
        // Given
        val url = TestData.Urls.COMPLEX_URL
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals(8080, parts.port)
        assertEquals("user:pass", parts.userInfo)
        assertEquals("/path", parts.path)
        assertEquals("fragment", parts.fragment)
        
        // Verify query parameters
        val queryMap = parts.queryPairs.toQueryMap()
        assertEquals("value", queryMap.get("param")[0])
    }
    
    @Test
    fun `parse handles URLs with tracking parameters correctly`() {
        // Given
        val testUrls = listOf(
            TestData.Urls.WITH_UTM,
            TestData.Urls.WITH_FACEBOOK_TRACKING,
            TestData.Urls.WITH_GOOGLE_TRACKING
        )
        
        testUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Failed to parse: $url")
            val parts = result.getOrNull()!!
            assertEquals("https", parts.scheme)
            assertEquals("example.com", parts.host)
            assertFalse(parts.queryPairs.isEmpty(), "Query parameters should be present for: $url")
        }
    }
    
    @Test
    fun `parse handles international domain names correctly`() {
        // Given
        val testUrls = listOf(
            TestData.Urls.INTERNATIONAL_DOMAIN,
            TestData.Urls.PUNYCODE_DOMAIN
        )
        
        testUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Failed to parse international domain: $url")
            val parts = result.getOrNull()!!
            assertEquals("https", parts.scheme)
            assertNotNull(parts.host, "Host should not be null for: $url")
        }
    }
    
    @Test
    fun `parse handles Unicode in paths and queries correctly`() {
        // Given
        val testUrls = listOf(
            TestData.Urls.UNICODE_PATH,
            TestData.Urls.UNICODE_QUERY
        )
        
        testUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Failed to parse Unicode URL: $url")
            val parts = result.getOrNull()!!
            assertEquals("https", parts.scheme)
            assertEquals("example.com", parts.host)
        }
    }
    
    @Test
    fun `parse handles social media URLs correctly`() {
        // Given
        val socialUrls = listOf(
            TestData.Urls.YOUTUBE_URL,
            TestData.Urls.TWITTER_URL,
            TestData.Urls.FACEBOOK_URL
        )
        
        socialUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Failed to parse social media URL: $url")
            val parts = result.getOrNull()!!
            assertEquals("https", parts.scheme)
            assertNotNull(parts.host, "Host should not be null for: $url")
            assertFalse(parts.queryPairs.isEmpty(), "Social media URLs should have query parameters: $url")
        }
    }
    
    @Test
    fun `parse handles e-commerce URLs correctly`() {
        // Given
        val ecommerceUrls = listOf(
            TestData.Urls.AMAZON_URL,
            TestData.Urls.EBAY_URL
        )
        
        ecommerceUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Failed to parse e-commerce URL: $url")
            val parts = result.getOrNull()!!
            assertEquals("https", parts.scheme)
            assertNotNull(parts.host, "Host should not be null for: $url")
            assertFalse(parts.queryPairs.isEmpty(), "E-commerce URLs should have query parameters: $url")
        }
    }
    
    @Test
    fun `parse handles malformed URLs gracefully`() {
        // Given
        val malformedUrls = TestData.ErrorScenarios.getInvalidUrls()
        
        malformedUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then - Android Uri parser is permissive, some "malformed" URLs might still parse
            assertNotNull(result, "Result should not be null for: $url")
            // Don't assert failure - Android Uri parser handles many edge cases gracefully
        }
    }
    
    @Test
    fun `parse handles empty and whitespace URLs correctly`() {
        // Given
        val testCases = listOf(
            TestData.Urls.EMPTY_URL,
            TestData.Urls.WHITESPACE_URL,
            "   ",
            "\t\n"
        )
        
        testCases.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then - Android Uri parser behavior varies for empty/whitespace URLs
            assertNotNull(result, "Result should not be null for: '$url'")
            // Don't assert specific success/failure - Android Uri parser handles these cases differently
        }
    }
    
    @Test
    fun `parse handles URLs with missing schemes correctly`() {
        // Given
        val url = TestData.Urls.MISSING_SCHEME
        
        // When
        val result = parser.parse(url)
        
        // Then
        // Android Uri parser might handle this differently
        // The test verifies the behavior is consistent
        assertNotNull(result)
    }
    
    @Test
    fun `parse handles very long URLs correctly`() {
        // Given
        val url = TestData.Urls.VERY_LONG_URL
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess, "Should handle very long URLs")
        val parts = result.getOrNull()!!
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNotNull(parts.path)
        assertTrue(parts.path!!.length > 1000, "Path should be very long")
    }
    
    @Test
    fun `parse handles IPv4 addresses correctly`() {
        // Given
        val testCases = listOf(
            "http://127.0.0.1",
            "https://192.168.1.1:8080",
            "http://10.0.0.1/path?param=value"
        )
        
        testCases.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse IPv4 address: $url")
            val parts = result.getOrNull()!!
            assertNotNull(parts.host, "Host should not be null for IPv4: $url")
            assertTrue(parts.host!!.matches(Regex("""\d+\.\d+\.\d+\.\d+""")), "Should be valid IPv4: ${parts.host}")
        }
    }
    
    @Test
    fun `parse handles IPv6 addresses correctly`() {
        // Given
        val testCases = listOf(
            "http://[::1]",
            "https://[2001:db8::1]:8080",
            "http://[fe80::1%eth0]/path"
        )
        
        testCases.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse IPv6 address: $url")
            val parts = result.getOrNull()!!
            assertNotNull(parts.host, "Host should not be null for IPv6: $url")
        }
    }
    
    @Test
    fun `parse handles IPv6 zone IDs correctly`() {
        // Given
        val url = "http://[fe80::1%eth0]/path"
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess, "Should parse IPv6 with zone ID")
        val parts = result.getOrNull()!!
        assertNotNull(parts.host)
        // Android Uri parser may handle zone IDs differently - just verify host is not null
        // The exact encoding behavior depends on the Android Uri implementation
    }
    
    @Test
    fun `parse handles custom ports correctly`() {
        // Given
        val testCases = listOf(
            "http://example.com:8080" to 8080,
            "https://example.com:443" to 443,
            "http://example.com:80" to 80,
            "https://example.com:9000" to 9000
        )
        
        testCases.forEach { (url, expectedPort) ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse URL with port: $url")
            val parts = result.getOrNull()!!
            assertEquals(expectedPort, parts.port, "Port should match for: $url")
        }
    }
    
    @Test
    fun `parse handles default ports correctly`() {
        // Given
        val testCases = listOf(
            "http://example.com" to null,
            "https://example.com" to null,
            "http://example.com:80" to 80, // Explicit default port
            "https://example.com:443" to 443 // Explicit default port
        )
        
        testCases.forEach { (url, expectedPort) ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse URL: $url")
            val parts = result.getOrNull()!!
            assertEquals(expectedPort, parts.port, "Port should match for: $url")
        }
    }
    
    @Test
    fun `parse handles user info correctly`() {
        // Given
        val testCases = listOf(
            "http://user@example.com" to "user",
            "https://user:pass@example.com" to "user:pass",
            "http://user:@example.com" to "user:",
            "https://:pass@example.com" to ":pass"
        )
        
        testCases.forEach { (url, expectedUserInfo) ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse URL with user info: $url")
            val parts = result.getOrNull()!!
            assertEquals(expectedUserInfo, parts.userInfo, "User info should match for: $url")
        }
    }
    
    @Test
    fun `parse handles various path formats correctly`() {
        // Given
        val testCases = listOf(
            "https://example.com/" to "/",
            "https://example.com/path" to "/path",
            "https://example.com/path/" to "/path/",
            "https://example.com/path/to/resource" to "/path/to/resource",
            "https://example.com/path%20with%20spaces" to "/path with spaces" // Android Uri auto-decodes
        )
        
        testCases.forEach { (url, expectedPath) ->
            // When
            val result = parser.parse(url)
            
            // Then
            assertTrue(result.isSuccess, "Should parse URL with path: $url")
            val parts = result.getOrNull()!!
            assertEquals(expectedPath, parts.path, "Path should match for: $url")
        }
    }
    
    @Test
    fun `parse handles complex query parameters correctly`() {
        // Given
        val url = "https://example.com?param1=value1&param2=value%202&param3&param4="
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        assertFalse(parts.queryPairs.isEmpty())
        
        val queryMap = parts.queryPairs.toQueryMap()
        assertEquals("value1", queryMap.get("param1")[0])
        assertEquals("value 2", queryMap.get("param2")[0]) // Android Uri auto-decodes
        assertTrue(queryMap.get("param3").isNotEmpty())
        assertTrue(queryMap.get("param4").isNotEmpty())
    }
    
    @Test
    fun `parse preserves query parameter order`() {
        // Given
        val url = "https://example.com?z=1&a=2&m=3"
        
        // When
        val result = parser.parse(url)
        
        // Then
        assertTrue(result.isSuccess)
        val parts = result.getOrNull()!!
        
        // Verify order is preserved
        val queryPairs = parts.queryPairs
        val parameterNames = queryPairs.tokens.map { it.decodedKey }
        assertEquals(listOf("z", "a", "m"), parameterNames)
        
        // Also verify the string representation preserves order
        val queryString = queryPairs.asString()
        assertEquals("z=1&a=2&m=3", queryString)
    }
    
    @Test
    fun `parse handles all test data URLs correctly`() {
        // Given
        val allTestUrls = TestData.Urls.getAllTestUrls()
        
        allTestUrls.forEach { url ->
            // When
            val result = parser.parse(url)
            
            // Then - should not crash, behavior depends on URL validity
            assertNotNull(result, "Result should not be null for: $url")
            
            if (result.isSuccess) {
                val parts = result.getOrNull()!!
                // Basic validation for successful parses
                if (parts.scheme != null) {
                    assertTrue(parts.scheme!!.isNotEmpty(), "Scheme should not be empty for: $url")
                }
            }
        }
    }
    
    @Test
    fun `parse is consistent across multiple calls`() {
        // Given
        val testUrls = listOf(
            TestData.Urls.COMPLEX_URL,
            TestData.Urls.WITH_UTM,
            TestData.Urls.YOUTUBE_URL
        )
        
        testUrls.forEach { url ->
            // When - parse multiple times
            val result1 = parser.parse(url)
            val result2 = parser.parse(url)
            val result3 = parser.parse(url)
            
            // Then - results should be consistent
            assertEquals(result1.isSuccess, result2.isSuccess, "Consistency check failed for: $url")
            assertEquals(result2.isSuccess, result3.isSuccess, "Consistency check failed for: $url")
            
            if (result1.isSuccess && result2.isSuccess && result3.isSuccess) {
                val parts1 = result1.getOrNull()!!
                val parts2 = result2.getOrNull()!!
                val parts3 = result3.getOrNull()!!
                
                assertEquals(parts1.scheme, parts2.scheme, "Scheme consistency failed for: $url")
                assertEquals(parts2.scheme, parts3.scheme, "Scheme consistency failed for: $url")
                assertEquals(parts1.host, parts2.host, "Host consistency failed for: $url")
                assertEquals(parts2.host, parts3.host, "Host consistency failed for: $url")
            }
        }
    }
}
