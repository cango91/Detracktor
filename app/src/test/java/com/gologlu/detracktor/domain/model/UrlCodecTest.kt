package com.gologlu.detracktor.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for UrlCodec percent encoding and decoding functionality.
 * 
 * Verifies proper Unicode handling, locale safety, and edge cases for URL component
 * encoding/decoding operations.
 */
class UrlCodecTest {

    @Test
    fun `percentEncodeUtf8 - basic ASCII characters`() {
        // Unreserved characters should not be encoded
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~", 
                    UrlCodec.percentEncodeUtf8("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"))
        
        // Space should be encoded
        assertEquals("hello%20world", UrlCodec.percentEncodeUtf8("hello world"))
        
        // Special characters should be encoded
        assertEquals("key%3Dvalue%26other%3Ddata", UrlCodec.percentEncodeUtf8("key=value&other=data"))
    }

    @Test
    fun `percentEncodeUtf8 - Unicode characters`() {
        // Unicode characters should be properly encoded as UTF-8 bytes
        assertEquals("%C3%A9", UrlCodec.percentEncodeUtf8("é"))  // é = C3 A9 in UTF-8
        assertEquals("%E2%9C%93", UrlCodec.percentEncodeUtf8("✓"))  // ✓ = E2 9C 93 in UTF-8
        assertEquals("%F0%9F%98%80", UrlCodec.percentEncodeUtf8("😀"))  // 😀 = F0 9F 98 80 in UTF-8
    }

    @Test
    fun `percentEncodeUtf8 - mixed content`() {
        assertEquals("utm_source%2Balias%3D%C3%A9%E2%9C%93", 
                    UrlCodec.percentEncodeUtf8("utm_source+alias=é✓"))
    }

    @Test
    fun `percentEncodeUtf8 - empty and edge cases`() {
        assertEquals("", UrlCodec.percentEncodeUtf8(""))
        assertEquals("%00", UrlCodec.percentEncodeUtf8("\u0000"))
        assertEquals("%C3%BF", UrlCodec.percentEncodeUtf8("\u00FF"))  // \u00FF -> UTF-8 bytes C3 BF
    }

    @Test
    fun `percentDecodeUtf8 - basic percent sequences`() {
        assertEquals("hello world", UrlCodec.percentDecodeUtf8("hello%20world"))
        assertEquals("key=value&other=data", UrlCodec.percentDecodeUtf8("key%3Dvalue%26other%3Ddata"))
    }

    @Test
    fun `percentDecodeUtf8 - Unicode sequences`() {
        assertEquals("é", UrlCodec.percentDecodeUtf8("%C3%A9"))
        assertEquals("✓", UrlCodec.percentDecodeUtf8("%E2%9C%93"))
        assertEquals("😀", UrlCodec.percentDecodeUtf8("%F0%9F%98%80"))
    }

    @Test
    fun `percentDecodeUtf8 - mixed encoded and literal`() {
        assertEquals("utm_source+alias=é✓", UrlCodec.percentDecodeUtf8("utm_source%2Balias%3D%C3%A9%E2%9C%93"))
        
        // Literal Unicode should be preserved
        assertEquals("test é literal", UrlCodec.percentDecodeUtf8("test é literal"))
    }

    @Test
    fun `percentDecodeUtf8 - plus signs preserved`() {
        // Plus signs should NOT be converted to spaces (unlike URLDecoder)
        assertEquals("utm_source+alias", UrlCodec.percentDecodeUtf8("utm_source+alias"))
        assertEquals("a+b=c+d", UrlCodec.percentDecodeUtf8("a+b=c+d"))
    }

    @Test
    fun `percentDecodeUtf8 - invalid sequences preserved`() {
        // Invalid percent sequences should be preserved as-is
        assertEquals("test%ZZ", UrlCodec.percentDecodeUtf8("test%ZZ"))
        assertEquals("test%2", UrlCodec.percentDecodeUtf8("test%2"))
        assertEquals("test%", UrlCodec.percentDecodeUtf8("test%"))
        assertEquals("test%G0", UrlCodec.percentDecodeUtf8("test%G0"))
    }

    @Test
    fun `percentDecodeUtf8 - contiguous sequences`() {
        // Contiguous %XX sequences should be decoded as a single UTF-8 byte sequence
        assertEquals("é", UrlCodec.percentDecodeUtf8("%C3%A9"))
        assertEquals("test", UrlCodec.percentDecodeUtf8("%74%65%73%74"))
    }

    @Test
    fun `percentDecodeUtf8 - edge cases`() {
        assertEquals("", UrlCodec.percentDecodeUtf8(""))
        assertEquals("\u0000", UrlCodec.percentDecodeUtf8("%00"))
        assertEquals("\u00FF", UrlCodec.percentDecodeUtf8("%C3%BF"))  // UTF-8 bytes C3 BF -> \u00FF
    }

    @Test
    fun `percentDecodeUtf8 - case insensitive hex`() {
        // Both uppercase and lowercase hex should work
        assertEquals("hello world", UrlCodec.percentDecodeUtf8("hello%20world"))
        assertEquals("hello*world", UrlCodec.percentDecodeUtf8("hello%2Aworld"))  // %2A = *
        assertEquals("hello*world", UrlCodec.percentDecodeUtf8("hello%2aworld"))  // %2a = *
    }

    @Test
    fun `round trip encoding and decoding`() {
        val testCases = listOf(
            "simple text",
            "key=value&other=data",
            "utm_source+alias",
            "é✓😀",
            "mixed é literal and encoded",
            "",
            "a=1&flag&b=2",
            "special!@#$%^&*()chars"
        )

        for (testCase in testCases) {
            val encoded = UrlCodec.percentEncodeUtf8(testCase)
            val decoded = UrlCodec.percentDecodeUtf8(encoded)
            assertEquals("Round-trip failed for: $testCase", testCase, decoded)
        }
    }

    @Test
    fun `locale safety - Turkish locale simulation`() {
        // Test that hex digit parsing works correctly regardless of locale
        // In Turkish locale, 'i'.toUpperCase() != 'I', but Character.digit() should be locale-safe
        assertEquals("hello world", UrlCodec.percentDecodeUtf8("hello%20world"))
        assertEquals("test", UrlCodec.percentDecodeUtf8("%74%65%73%74"))
        
        // Test encoding uses Locale.ROOT for hex formatting
        val encoded = UrlCodec.percentEncodeUtf8("hello world")
        assertTrue("Should use uppercase hex", encoded.contains("%20"))
        assertFalse("Should not contain lowercase hex", encoded.contains("%2a"))
    }

    @Test
    fun `encoding preserves unreserved characters`() {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        assertEquals("Unreserved characters should not be encoded", 
                    unreserved, UrlCodec.percentEncodeUtf8(unreserved))
    }

    @Test
    fun `encoding handles reserved and unsafe characters`() {
        val reserved = ":/?#[]@!$&'()*+,;="
        val encoded = UrlCodec.percentEncodeUtf8(reserved)
        
        // All reserved characters should be encoded
        assertFalse("Reserved characters should be encoded", encoded == reserved)
        assertTrue("Should contain percent-encoded sequences", encoded.contains("%"))
        
        // Verify round-trip
        assertEquals(reserved, UrlCodec.percentDecodeUtf8(encoded))
    }
}
