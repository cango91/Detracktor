package com.gologlu.detracktor.domain.model

import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.error.ValidationError
import com.gologlu.detracktor.domain.error.getOrThrow
import com.gologlu.detracktor.domain.error.isSuccess
import com.gologlu.detracktor.domain.error.isFailure
import com.gologlu.detracktor.domain.error.fold
import com.gologlu.detracktor.domain.service.UrlParser
import org.junit.Test
import org.junit.Assert.*

class UrlTest {

    private val mockUrlParser = object : UrlParser {
        override fun parse(url: MaybeUrl): DomainResult<UrlParts> {
            // Simple mock parser for testing
            return when {
                // More specific patterns first
                url.startsWith("https://example.com:8443") -> {
                    DomainResult.success(UrlParts(
                        scheme = "https",
                        host = "example.com",
                        port = 8443,
                        userInfo = null,
                        path = "/secure",
                        queryPairs = QueryPairs.from(null),
                        fragment = null
                    ))
                }
                url.startsWith("http://localhost:3000") -> {
                    DomainResult.success(UrlParts(
                        scheme = "http",
                        host = "localhost",
                        port = 3000,
                        userInfo = null,
                        path = "/dev",
                        queryPairs = QueryPairs.from(null),
                        fragment = null
                    ))
                }
                url.startsWith("https://example.com") -> {
                    val parts = url.split("?", limit = 2)
                    val baseUrl = parts[0]
                    val query = if (parts.size > 1) parts[1] else null
                    
                    val pathStart = baseUrl.indexOf("/", 8) // after "https://"
                    val path = if (pathStart != -1) baseUrl.substring(pathStart) else null
                    
                    DomainResult.success(UrlParts(
                        scheme = "https",
                        host = "example.com",
                        port = null,
                        userInfo = null,
                        path = path,
                        queryPairs = QueryPairs.from(query),
                        fragment = null
                    ))
                }
                url.startsWith("http://test.com") -> {
                    DomainResult.success(UrlParts(
                        scheme = "http",
                        host = "test.com",
                        port = null,
                        userInfo = null,
                        path = "/path",
                        queryPairs = QueryPairs.from(null),
                        fragment = null
                    ))
                }
                url == "invalid://no-host" -> {
                    DomainResult.success(UrlParts(
                        scheme = "invalid",
                        host = null, // Missing host
                        port = null,
                        userInfo = null,
                        path = null,
                        queryPairs = QueryPairs.from(null),
                        fragment = null
                    ))
                }
                url == "no-scheme://example.com" -> {
                    DomainResult.success(UrlParts(
                        scheme = null, // Missing scheme
                        host = "example.com",
                        port = null,
                        userInfo = null,
                        path = null,
                        queryPairs = QueryPairs.from(null),
                        fragment = null
                    ))
                }
                else -> DomainResult.failure(ValidationError.InvalidUrl("Cannot parse: $url"))
            }
        }
    }

    @Test
    fun `from should validate scheme is present`() {
        with(mockUrlParser) {
            val result = Url.from("no-scheme://example.com")
            
            assertTrue("Should fail when scheme is missing", result.isFailure)
            assertTrue("Should have appropriate error message", 
                result.fold({ false }, { it.message.contains("scheme is required") }))
        }
    }

    @Test
    fun `from should validate host is present`() {
        with(mockUrlParser) {
            val result = Url.from("invalid://no-host")
            
            assertTrue("Should fail when host is missing", result.isFailure)
            assertTrue("Should have appropriate error message", 
                result.fold({ false }, { it.message.contains("host is required") }))
        }
    }

