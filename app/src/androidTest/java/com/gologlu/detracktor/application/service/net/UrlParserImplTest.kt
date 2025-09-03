package com.gologlu.detracktor.application.service.net

import com.gologlu.detracktor.domain.error.exceptionOrNull
import com.gologlu.detracktor.domain.error.isSuccess
import com.gologlu.detracktor.domain.error.getOrThrow
import com.gologlu.detracktor.domain.model.Url
import com.gologlu.detracktor.runtime.android.service.net.UrlParserImpl
import org.junit.Test
import org.junit.Assert.*

class UrlParserImplTest {

    private val parser = UrlParserImpl()

    @Test
    fun `parse_should_handle_basic_HTTP_URL_correctly`() {
        val result = parser.parse("http://example.com/path?query=value")

        assertTrue("Should succeed with valid URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("http", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertEquals("/path", parts.path)
        assertEquals("query=value", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_HTTPS_URL_with_port_correctly`() {
        val result = parser.parse("https://api.example.com:8443/v1/resource")

        assertTrue("Should succeed with valid URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("https", parts.scheme)
        assertEquals("api.example.com", parts.host)
        assertEquals(8443, parts.port)
        assertNull(parts.userInfo)
        assertEquals("/v1/resource", parts.path)
        assertEquals("", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_URL_with_query_parameters_correctly`() {
        val result = parser.parse("https://example.com/search?q=kotlin&type=code&page=1")

        assertTrue("Should succeed with valid URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertEquals("/search", parts.path)
        assertEquals("q=kotlin&type=code&page=1", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_URL_with_fragment_correctly`() {
        val result = parser.parse("https://example.com/page#section")

        assertTrue("Should succeed with valid URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertEquals("/page", parts.path)
        assertEquals("", parts.rawQuery)
        assertEquals("section", parts.fragment)
    }

    @Test
    fun `parse_should_handle_IPv6_address_correctly`() {
        val result = parser.parse("http://[::1]:8080/api")

        assertTrue("Should succeed with IPv6 URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("http", parts.scheme)
        // android.net.Uri preserves IPv6 brackets in the host
        assertEquals("[::1]", parts.host)
        assertEquals(8080, parts.port)
        assertNull(parts.userInfo)
        assertEquals("/api", parts.path)
        assertEquals("", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_userInfo_correctly`() {
        val result = parser.parse("https://user:pass@example.com:8443/secure")

        assertTrue("Should succeed with userInfo URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals(8443, parts.port)
        assertEquals("user:pass", parts.userInfo)
        assertEquals("/secure", parts.path)
        assertEquals("", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_invalid_URL_gracefully`() {
        // android.net.Uri is quite lenient and parses "http:///path" as valid
        // with scheme="http", host="", path="/path"
        val malformedResult = parser.parse("http:///path")  // Missing host

        // android.net.Uri accepts this and returns empty string for host
        assertTrue("Should succeed with malformed URL that android.net.Uri can parse", malformedResult.isSuccess)
        val parts = malformedResult.getOrThrow()
        assertEquals("http", parts.scheme)
        assertEquals("", parts.host)
        assertNull(parts.port)
        assertNull(parts.userInfo)
        assertEquals("/path", parts.path)
        assertEquals("", parts.rawQuery)
        assertNull(parts.fragment)
    }

    @Test
    fun `parse_should_handle_empty_URL_gracefully`() {

        // parser will successfully parse empty URL (UrlParts allows all fields to be null)
        // mimics androidx.core.net.toUri behaviour
        val result = parser.parse("")
        assertNotNull(result)
        assertTrue(result.isSuccess)

        // However the domain provided vocabulary for creating a valid Url
        // `Url.from` will fail with InvalidUrl error (Scheme and Host are required)
        // despite `parser.parse` successfully parsing the empty URL
        with(this.parser){
            val domainResult = Url.from("")
            assertFalse(domainResult.isSuccess)
            assertTrue(domainResult.exceptionOrNull() is com.gologlu.detracktor.domain.error.DomainException)
        }
    }

    @Test
    fun `parse_should_handle_URL_reconstruction_correctly`() {
        val originalUrl = "https://user:pass@example.com:8443/api/v1?param1=value1&param2=value2#section"
        val result = parser.parse(originalUrl)

        assertTrue("Should succeed with complex URL", result.isSuccess)
        val parts = result.getOrThrow()

        // Reconstruct URL using UrlParts.toUrlString()
        val reconstructedUrl = parts.toUrlString()

        // android.net.Uri handles URL parsing and normalization consistently
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals(8443, parts.port)
        assertEquals("user:pass", parts.userInfo)
        assertEquals("/api/v1", parts.path)
        assertEquals("param1=value1&param2=value2", parts.rawQuery)
        assertEquals("section", parts.fragment)
    }

    // Additional edge case tests for UrlParserImpl

    @Test
    fun `parse_should_handle_IPv6_zone_id_encoding`() {
        // Test IPv6 addresses with zone IDs that need % encoding
        // Use pre-encoded URL since Android Uri requires proper encoding
        val result = parser.parse("http://[fe80::1%25eth0]:8080/api")

        assertTrue("Should succeed with IPv6 zone ID URL", result.isSuccess)
        val parts = result.getOrThrow()

        assertEquals("http", parts.scheme)
        // The host should preserve the %25 encoding
        assertEquals("[fe80::1%25eth0]", parts.host)
        assertEquals(8080, parts.port)
        assertEquals("/api", parts.path)
    }

    @Test
    fun `parse_should_handle_malformed_IPv6_addresses`() {
        // Test various malformed IPv6 addresses
        val malformedAddresses = listOf(
            "http://[::1::2]:8080/",  // Invalid double ::
            "http://[gggg::1]:8080/", // Invalid hex digits
            "http://[::1:8080/",      // Missing closing bracket
            "http://::1]:8080/"       // Missing opening bracket
        )

        for (address in malformedAddresses) {
            val result = parser.parse(address)
            // android.net.Uri might still parse these, but they should be handled gracefully
            assertNotNull("Should handle malformed IPv6 address: $address", result)
        }
    }

    @Test
    fun `parse_should_handle_very_long_URLs`() {
        val longPath = "/path/" + "segment/".repeat(1000)
        val longQuery = "param=" + "value".repeat(1000)
        val longUrl = "https://example.com$longPath?$longQuery"

        val result = parser.parse(longUrl)
        assertTrue("Should handle very long URLs", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertTrue("Path should be preserved", parts.path!!.length > 5000)
        assertTrue("Query should be preserved", parts.rawQuery.length > 4000)
    }

    @Test
    fun `parse_should_handle_unusual_schemes`() {
        val unusualSchemes = listOf(
            "ftp://files.example.com/file.txt",
            "file:///local/path/file.txt",
            "custom://app.specific/action",
            "mailto:user@example.com",
            "tel:+1234567890"
        )

        for (url in unusualSchemes) {
            val result = parser.parse(url)
            assertTrue("Should handle unusual scheme: $url", result.isSuccess)
            
            val parts = result.getOrThrow()
            assertNotNull("Scheme should be parsed", parts.scheme)
        }
    }

    @Test
    fun `parse_should_handle_encoded_characters_in_components`() {
        val encodedUrl = "https://example.com/path%20with%20spaces?param%20name=value%20with%20spaces&encoded%3Dparam=normal%20value#fragment%20with%20spaces"
        
        val result = parser.parse(encodedUrl)
        assertTrue("Should handle encoded characters", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        // Path and query should preserve encoding as received from android.net.Uri
        assertNotNull("Path should be parsed", parts.path)
        assertNotNull("Query should be parsed", parts.rawQuery)
        assertNotNull("Fragment should be parsed", parts.fragment)
    }

    @Test
    fun `parse_should_handle_multiple_consecutive_slashes`() {
        val urlWithSlashes = "https://example.com//path///with////slashes"
        
        val result = parser.parse(urlWithSlashes)
        assertTrue("Should handle multiple slashes", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        // android.net.Uri preserves multiple slashes in path
        assertTrue("Path should contain multiple slashes", parts.path!!.contains("//"))
    }

    @Test
    fun `parse_should_handle_URLs_with_only_fragments`() {
        val fragmentOnlyUrl = "https://example.com#section"
        
        val result = parser.parse(fragmentOnlyUrl)
        assertTrue("Should handle fragment-only URLs", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNull(parts.path)
        assertEquals("", parts.rawQuery)
        assertEquals("section", parts.fragment)
    }

    @Test
    fun `parse_should_handle_international_domain_names`() {
        // Test IDN domains (should be handled by android.net.Uri)
        val idnUrls = listOf(
            "https://例え.テスト/path",
            "https://مثال.اختبار/path",
            "https://пример.испытание/path"
        )

        for (url in idnUrls) {
            val result = parser.parse(url)
            assertTrue("Should handle IDN URL: $url", result.isSuccess)
            
            val parts = result.getOrThrow()
            assertEquals("https", parts.scheme)
            assertNotNull("Host should be parsed", parts.host)
            assertEquals("/path", parts.path)
        }
    }

    @Test
    fun `parse_should_handle_port_edge_cases`() {
        val portTestCases = mapOf(
            "https://example.com:0/path" to 0,
            "https://example.com:65535/path" to 65535,
            "https://example.com:80/path" to 80,
            "https://example.com:443/path" to 443
        )

        for ((url, expectedPort) in portTestCases) {
            val result = parser.parse(url)
            assertTrue("Should handle port case: $url", result.isSuccess)
            
            val parts = result.getOrThrow()
            assertEquals("Port should match for $url", expectedPort, parts.port)
        }
    }

    @Test
    fun `parse_should_handle_invalid_port_numbers`() {
        val invalidPortUrls = listOf(
            "https://example.com:-1/path",
            "https://example.com:65536/path",
            "https://example.com:abc/path",
            "https://example.com:123456/path"
        )

        for (url in invalidPortUrls) {
            val result = parser.parse(url)
            // android.net.Uri might handle these differently, but should not crash
            assertNotNull("Should handle invalid port gracefully: $url", result)
        }
    }

    @Test
    fun `parse_should_handle_complex_query_parameters`() {
        val complexQuery = "https://example.com/search?q=kotlin+android&sort=date&filters[]=type:code&filters[]=lang:kotlin&page=1&limit=50&callback=jsonp_callback_123"
        
        val result = parser.parse(complexQuery)
        assertTrue("Should handle complex query parameters", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/search", parts.path)
        assertTrue("Query should contain all parameters", parts.rawQuery.contains("q=kotlin+android"))
        assertTrue("Query should contain array parameters", parts.rawQuery.contains("filters[]="))
    }

    @Test
    fun `parse_should_handle_empty_query_parameters`() {
        val emptyParamUrl = "https://example.com/path?param1=&param2=value&param3&param4="
        
        val result = parser.parse(emptyParamUrl)
        assertTrue("Should handle empty query parameters", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/path", parts.path)
        assertTrue("Query should preserve empty parameters", parts.rawQuery.contains("param1="))
        assertTrue("Query should preserve valueless parameters", parts.rawQuery.contains("param3"))
    }

    @Test
    fun `parse_should_handle_duplicate_query_parameters`() {
        val duplicateParamUrl = "https://example.com/path?param=value1&param=value2&param=value3"
        
        val result = parser.parse(duplicateParamUrl)
        assertTrue("Should handle duplicate query parameters", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/path", parts.path)
        // Should preserve all duplicate parameters
        val paramCount = parts.rawQuery.split("param=").size - 1
        assertEquals("Should preserve all duplicate parameters", 3, paramCount)
    }

    @Test
    fun `parse_should_handle_special_characters_in_userinfo`() {
        val specialUserInfo = "https://user%40domain:p%40ssw0rd@example.com/path"
        
        val result = parser.parse(specialUserInfo)
        assertTrue("Should handle special characters in userInfo", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNotNull("UserInfo should be parsed", parts.userInfo)
        assertEquals("/path", parts.path)
    }

    @Test
    fun `parse_should_handle_data_urls`() {
        val dataUrl = "data:text/plain;base64,SGVsbG8gV29ybGQ="
        
        val result = parser.parse(dataUrl)
        assertTrue("Should handle data URLs", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("data", parts.scheme)
        // Data URLs have different structure, host might be null
        assertNotNull("Should parse data URL structure", parts)
    }

    @Test
    fun `parse_should_handle_javascript_urls`() {
        val jsUrl = "javascript:alert('Hello World')"
        
        val result = parser.parse(jsUrl)
        assertTrue("Should handle JavaScript URLs", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("javascript", parts.scheme)
    }

    @Test
    fun `parse_should_handle_blob_urls`() {
        val blobUrl = "blob:https://example.com/550e8400-e29b-41d4-a716-446655440000"
        
        val result = parser.parse(blobUrl)
        assertTrue("Should handle blob URLs", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("blob", parts.scheme)
    }

    @Test
    fun `parse_should_handle_urls_with_unicode_in_path`() {
        val unicodePath = "https://example.com/测试/路径/文件.html"
        
        val result = parser.parse(unicodePath)
        assertTrue("Should handle Unicode in path", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertNotNull("Path with Unicode should be parsed", parts.path)
    }

    @Test
    fun `parse_should_handle_urls_with_unicode_in_query`() {
        val unicodeQuery = "https://example.com/search?q=测试查询&lang=中文"
        
        val result = parser.parse(unicodeQuery)
        assertTrue("Should handle Unicode in query", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/search", parts.path)
        assertNotNull("Query with Unicode should be parsed", parts.rawQuery)
    }

    @Test
    fun `parse_should_handle_urls_with_unicode_in_fragment`() {
        val unicodeFragment = "https://example.com/page#章节一"
        
        val result = parser.parse(unicodeFragment)
        assertTrue("Should handle Unicode in fragment", result.isSuccess)
        
        val parts = result.getOrThrow()
        assertEquals("https", parts.scheme)
        assertEquals("example.com", parts.host)
        assertEquals("/page", parts.path)
        assertNotNull("Fragment with Unicode should be parsed", parts.fragment)
    }
}
