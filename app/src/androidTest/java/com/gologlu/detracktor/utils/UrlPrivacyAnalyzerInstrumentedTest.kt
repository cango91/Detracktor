package com.gologlu.detracktor.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Instrumented tests for UrlPrivacyAnalyzer - comprehensive URL parsing behavior tests.
 * These tests use actual Android Uri.parse() functionality and count towards test coverage.
 */
@RunWith(AndroidJUnit4::class)
class UrlPrivacyAnalyzerInstrumentedTest {

    @Test
    fun testBasicUrlAnalysis() {
        val url = "https://example.com/path?param=value"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path", analysis.path)
        assertEquals("", analysis.port)
        assertEquals("", analysis.fragment)
        assertFalse(analysis.hasCredentials)
        assertEquals(0, analysis.matchingParameters.size)
        assertEquals(1, analysis.nonMatchingParameters.size)
        assertEquals("value", analysis.nonMatchingParameters["param"])
    }

    @Test
    fun testUrlWithCredentials() {
        val url = "https://user:pass@example.com/path"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path", analysis.path)
        assertTrue(analysis.hasCredentials)
    }

    @Test
    fun testUrlWithPort() {
        val url = "https://example.com:8080/path"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path", analysis.path)
        assertEquals("8080", analysis.port)
    }

    @Test
    fun testUrlWithFragment() {
        val url = "https://example.com/path#section"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path", analysis.path)
        assertEquals("section", analysis.fragment)
    }

    @Test
    fun testParameterCategorization() {
        val url = "https://example.com/path?token=abc123&password=secret&api_key=xyz789"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        // All parameters should be non-matching since no rules provided
        assertEquals(0, analysis.matchingParameters.size)
        assertEquals(3, analysis.nonMatchingParameters.size)
        assertEquals("abc123", analysis.nonMatchingParameters["token"])
        assertEquals("secret", analysis.nonMatchingParameters["password"])
        assertEquals("xyz789", analysis.nonMatchingParameters["api_key"])
    }

    @Test
    fun testMatchingParametersWithRules() {
        val url = "https://example.com/path?utm_source=google&utm_medium=cpc&normal_param=value"
        val parametersToRemove = listOf("utm_source", "utm_medium")
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, parametersToRemove)