    @Test
    fun `from should succeed with valid URL`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path?query=value")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            assertEquals("https", url.scheme)
            assertEquals("example.com", url.host)
        }
    }

    @Test
    fun `asString should work with valid URL from parser`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path?query=value")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            val urlString = url.asString()
            
            assertTrue("Should contain scheme", urlString.contains("https://"))
            assertTrue("Should contain host", urlString.contains("example.com"))
            assertTrue("Should contain path", urlString.contains("/path"))
            assertTrue("Should contain query", urlString.contains("?query=value"))
        }
    }

    @Test
    fun `queryPairs should preserve exact order and duplicates`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path?c=3&a=1&b=2&a=4")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            
            // Test QueryPairs preserves exact order
            assertEquals("c=3&a=1&b=2&a=4", url.queryPairs.asString())
            assertEquals(listOf("1", "4"), url.queryPairs.getAll("a"))
            assertEquals(listOf("2"), url.queryPairs.getAll("b"))
            assertEquals(listOf("3"), url.queryPairs.getAll("c"))
        }
    }

    @Test
    fun `queryMap should provide grouped access while preserving order`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path?c=3&a=1&b=2&a=4")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            
            // Test QueryMap provides grouped access
            assertEquals(listOf("1", "4"), url.queryMap.get("a"))
            assertEquals(listOf("2"), url.queryMap.get("b"))
            assertEquals(listOf("3"), url.queryMap.get("c"))
            
            // Test that QueryMap still preserves order through underlying QueryPairs
            assertEquals("c=3&a=1&b=2&a=4", url.queryMap.asString())
        }
    }

    @Test
    fun `URL reconstruction should preserve exact query parameter order`() {
        with(mockUrlParser) {
            val originalUrl = "https://example.com/path?z=last&a=first&a=second&b=middle&a=third"
            val result = Url.from(originalUrl)
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            val reconstructed = url.asString()
            
            // Should preserve exact query parameter order
            assertTrue("Should preserve query order", 
                reconstructed.contains("?z=last&a=first&a=second&b=middle&a=third"))
        }
    }

    @Test
    fun `both queryPairs and queryMap should be accessible`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path?param=value1&param=value2")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            
            // Both should be accessible and consistent
            assertEquals(2, url.queryPairs.getAll("param").size)
            assertEquals(2, url.queryMap.get("param").size)
            assertEquals(url.queryPairs.getAll("param"), url.queryMap.get("param"))
            
            // Both should produce the same string representation
            assertEquals(url.queryPairs.asString(), url.queryMap.asString())
        }
    }

    @Test
    fun `URL asString should be consistent with UrlParts toUrlString`() {
        // Test that IUrl.asString() produces consistent results by comparing with mock parser
        with(mockUrlParser) {
            val testUrl = "https://example.com/path?a=1&b=2"
            val result = Url.from(testUrl)
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            val urlString = url.asString()
            
            // Verify the URL string contains expected components
            assertTrue("Should contain scheme", urlString.contains("https://"))
            assertTrue("Should contain host", urlString.contains("example.com"))
            assertTrue("Should contain path", urlString.contains("/path"))
            assertTrue("Should contain query", urlString.contains("?a=1&b=2"))
        }
    }

    @Test
    fun `UrlParts toUrlString should handle IPv6 addresses correctly`() {
        val testCases = listOf(
            // IPv6 without port
            UrlParts("http", "::1", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]/path",
            
            // IPv6 with port
            UrlParts("https", "2001:db8::1", 8080, null, null, QueryPairs.empty(), null) to 
                "https://[2001:db8::1]:8080",
            
            // Already bracketed IPv6 (should not double-bracket)
            UrlParts("http", "[::1]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]/path",
            
            // IPv6 with query and fragment (port 443 is now always rendered)
            UrlParts("https", "fe80::1", 443, null, "/api", QueryPairs.from("test=1"), "section") to 
                "https://[fe80::1]:443/api?test=1#section",
            
            // IPv4 should not be bracketed
            UrlParts("http", "192.168.1.1", 8080, null, "/path", QueryPairs.empty(), null) to 
                "http://192.168.1.1:8080/path",
            
            // Regular hostname should not be bracketed
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com/path"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("IPv6 rendering failed for ${parts.host}", expected, result)
        }
    }

    @Test
    fun `UrlParts toUrlString should always render ports when present for lossless round-trip`() {
        val testCases = listOf(
            // Explicit default HTTP port should be rendered (lossless round-trip)
            UrlParts("http", "example.com", 80, null, "/path", QueryPairs.empty(), null) to 
                "http://example.com:80/path",
            
            // Explicit default HTTPS port should be rendered (lossless round-trip)
            UrlParts("https", "example.com", 443, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com:443/path",
            
            // Explicit default FTP port should be rendered (lossless round-trip)
            UrlParts("ftp", "example.com", 21, null, "/path", QueryPairs.empty(), null) to 
                "ftp://example.com:21/path",
            
            // Non-default ports should be included
            UrlParts("http", "example.com", 8080, null, "/path", QueryPairs.empty(), null) to 
                "http://example.com:8080/path",
            
            UrlParts("https", "example.com", 8443, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com:8443/path",
            
            // Unknown scheme should include port
            UrlParts("custom", "example.com", 80, null, "/path", QueryPairs.empty(), null) to 
                "custom://example.com:80/path",
            
            // No port specified should not render port
            UrlParts("http", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "http://example.com/path",
            
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com/path"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("Lossless port rendering failed for ${parts.scheme}:${parts.port}", expected, result)
        }
    }

    @Test
    fun `UrlParts toUrlString should handle userInfo correctly`() {
        val testCases = listOf(
            // With userInfo
            UrlParts("https", "example.com", null, "user:pass", "/path", QueryPairs.empty(), null) to 
                "https://user:pass@example.com/path",
            
            // Without userInfo
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com/path",
            
            // UserInfo with IPv6
            UrlParts("http", "::1", 8080, "admin:secret", "/api", QueryPairs.empty(), null) to 
                "http://admin:secret@[::1]:8080/api"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("UserInfo handling failed", expected, result)
        }
    }

    @Test
    fun `UrlParts toUrlString should handle query parameters and fragments correctly`() {
        val testCases = listOf(
            // With query parameters
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("a=1&b=2"), null) to 
                "https://example.com/path?a=1&b=2",
            
            // With fragment
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), "section") to 
                "https://example.com/path#section",
            
            // With both query and fragment
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("q=test"), "top") to 
                "https://example.com/path?q=test#top",
            
            // Empty query should not add ?
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com/path",
            
            // Complex query with edge cases
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("=value&flag&key="), null) to 
                "https://example.com/path?=value&flag&key="
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("Query/fragment handling failed", expected, result)
        }
    }

    // New tests for port handling fix

    @Test
    fun `IUrl port property should be accessible`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com:8443/secure")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            
            // Test that port property is accessible
            assertEquals(8443, url.port)
            assertEquals("https", url.scheme)
            assertEquals("example.com", url.host)
        }
    }

    @Test
    fun `IUrl port property should be null when not specified`() {
        with(mockUrlParser) {
            val result = Url.from("https://example.com/path")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            
            // Test that port property is null when not specified
            assertNull(url.port)
            assertEquals("https", url.scheme)
            assertEquals("example.com", url.host)
        }
    }

    @Test
    fun `IUrl asString should preserve port information`() {
        with(mockUrlParser) {
            val result = Url.from("http://localhost:3000/dev")
            
            assertTrue("Should succeed with valid URL", result.isSuccess)
            val url = result.getOrThrow()
            val urlString = url.asString()
            
            // Test that port is preserved in asString() output
            assertTrue("Should contain port", urlString.contains(":3000"))
            assertTrue("Should contain full URL", urlString.contains("http://localhost:3000/dev"))
            assertEquals(3000, url.port)
        }
    }

    @Test
    fun `IPv6 with zone ID should be handled correctly`() {
        // Test IPv6 addresses with zone IDs (percent-encoded)
        val testCases = listOf(
            // IPv6 with encoded zone ID
            UrlParts("http", "fe80::1%25eth0", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[fe80::1%25eth0]/path",
            
            // IPv6 with encoded zone ID and port
            UrlParts("https", "fe80::1%25lo0", 8443, null, "/api", QueryPairs.empty(), null) to 
                "https://[fe80::1%25lo0]:8443/api",
            
            // IPv6 with zone ID and query parameters
            UrlParts("http", "2001:db8::1%25eth1", 8080, null, "/test", QueryPairs.from("param=value"), null) to 
                "http://[2001:db8::1%25eth1]:8080/test?param=value"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("IPv6 zone ID handling failed for ${parts.host}", expected, result)
        }
    }

    @Test
    fun `userInfo with special characters should be handled correctly`() {
        // Test userInfo with special characters that might need encoding
        val testCases = listOf(
            // UserInfo with colon (standard case)
            UrlParts("https", "example.com", null, "user:pass", "/path", QueryPairs.empty(), null) to 
                "https://user:pass@example.com/path",
            
            // UserInfo with encoded characters (should be preserved as-is)
            UrlParts("https", "example.com", 8443, "user%40domain:pass%21", "/secure", QueryPairs.empty(), null) to 
                "https://user%40domain:pass%21@example.com:8443/secure",
            
            // UserInfo with IPv6 and port
            UrlParts("http", "::1", 3000, "admin:secret", "/admin", QueryPairs.empty(), null) to 
                "http://admin:secret@[::1]:3000/admin"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("UserInfo with special characters failed", expected, result)
        }
    }

    @Test
    fun `comprehensive URL reconstruction with all components`() {
        // Test comprehensive URL reconstruction with all components including port
        val parts = UrlParts(
            scheme = "https",
            host = "api.example.com",
            port = 8443,
            userInfo = "user:pass",
            path = "/v1/resource",
            queryPairs = QueryPairs.from("param1=value1&param2=value2&flag"),
            fragment = "section"
        )
        
        val result = parts.toUrlString()
        val expected = "https://user:pass@api.example.com:8443/v1/resource?param1=value1&param2=value2&flag#section"
        
        assertEquals("Comprehensive URL reconstruction failed", expected, result)
    }

    @Test
    fun `UrlParts rawQuery convenience accessor should work correctly`() {
        val testCases = listOf(
            // With query parameters
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("a=1&b=2"), null) to 
                "a=1&b=2",
            
            // Empty query
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "",
            
            // Complex query with edge cases
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("=value&flag&key="), null) to 
                "=value&flag&key=",
            
            // Query with encoded characters
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.from("key%20name=value%20data"), null) to 
                "key%20name=value%20data"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.rawQuery
            assertEquals("rawQuery accessor failed", expected, result)
            
            // Verify consistency with queryPairs.asString()
            assertEquals("rawQuery should match queryPairs.asString()", parts.queryPairs.asString(), result)
        }
    }

    @Test
    fun `lossless round-trip conversion with explicit default ports`() {
        // Test the core principle: from(urlString).asString() == urlString
        val testCases = listOf(
            "http://example.com:80/path",
            "https://example.com:443/secure",
            "ftp://example.com:21/files",
            "http://localhost:8080/dev",
            "https://api.example.com:8443/v1/resource?param=value",
            "http://[::1]:8080/ipv6",
            "https://[2001:db8::1]:443/ipv6-default"
        )
        
        // Mock parser that preserves ports exactly
        val losslessParser = object : UrlParser {
            override fun parse(url: MaybeUrl): DomainResult<UrlParts> {
                // Simple regex-based parsing for test purposes that handles IPv6
                val regex = """^(\w+)://(\[[^\]]+\]|[^:/]+)(?::(\d+))?(/[^?#]*)?(?:\?([^#]*))?(?:#(.*))?$""".toRegex()
                val match = regex.matchEntire(url)
                
                return if (match != null) {
                    val (scheme, host, portStr, path, query, fragment) = match.destructured
                    val port = if (portStr.isNotEmpty()) portStr.toInt() else null
                    
                    // Remove brackets from IPv6 addresses for storage
                    val cleanHost = if (host.startsWith("[") && host.endsWith("]")) {
                        host.substring(1, host.length - 1)
                    } else {
                        host
                    }
                    
                    DomainResult.success(UrlParts(
                        scheme = scheme,
                        host = cleanHost,
                        port = port,
                        userInfo = null,
                        path = path.ifEmpty { null },
                        queryPairs = QueryPairs.from(query.ifEmpty { null }),
                        fragment = fragment.ifEmpty { null }
                    ))
                } else {
                    DomainResult.failure(ValidationError.InvalidUrl("Cannot parse: $url"))
                }
            }
        }
        
        testCases.forEach { originalUrl ->
            with(losslessParser) {
                val result = Url.from(originalUrl)
                assertTrue("Should parse successfully: $originalUrl", result.isSuccess)
                
                val url = result.getOrThrow()
                val reconstructed = url.asString()
                
                assertEquals("Lossless round-trip failed for: $originalUrl", originalUrl, reconstructed)
            }
        }
    }

    @Test
    fun `lossless round-trip with trailing empty segments in query`() {
        // Test lossless round-trip with trailing empty segments in query
        val queries = listOf(
            "a=1&&b=2",
            "a=1&&b=2&",
            "a=1&&b=2&&",
            "a=1&&b=2&&&",
            "a=1&&b=2&&&   "
        )
        
        queries.forEach { query ->
            val roundTrip = QueryPairs.from(query).asString()
            assertEquals("Lossless round-trip failed for: $query", query, roundTrip)
        }
    }

    @Test
    fun `IPv6 double-bracketing edge cases should be prevented`() {
        // Test that already bracketed IPv6 addresses don't get double-bracketed
        val testCases = listOf(
            // Already bracketed IPv6 - should not add more brackets
            UrlParts("http", "[::1]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]/path",
            
            // Already bracketed IPv6 with port
            UrlParts("https", "[2001:db8::1]", 8443, null, "/api", QueryPairs.empty(), null) to 
                "https://[2001:db8::1]:8443/api",
            
            // Already bracketed IPv6 with zone ID
            UrlParts("http", "[fe80::1%25eth0]", 8080, null, "/test", QueryPairs.empty(), null) to 
                "http://[fe80::1%25eth0]:8080/test",
            
            // Unbracketed IPv6 - should add brackets
            UrlParts("http", "::1", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]/path",
            
            // Unbracketed IPv6 with port
            UrlParts("https", "2001:db8::1", 8443, null, "/api", QueryPairs.empty(), null) to 
                "https://[2001:db8::1]:8443/api",
            
            // Partial bracketing (only start bracket) - should add end bracket
            UrlParts("http", "[::1", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[[::1]/path",
            
            // Partial bracketing (only end bracket) - should add start bracket  
            UrlParts("http", "::1]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]]/path",
            
            // IPv4 addresses should never be bracketed
            UrlParts("http", "192.168.1.1", 8080, null, "/path", QueryPairs.empty(), null) to 
                "http://192.168.1.1:8080/path",
            
            // Hostnames should never be bracketed
            UrlParts("https", "example.com", null, null, "/path", QueryPairs.empty(), null) to 
                "https://example.com/path",
            
            // Hostname with colon in path should not be bracketed
            UrlParts("https", "example.com", null, null, "/path:with:colons", QueryPairs.empty(), null) to 
                "https://example.com/path:with:colons"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("IPv6 bracketing failed for ${parts.host}", expected, result)
        }
    }

    @Test
    fun `toString overrides should work for debugging and logging`() {
        // Test UrlParts toString override
        val urlParts = UrlParts(
            scheme = "https",
            host = "example.com",
            port = 8443,
            userInfo = "user:pass",
            path = "/api/v1",
            queryPairs = QueryPairs.from("param=value&flag"),
            fragment = "section"
        )
        
        val toStringResult = urlParts.toString()
        val toUrlStringResult = urlParts.toUrlString()
        
        assertEquals("UrlParts toString should delegate to toUrlString", toUrlStringResult, toStringResult)
        assertTrue("Should contain all components", toStringResult.contains("https://user:pass@example.com:8443/api/v1?param=value&flag#section"))
        
        // Test Url toString override
        with(mockUrlParser) {
            val urlResult = Url.from("https://example.com/path?query=value")
            assertTrue("Should succeed", urlResult.isSuccess)
            
            val url = urlResult.getOrThrow()
            val urlToString = url.toString()
            val urlAsString = url.asString()
            
            assertEquals("Url toString should delegate to asString", urlAsString, urlToString)
            assertTrue("Should be useful for debugging", urlToString.contains("https://"))
            assertTrue("Should be useful for debugging", urlToString.contains("example.com"))
        }
    }

    @Test
    fun `copyWithQuery should create copy with new query parameters`() {
        val originalParts = UrlParts(
            scheme = "https",
            host = "example.com",
            port = 8443,
            userInfo = "user:pass",
            path = "/api/v1",
            queryPairs = QueryPairs.from("old=param"),
            fragment = "section"
        )
        
        val newQuery = QueryPairs.from("new=param&another=value")
        val updatedParts = originalParts.copyWithQuery(newQuery)
        
        // Should preserve all other components
        assertEquals("Scheme should be preserved", originalParts.scheme, updatedParts.scheme)
        assertEquals("Host should be preserved", originalParts.host, updatedParts.host)
        assertEquals("Port should be preserved", originalParts.port, updatedParts.port)
        assertEquals("UserInfo should be preserved", originalParts.userInfo, updatedParts.userInfo)
        assertEquals("Path should be preserved", originalParts.path, updatedParts.path)
        assertEquals("Fragment should be preserved", originalParts.fragment, updatedParts.fragment)
        
        // Should update query parameters
        assertEquals("Query should be updated", newQuery, updatedParts.queryPairs)
        assertEquals("Query string should be updated", "new=param&another=value", updatedParts.rawQuery)
        
        // Should produce correct URL string
        val expectedUrl = "https://user:pass@example.com:8443/api/v1?new=param&another=value#section"
        assertEquals("URL string should be correct", expectedUrl, updatedParts.toUrlString())
    }

    @Test
    fun `copyWithQuery should work with empty query parameters`() {
        val originalParts = UrlParts(
            scheme = "http",
            host = "localhost",
            port = 3000,
            userInfo = null,
            path = "/api",
            queryPairs = QueryPairs.from("old=param&another=old"),
            fragment = null
        )
        
        val emptyQuery = QueryPairs.empty()
        val updatedParts = originalParts.copyWithQuery(emptyQuery)
        
        // Should preserve other components
        assertEquals("Scheme should be preserved", "http", updatedParts.scheme)
        assertEquals("Host should be preserved", "localhost", updatedParts.host)
        assertEquals("Port should be preserved", 3000, updatedParts.port)
        assertEquals("Path should be preserved", "/api", updatedParts.path)
        
        // Should clear query parameters
        assertTrue("Query should be empty", updatedParts.queryPairs.isEmpty())
        assertEquals("Raw query should be empty", "", updatedParts.rawQuery)
        
        // Should produce URL without query string
        assertEquals("URL should not have query", "http://localhost:3000/api", updatedParts.toUrlString())
    }

    @Test
    fun `copyWithQuery should preserve order and duplicates in new query`() {
        val originalParts = UrlParts(
            scheme = "https",
            host = "api.example.com",
            port = null,
            userInfo = null,
            path = "/v1/resource",
            queryPairs = QueryPairs.from("old=value"),
            fragment = null
        )
        
        val complexQuery = QueryPairs.from("z=last&a=first&a=second&b=middle&a=third")
        val updatedParts = originalParts.copyWithQuery(complexQuery)
        
        // Should preserve exact order and duplicates
        assertEquals("Query order should be preserved", "z=last&a=first&a=second&b=middle&a=third", updatedParts.rawQuery)
        assertEquals("Should have correct values for 'a'", listOf("first", "second", "third"), updatedParts.queryPairs.getAll("a"))
        
        // Should produce correct URL
        val expectedUrl = "https://api.example.com/v1/resource?z=last&a=first&a=second&b=middle&a=third"
        assertEquals("URL should preserve query order", expectedUrl, updatedParts.toUrlString())
    }

    @Test
    fun `copyWithQuery should be useful for URL manipulation`() {
        // Test a common use case: adding/modifying query parameters while preserving URL structure
        val baseParts = UrlParts(
            scheme = "https",
            host = "api.service.com",
            port = null,
            userInfo = null,
            path = "/search",
            queryPairs = QueryPairs.empty(),
            fragment = null
        )
        
        // Add search parameters
        val searchQuery = QueryPairs.from("q=kotlin&type=code&sort=updated")
        val searchUrl = baseParts.copyWithQuery(searchQuery)
        
        assertEquals("Search URL should be correct", 
            "https://api.service.com/search?q=kotlin&type=code&sort=updated", 
            searchUrl.toUrlString())
        
        // Modify search parameters (add pagination)
        val paginatedQuery = QueryPairs.from("q=kotlin&type=code&sort=updated&page=2&limit=50")
        val paginatedUrl = baseParts.copyWithQuery(paginatedQuery)
        
        assertEquals("Paginated URL should be correct",
            "https://api.service.com/search?q=kotlin&type=code&sort=updated&page=2&limit=50",
            paginatedUrl.toUrlString())
        
        // Clear search parameters
        val clearedUrl = baseParts.copyWithQuery(QueryPairs.empty())
        
        assertEquals("Cleared URL should be correct",
            "https://api.service.com/search",
            clearedUrl.toUrlString())
    }

    @Test
    fun `IPv6 edge cases with malformed brackets should be handled gracefully`() {
        // Test various malformed bracket scenarios
        val testCases = listOf(
            // Multiple opening brackets
            UrlParts("http", "[[::1", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[[[::1]/path",
            
            // Multiple closing brackets  
            UrlParts("http", "::1]]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[::1]]]/path",
            
            // Brackets in wrong order
            UrlParts("http", "]::1[", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[]::1[]/path",
            
            // Empty brackets
            UrlParts("http", "[]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[]/path",
            
            // Brackets with no IPv6 content
            UrlParts("http", "[not-ipv6]", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[not-ipv6]/path",
            
            // Normal IPv6 should still work correctly
            UrlParts("http", "2001:db8::1", null, null, "/path", QueryPairs.empty(), null) to 
                "http://[2001:db8::1]/path"
        )
        
        testCases.forEach { (parts, expected) ->
            val result = parts.toUrlString()
            assertEquals("Malformed bracket handling failed for ${parts.host}", expected, result)
        }
    }

}
