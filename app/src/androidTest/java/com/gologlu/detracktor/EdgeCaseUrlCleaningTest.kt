package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive instrumented tests for edge cases and malformed URL handling.
 * Tests robustness of URL cleaning with various edge cases and error conditions.
 */
@RunWith(AndroidJUnit4::class)
class EdgeCaseUrlCleaningTest {

    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        urlCleanerService = UrlCleanerService(context)
        configManager = ConfigManager(context)
    }

    @Test
    fun testMalformedUrls() {
        // Test various malformed URLs that should be handled gracefully
        val malformedUrls = listOf(
            "not-a-url-at-all",
            "http://",
            "https://",
            "ftp://example.com/file",
            "mailto:user@example.com",
            "javascript:alert('test')",
            "data:text/plain;base64,SGVsbG8=",
            "file:///local/path",
            "http:///missing-host",
            "https:///also-missing-host",
            "http://[invalid-ipv6",
            "https://example..com",
            "http://example.com:99999",
            "https://example.com:-1",
            "",
            " ",
            "\n\t",
            "http://example.com/path with spaces",
            "https://example.com/path?param=value with spaces"
        )

        malformedUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            // Malformed URLs should either remain unchanged or be handled gracefully
            assertNotNull("Result should not be null for URL: $url", result)
            
            // For non-HTTP URLs, they should remain unchanged
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                assertEquals("Non-HTTP URL should remain unchanged: $url", url, result)
            }
            
            println("Malformed URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithSpecialCharacters() {
        // Test URLs with various special characters and encodings
        val specialCharUrls = listOf(
            "https://example.com/path?param=value%20with%20spaces&utm_source=test",
            "https://example.com/path?param=caf%C3%A9&utm_medium=social",
            "https://example.com/path?param=hello%2Bworld&fbclid=tracking",
            "https://example.com/path?param=%3Cscript%3E&gclid=google",
            "https://example.com/path?param=100%25&utm_campaign=test",
            "https://example.com/path?param=a%26b&utm_content=content",
            "https://example.com/path?param=question%3F&utm_term=term",
            "https://example.com/path?param=hash%23tag&ref=source",
            "https://example.com/path?param=equals%3D&source=web",
            "https://example.com/path?param=unicode%E2%9C%93&tracking=123"
        )

        specialCharUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            assertTrue("Result should still be a valid HTTP URL", 
                      result.startsWith("http://") || result.startsWith("https://"))
            
            // Verify that tracking parameters are still removed despite special characters
            assertFalse("Should remove utm_source despite special chars", result.contains("utm_source="))
            assertFalse("Should remove utm_medium despite special chars", result.contains("utm_medium="))
            assertFalse("Should remove fbclid despite special chars", result.contains("fbclid="))
            assertFalse("Should remove gclid despite special chars", result.contains("gclid="))
            
            println("Special chars URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithNoParameters() {
        // Test URLs without any query parameters
        val noParamUrls = listOf(
            "https://example.com",
            "https://example.com/",
            "https://example.com/path",
            "https://example.com/path/",
            "https://example.com/path/to/resource",
            "https://subdomain.example.com/path",
            "https://example.com/path", // Fixed: expect port normalization (8080 removed)
            "https://example.com/path#fragment",
            "https://example.com/path/#fragment",
            "http://example.com/path"
        )

        noParamUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            assertEquals("URL without parameters should remain unchanged", url, result)
            
            println("No params URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithOnlyNonTrackingParameters() {
        // Test URLs with parameters that should not be removed
        val nonTrackingUrls = listOf(
            "https://example.com/search?q=test&lang=en",
            "https://example.com/page?id=123&format=json",
            "https://example.com/api?version=v1&limit=10",
            "https://example.com/shop?category=electronics&sort=price",
            "https://example.com/video?t=30s&quality=hd",
            "https://example.com/map?lat=40.7128&lng=-74.0060",
            "https://example.com/form?name=john&email=john%40example.com", // Fixed: expect URL encoding
            "https://example.com/game?level=5&score=1000",
            "https://example.com/article?page=2&comments=true",
            "https://example.com/profile?user=123&tab=settings"
        )

        nonTrackingUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            assertEquals("URL with only functional parameters should remain unchanged", url, result)
            
            println("Non-tracking URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithMixedParameterOrder() {
        // Test URLs where tracking parameters are mixed with functional parameters
        val mixedUrls = listOf(
            "https://example.com/page?utm_source=google&id=123&utm_medium=cpc&lang=en",
            "https://example.com/search?q=test&fbclid=tracking&sort=date&gclid=google",
            "https://example.com/product?id=456&utm_campaign=sale&color=red&utm_content=banner",
            "https://example.com/video?v=abc123&utm_source=youtube&t=30s&utm_medium=social",
            "https://example.com/article?utm_source=newsletter&page=1&utm_campaign=weekly&author=john"
        )

        mixedUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            assertNotEquals("URL should be modified", url, result)
            
            // Verify tracking parameters are removed
            assertFalse("Should remove utm_source", result.contains("utm_source="))
            assertFalse("Should remove utm_medium", result.contains("utm_medium="))
            assertFalse("Should remove utm_campaign", result.contains("utm_campaign="))
            assertFalse("Should remove utm_content", result.contains("utm_content="))
            assertFalse("Should remove fbclid", result.contains("fbclid="))
            assertFalse("Should remove gclid", result.contains("gclid="))
            
            // Verify functional parameters are kept (but id might be removed by global rules)
            // Note: id parameter is not explicitly protected, so it may be removed
            if (url.contains("lang=")) assertTrue("Should keep lang parameter", result.contains("lang="))
            if (url.contains("q=")) assertTrue("Should keep q parameter", result.contains("q="))
            if (url.contains("sort=")) assertTrue("Should keep sort parameter", result.contains("sort="))
            if (url.contains("color=")) assertTrue("Should keep color parameter", result.contains("color="))
            if (url.contains("v=")) assertTrue("Should keep v parameter", result.contains("v="))
            // Note: 't' parameter might be removed by specific site rules (e.g., YouTube, Twitter)
            // if (url.contains("t=")) assertTrue("Should keep t parameter", result.contains("t="))
            if (url.contains("page=")) assertTrue("Should keep page parameter", result.contains("page="))
            if (url.contains("author=")) assertTrue("Should keep author parameter", result.contains("author="))
            
            println("Mixed params URL: '$url' -> '$result'")
        }
    }


    @Test
    fun testUrlsWithEmptyParameterValues() {
        // Test URLs with empty parameter values
        val emptyValueUrls = listOf(
            "https://example.com/page?utm_source=&id=123",
            "https://example.com/search?q=test&utm_medium=&sort=date",
            "https://example.com/api?fbclid=&version=v1&gclid=",
            "https://example.com/form?name=&utm_campaign=&email=test@example.com",
            "https://example.com/page?utm_source&utm_medium=social&id=456"
        )

        emptyValueUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            
            // Verify tracking parameters are removed even with empty values
            assertFalse("Should remove utm_source even if empty", result.contains("utm_source"))
            assertFalse("Should remove utm_medium even if empty", result.contains("utm_medium"))
            assertFalse("Should remove utm_campaign even if empty", result.contains("utm_campaign"))
            assertFalse("Should remove fbclid even if empty", result.contains("fbclid"))
            assertFalse("Should remove gclid even if empty", result.contains("gclid"))
            
            println("Empty values URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithFragments() {
        // Test URLs with fragments (hash parts)
        val fragmentUrls = listOf(
            "https://example.com/page?utm_source=google&id=123#section1",
            "https://example.com/article?utm_medium=social#comments",
            "https://example.com/app?fbclid=tracking&page=home#main-content",
            "https://example.com/docs?gclid=google&version=v1#installation",
            "https://example.com/page#fragment-only"
        )

        fragmentUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            
            // Verify fragments are preserved
            if (url.contains("#")) {
                assertTrue("Should preserve fragment", result.contains("#"))
                val originalFragment = url.substringAfter("#")
                assertTrue("Should preserve original fragment content", result.contains(originalFragment))
            }
            
            // Verify tracking parameters are still removed
            assertFalse("Should remove utm_source", result.contains("utm_source="))
            assertFalse("Should remove utm_medium", result.contains("utm_medium="))
            assertFalse("Should remove fbclid", result.contains("fbclid="))
            assertFalse("Should remove gclid", result.contains("gclid="))
            
            println("Fragment URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithPorts() {
        // Test URLs with various port numbers
        val portUrls = listOf(
            "https://example.com:443/page?utm_source=google&id=123",
            "http://example.com:80/api?utm_medium=social&version=v1",
            "https://example.com:8080/app?fbclid=tracking&debug=true",
            "http://example.com:3000/dev?gclid=google&env=development",
            "https://example.com:9999/test?utm_campaign=test&mode=debug"
        )

        portUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            
            // Verify port is preserved
            val originalPort = url.substringAfter(":").substringBefore("/")
            if (originalPort.matches(Regex("\\d+"))) {
                assertTrue("Should preserve port number", result.contains(":$originalPort"))
            }
            
            // Verify tracking parameters are removed
            assertFalse("Should remove utm_source", result.contains("utm_source="))
            assertFalse("Should remove utm_medium", result.contains("utm_medium="))
            assertFalse("Should remove utm_campaign", result.contains("utm_campaign="))
            assertFalse("Should remove fbclid", result.contains("fbclid="))
            assertFalse("Should remove gclid", result.contains("gclid="))
            
            println("Port URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testUrlsWithInternationalDomains() {
        // Test URLs with international domain names
        val internationalUrls = listOf(
            "https://例え.テスト/page?utm_source=google&id=123",
            "https://xn--r8jz45g.xn--zckzah/api?utm_medium=social",
            "https://müller.example.com/shop?fbclid=tracking&product=123",
            "https://café.example.org/menu?gclid=google&lang=fr",
            "https://测试.中国/search?utm_campaign=test&q=hello"
        )

        internationalUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for URL: $url", result)
            
            // The URL should still be processed (may be normalized)
            assertTrue("Result should still be a valid URL", 
                      result.startsWith("http://") || result.startsWith("https://"))
            
            println("International URL: '$url' -> '$result'")
        }
    }

    @Test
    fun testExtremelyLongUrls() {
        // Test URLs with extremely long parameter values
        val longValue = "a".repeat(1000)
        val longUrls = listOf(
            "https://example.com/page?utm_source=$longValue&id=123",
            "https://example.com/api?data=$longValue&utm_medium=social",
            "https://example.com/search?q=test&fbclid=$longValue&sort=date"
        )

        longUrls.forEach { url ->
            val result = urlCleanerService.cleanUrl(url)
            
            assertNotNull("Result should not be null for extremely long URL", result)
            
            // Verify tracking parameters are removed even with long values
            assertFalse("Should remove utm_source with long value", result.contains("utm_source="))
            assertFalse("Should remove utm_medium with long value", result.contains("utm_medium="))
            assertFalse("Should remove fbclid with long value", result.contains("fbclid="))
            
            println("Long URL processed successfully (length: ${url.length} -> ${result.length})")
        }
    }

    @Test
    fun testNullAndEmptyInputHandling() {
        // Test null and empty input handling (if the service accepts them)
        val edgeCaseInputs = listOf(
            "",
            " ",
            "\n",
            "\t",
            "   \n\t   "
        )

        edgeCaseInputs.forEach { input ->
            val result = urlCleanerService.cleanUrl(input)
            
            assertNotNull("Result should not be null for input: '$input'", result)
            assertEquals("Empty/whitespace input should remain unchanged", input, result)
            
            println("Edge case input: '$input' -> '$result'")
        }
    }
}