        assertEquals(2, analysis.matchingParameters.size)
        assertEquals("google", analysis.matchingParameters["utm_source"])
        assertEquals("cpc", analysis.matchingParameters["utm_medium"])
        assertEquals(1, analysis.nonMatchingParameters.size)
        assertEquals("value", analysis.nonMatchingParameters["normal_param"])
    }

    @Test
    fun testPathHandling() {
        val url = "https://example.com/api/v1/users/12345/token/abc123def456"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/api/v1/users/12345/token/abc123def456", analysis.path)
    }

    @Test
    fun testFragmentHandling() {
        val url = "https://example.com/path#access_token=abc123"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("access_token=abc123", analysis.fragment)
    }

    @Test
    fun testComplexUrlAnalysis() {
        val url = "https://user:pass@api.example.com:443/v1/users/12345?utm_source=google&api_key=secret123&page=1&token=abc#access_token=xyz"
        val parametersToRemove = listOf("utm_source")
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, parametersToRemove)

        // Basic components
        assertEquals("https", analysis.scheme)
        assertEquals("api.example.com", analysis.safeHost)
        assertEquals("", analysis.port) // Port 443 is standard for HTTPS, so it should be empty
        assertEquals("/v1/users/12345", analysis.path)
        assertEquals("access_token=xyz", analysis.fragment)
        assertTrue(analysis.hasCredentials)

        // Parameters categorization
        assertEquals(1, analysis.matchingParameters.size)
        assertEquals("google", analysis.matchingParameters["utm_source"])
        
        assertEquals(3, analysis.nonMatchingParameters.size)
        assertEquals("secret123", analysis.nonMatchingParameters["api_key"])
        assertEquals("1", analysis.nonMatchingParameters["page"])
        assertEquals("abc", analysis.nonMatchingParameters["token"])
    }

    @Test
    fun testUrlWithOnlyQueryParameters() {
        val url = "https://example.com?param1=value1&param2=value2&token=secret"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, listOf("param1"))

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("", analysis.path)
        assertEquals(1, analysis.matchingParameters.size)
        assertEquals("value1", analysis.matchingParameters["param1"])
        assertEquals(2, analysis.nonMatchingParameters.size)
        assertEquals("value2", analysis.nonMatchingParameters["param2"])
        assertEquals("secret", analysis.nonMatchingParameters["token"])
    }

    @Test
    fun testUrlWithSpecialCharactersInPath() {
        val url = "https://example.com/path/with%20spaces/and-dashes_underscores"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path/with%20spaces/and-dashes_underscores", analysis.path)
    }

    @Test
    fun testUrlWithStandardPorts() {
        // Standard HTTP port (80) should not be included
        val httpUrl = "http://example.com:80/path"
        val httpAnalysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(httpUrl, emptyList())
        assertEquals("", httpAnalysis.port)

        // Standard HTTPS port (443) should not be included
        val httpsUrl = "https://example.com:443/path"
        val httpsAnalysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(httpsUrl, emptyList())
        assertEquals("", httpsAnalysis.port)

        // Non-standard port should be included
        val customPortUrl = "https://example.com:8080/path"
        val customPortAnalysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(customPortUrl, emptyList())
        assertEquals("8080", customPortAnalysis.port)
    }

    @Test
    fun testUrlWithMultipleParametersOfSameName() {
        // Android Uri.parse handles multiple parameters with same name by returning the last value
        val url = "https://example.com?param=value1&param=value2"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals(1, analysis.nonMatchingParameters.size)
        assertEquals("value2", analysis.nonMatchingParameters["param"])
    }

    @Test
    fun testUrlWithEmptyParameterValues() {
        val url = "https://example.com?empty=&normal=value&blank"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, listOf("empty"))

        assertEquals(1, analysis.matchingParameters.size)
        assertEquals("", analysis.matchingParameters["empty"])
        assertEquals(2, analysis.nonMatchingParameters.size)
        assertEquals("value", analysis.nonMatchingParameters["normal"])
        assertEquals("", analysis.nonMatchingParameters["blank"])
    }

    @Test
    fun testHttpUrlAnalysis() {
        val url = "http://example.com/path?param=value"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("http", analysis.scheme)
        assertEquals("example.com", analysis.safeHost)
        assertEquals("/path", analysis.path)
        assertEquals("", analysis.port)
        assertFalse(analysis.hasCredentials)
    }

    @Test
    fun testUrlWithComplexQueryString() {
        val url = "https://example.com/search?q=hello+world&sort=date&filter=all&utm_source=google&utm_medium=cpc"
        val parametersToRemove = listOf("utm_source", "utm_medium")
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, parametersToRemove)

        assertEquals(2, analysis.matchingParameters.size)
        assertEquals("google", analysis.matchingParameters["utm_source"])
        assertEquals("cpc", analysis.matchingParameters["utm_medium"])
        
        assertEquals(3, analysis.nonMatchingParameters.size)
        assertEquals("hello+world", analysis.nonMatchingParameters["q"])
        assertEquals("date", analysis.nonMatchingParameters["sort"])
        assertEquals("all", analysis.nonMatchingParameters["filter"])
    }

    @Test
    fun testUrlWithSubdomain() {
        val url = "https://api.v2.example.com:9000/endpoint?key=value"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, emptyList())

        assertEquals("https", analysis.scheme)
        assertEquals("api.v2.example.com", analysis.safeHost)
        assertEquals("9000", analysis.port)
        assertEquals("/endpoint", analysis.path)
        assertEquals(1, analysis.nonMatchingParameters.size)
        assertEquals("value", analysis.nonMatchingParameters["key"])
    }

    @Test
    fun testUrlWithEncodedCharacters() {
        val url = "https://example.com/path?name=John%20Doe&email=john%40example.com"
        val analysis = UrlPrivacyAnalyzer.analyzeUrlEnhanced(url, listOf("name"))

        assertEquals(1, analysis.matchingParameters.size)
        assertEquals("John Doe", analysis.matchingParameters["name"]) // Android Uri decodes automatically
        assertEquals(1, analysis.nonMatchingParameters.size)
        assertEquals("john@example.com", analysis.nonMatchingParameters["email"]) // Android Uri decodes automatically
    }
}
