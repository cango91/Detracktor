package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.SegmentType
import org.junit.Test
import org.junit.Assert.*
import java.util.regex.Pattern

/**
 * Test suite for the UrlAnalyzer.
 * Focuses on URL segmentation and smart partial-blur rendering.
 */
class UrlAnalyzerTest {
    
    private val urlAnalyzer = UrlAnalyzer()
    
    @Test
    fun testAnalyzeUrl_simpleUrl() {
        val url = "https://example.com/page"
        val patterns = emptyList<Pattern>()
        
        val analysis = urlAnalyzer.analyzeUrl(url, patterns)
        
        assertEquals(url, analysis.originalUrl)
        assertEquals(url, analysis.cleanedUrl) // No parameters to clean
        assertFalse(analysis.hasEmbeddedCredentials)
        assertTrue(analysis.segments.isNotEmpty())
    }
    
    @Test
    fun testAnalyzeUrl_urlWithParameters() {
        val url = "https://example.com/page?param1=value1&param2=value2"
        val patterns = listOf(Pattern.compile("param1"))
        
        val analysis = urlAnalyzer.analyzeUrl(url, patterns)
        
        assertEquals(url, analysis.originalUrl)
        assertEquals("https://example.com/page?param2=value2", analysis.cleanedUrl)
        assertFalse(analysis.hasEmbeddedCredentials)
    }
    
    @Test
    fun testAnalyzeUrl_urlWithCredentials() {
        val url = "https://user:pass@example.com/page"
        val patterns = emptyList<Pattern>()
        
        val analysis = urlAnalyzer.analyzeUrl(url, patterns)
        
        assertEquals(url, analysis.originalUrl)
        assertTrue(analysis.hasEmbeddedCredentials)
    }
    
    @Test
    fun testCreateSegments_basicUrl() {
        val url = "https://example.com/page"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        assertTrue(segments.any { it.type == SegmentType.PROTOCOL })
        assertTrue(segments.any { it.type == SegmentType.HOST })
        assertTrue(segments.any { it.type == SegmentType.PATH })
        assertTrue(segments.any { it.type == SegmentType.SEPARATOR })
    }
    
    @Test
    fun testCreateSegments_urlWithCredentials() {
        val url = "https://user:pass@example.com/page"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        val credentialSegments = segments.filter { it.type == SegmentType.CREDENTIALS }
        assertEquals(1, credentialSegments.size)
        assertTrue(credentialSegments[0].shouldBlur) // Credentials should always be blurred
    }
    
    @Test
    fun testCreateSegments_urlWithPort() {
        val url = "https://example.com:8080/page"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        val hostSegments = segments.filter { it.type == SegmentType.HOST }
        assertEquals(2, hostSegments.size) // Host and port
        assertTrue(hostSegments.any { it.text == "example.com" })
        assertTrue(hostSegments.any { it.text == "8080" })
    }
    
    @Test
    fun testCreateSegments_urlWithParameters() {
        val url = "https://example.com/page?param1=value1&param2=value2"
        val patterns = listOf(Pattern.compile("param1"))
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        val paramNameSegments = segments.filter { it.type == SegmentType.PARAM_NAME }
        val paramValueSegments = segments.filter { it.type == SegmentType.PARAM_VALUE }
        
        assertEquals(2, paramNameSegments.size)
        assertEquals(2, paramValueSegments.size)
        
        // Find param1 value by looking at segments in order
        var param1Value: com.gologlu.detracktor.data.AnnotatedUrlSegment? = null
        var param2Value: com.gologlu.detracktor.data.AnnotatedUrlSegment? = null
        
        for (i in segments.indices) {
            if (segments[i].type == SegmentType.PARAM_NAME && segments[i].text == "param1") {
                // Look for the value segment after this name (skip the "=" separator)
                if (i + 2 < segments.size && segments[i + 2].type == SegmentType.PARAM_VALUE) {
                    param1Value = segments[i + 2]
                }
            }
            if (segments[i].type == SegmentType.PARAM_NAME && segments[i].text == "param2") {
                // Look for the value segment after this name (skip the "=" separator)
                if (i + 2 < segments.size && segments[i + 2].type == SegmentType.PARAM_VALUE) {
                    param2Value = segments[i + 2]
                }
            }
        }
        
        // param1 value should not be blurred (matches pattern)
        assertNotNull(param1Value)
        assertFalse(param1Value!!.shouldBlur)
        
        // param2 value should be blurred (doesn't match pattern)
        assertNotNull(param2Value)
        assertTrue(param2Value!!.shouldBlur)
    }
    
    @Test
    fun testCreateSegments_urlWithFragment() {
        val url = "https://example.com/page#section"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        assertTrue(segments.any { it.text == "section" && it.type == SegmentType.PATH })
        assertTrue(segments.any { it.text == "#" && it.type == SegmentType.SEPARATOR })
    }
    
    @Test
    fun testCreateSegments_complexUrl() {
        val url = "https://user:pass@api.example.com:8080/v1/data?api_key=secret&public=info#results"
        val patterns = listOf(Pattern.compile("public"))
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        // Verify all segment types are present
        assertTrue(segments.any { it.type == SegmentType.PROTOCOL })
        assertTrue(segments.any { it.type == SegmentType.CREDENTIALS && it.shouldBlur })
        assertTrue(segments.any { it.type == SegmentType.HOST })
        assertTrue(segments.any { it.type == SegmentType.PATH })
        assertTrue(segments.any { it.type == SegmentType.PARAM_NAME })
        assertTrue(segments.any { it.type == SegmentType.PARAM_VALUE })
        assertTrue(segments.any { it.type == SegmentType.SEPARATOR })
        
        // Verify blurring logic
        val paramValues = segments.filter { it.type == SegmentType.PARAM_VALUE }
        val secretValue = paramValues.find { it.text == "secret" }
        val publicValue = paramValues.find { it.text == "info" }
        
        assertNotNull(secretValue)
        assertNotNull(publicValue)
        assertTrue(secretValue!!.shouldBlur) // api_key doesn't match pattern
        assertFalse(publicValue!!.shouldBlur) // public matches pattern
    }
    
    @Test
    fun testCreateSegments_invalidUrl() {
        val url = "not-a-valid-url"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        // Should fallback to single segment
        assertEquals(1, segments.size)
        assertEquals(url, segments[0].text)
        assertEquals(SegmentType.HOST, segments[0].type)
    }
    
    @Test
    fun testCreateSegments_urlWithoutParameters() {
        val url = "https://example.com/path/to/resource"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        // Should not have any parameter segments
        assertFalse(segments.any { it.type == SegmentType.PARAM_NAME })
        assertFalse(segments.any { it.type == SegmentType.PARAM_VALUE })
        assertTrue(segments.any { it.type == SegmentType.PATH })
    }
    
    @Test
    fun testCreateSegments_urlWithEmptyParameterValue() {
        val url = "https://example.com/page?param1=&param2=value"
        val patterns = emptyList<Pattern>()
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        val paramNames = segments.filter { it.type == SegmentType.PARAM_NAME }
        val paramValues = segments.filter { it.type == SegmentType.PARAM_VALUE }
        
        assertEquals(2, paramNames.size)
        assertEquals(1, paramValues.size) // Only param2 has a value
    }
    
    @Test
    fun testCreateSegments_multipleMatchingPatterns() {
        val url = "https://example.com/page?utm_source=google&utm_medium=cpc&other=value"
        val patterns = listOf(
            Pattern.compile("utm_.*"),
            Pattern.compile("other")
        )
        
        val segments = urlAnalyzer.createSegments(url, patterns)
        
        val paramValues = segments.filter { it.type == SegmentType.PARAM_VALUE }
        
        // All parameter values should not be blurred as they match patterns
        paramValues.forEach { segment ->
            assertFalse("Parameter value '${segment.text}' should not be blurred", segment.shouldBlur)
        }
    }
}
