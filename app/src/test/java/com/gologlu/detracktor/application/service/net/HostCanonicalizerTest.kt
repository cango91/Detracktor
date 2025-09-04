package com.gologlu.detracktor.application.service.net

import com.gologlu.detracktor.runtime.android.service.net.HostCanonicalizer
import org.junit.Test
import org.junit.Assert.*

class HostCanonicalizerTest {

    @Test
    fun toAscii_should_handle_null_input() {
        assertNull(HostCanonicalizer.toAscii(null))
    }

    @Test
    fun toAscii_should_handle_empty_input() {
        assertNull(HostCanonicalizer.toAscii(""))
    }

    @Test
    fun toAscii_should_handle_basic_ascii_domains() {
        assertEquals("example.com", HostCanonicalizer.toAscii("example.com"))
        assertEquals("google.com", HostCanonicalizer.toAscii("google.com"))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub.domain.com"))
    }

    @Test
    fun toAscii_should_convert_to_lowercase() {
        assertEquals("example.com", HostCanonicalizer.toAscii("EXAMPLE.COM"))
        assertEquals("google.com", HostCanonicalizer.toAscii("Google.Com"))
        assertEquals("mixed.case.domain", HostCanonicalizer.toAscii("MiXeD.CaSe.DoMaIn"))
    }

    @Test
    fun `toAscii_should_remove_trailing_dot`() {
        assertEquals("example.com", HostCanonicalizer.toAscii("example.com."))
        assertEquals("google.com", HostCanonicalizer.toAscii("GOOGLE.COM."))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub.domain.com."))
    }

    @Test
    fun `toAscii_should_handle_multiple_trailing_dots`() {
        assertEquals("example.com", HostCanonicalizer.toAscii("example.com.."))
        assertEquals("example.com", HostCanonicalizer.toAscii("example.com..."))
        assertEquals("example.com.", HostCanonicalizer.toAscii("example.com...."))
    }

    @Test
    fun `toAscii_should_convert_ideographic_full_stop`() {
        // U+3002 IDEOGRAPHIC FULL STOP
        assertEquals("example.com", HostCanonicalizer.toAscii("example。com"))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub。domain。com"))
        assertEquals("example.com", HostCanonicalizer.toAscii("example。com。"))
    }

    @Test
    fun `toAscii_should_convert_fullwidth_full_stop`() {
        // U+FF0E FULLWIDTH FULL STOP
        assertEquals("example.com", HostCanonicalizer.toAscii("example．com"))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub．domain．com"))
        assertEquals("example.com", HostCanonicalizer.toAscii("example．com．"))
    }

    @Test
    fun `toAscii_should_convert_halfwidth_ideographic_full_stop`() {
        // U+FF61 HALFWIDTH IDEOGRAPHIC FULL STOP
        assertEquals("example.com", HostCanonicalizer.toAscii("example｡com"))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub｡domain｡com"))
        assertEquals("example.com", HostCanonicalizer.toAscii("example｡com｡"))
    }

    @Test
    fun `toAscii_should_handle_mixed_dot_types`() {
        assertEquals("example.com", HostCanonicalizer.toAscii("example。com．"))
        assertEquals("sub.domain.com", HostCanonicalizer.toAscii("sub｡domain。com．"))
        assertEquals("complex.mixed.domain", HostCanonicalizer.toAscii("complex。mixed．domain｡"))
    }

    @Test
    fun `toAscii_should_handle_internationalized_domain_names`() {
        // German umlaut domain
        assertEquals("xn--bcher-kva.com", HostCanonicalizer.toAscii("bücher.com"))
        
        // Japanese domain
        assertEquals("xn--wgv71a.com", HostCanonicalizer.toAscii("日本.com"))
        
        // Arabic domain
        assertEquals("xn--mgbcd4a2b0d2b.com", HostCanonicalizer.toAscii("العربية.com"))
        
        // Chinese domain
        assertEquals("xn--fiqs8s.com", HostCanonicalizer.toAscii("中国.com"))
    }

    @Test
    fun `toAscii_should_handle_mixed_ascii_and_unicode`() {
        assertEquals("xn--bcher-kva.example.com", HostCanonicalizer.toAscii("bücher.example.com"))
        assertEquals("sub.xn--wgv71a.com", HostCanonicalizer.toAscii("sub.日本.com"))
        assertEquals("xn--mgbcd4a2b0d2b.xn--wgv71a.com", HostCanonicalizer.toAscii("العربية.日本.com"))
    }

    @Test
    fun `toAscii_should_handle_punycode_domains`() {
        // Already encoded domains should remain the same
        assertEquals("xn--bcher-kva.com", HostCanonicalizer.toAscii("xn--bcher-kva.com"))
        assertEquals("xn--wgv71a.com", HostCanonicalizer.toAscii("xn--wgv71a.com"))
    }

    @Test
    fun `toAscii_should_handle_case_insensitive_unicode`() {
        // Unicode characters don't have case, but test mixed case ASCII parts
        assertEquals("xn--bcher-kva.example.com", HostCanonicalizer.toAscii("bücher.EXAMPLE.COM"))
        assertEquals("sub.xn--wgv71a.com", HostCanonicalizer.toAscii("SUB.日本.COM"))
    }

    @Test
    fun `toAscii_should_handle_single_character_domains`() {
        assertEquals("a.com", HostCanonicalizer.toAscii("a.com"))
        assertEquals("x.y.z", HostCanonicalizer.toAscii("x.y.z"))
    }

    @Test
    fun `toAscii_should_handle_numeric_domains`() {
        assertEquals("123.456.com", HostCanonicalizer.toAscii("123.456.com"))
        assertEquals("1.2.3.4", HostCanonicalizer.toAscii("1.2.3.4"))
    }

    @Test
    fun `toAscii_should_handle_hyphenated_domains`() {
        assertEquals("sub-domain.example-site.com", HostCanonicalizer.toAscii("sub-domain.example-site.com"))
        assertEquals("multi-word-domain.co.uk", HostCanonicalizer.toAscii("MULTI-WORD-DOMAIN.CO.UK"))
    }

    @Test
    fun `toAscii_should_handle_very_long_domains`() {
        val longLabel = "a".repeat(63) // Maximum label length
        val longDomain = "$longLabel.com"
        assertEquals(longDomain.lowercase(), HostCanonicalizer.toAscii(longDomain.uppercase()))
    }

    @Test
    fun `toAscii_should_handle_invalid_domains_gracefully`() {
        // IDN.toASCII should handle these gracefully and return null for invalid input
        
        // Test with invalid characters that might cause IDN.toASCII to throw
        assertNull(HostCanonicalizer.toAscii("invalid..domain"))
        assertNull(HostCanonicalizer.toAscii("domain-.com"))
        assertNull(HostCanonicalizer.toAscii("-domain.com"))
        
        // Test with extremely long labels (over 63 characters)
        val tooLongLabel = "a".repeat(64)
        assertNull(HostCanonicalizer.toAscii("$tooLongLabel.com"))
    }

    @Test
    fun `toAscii_should_handle_whitespace_domains`() {
        // Domains with whitespace should be handled gracefully
        assertNull(HostCanonicalizer.toAscii("example .com"))
        assertNull(HostCanonicalizer.toAscii(" example.com"))
        assertNull(HostCanonicalizer.toAscii("example.com "))
        assertNull(HostCanonicalizer.toAscii("exam ple.com"))
    }

    @Test
    fun `toAscii_should_handle_special_characters`() {
        // Test various special characters that might cause issues
        assertNull(HostCanonicalizer.toAscii("example@.com"))
        assertNull(HostCanonicalizer.toAscii("example#.com"))
        assertNull(HostCanonicalizer.toAscii("example$.com"))
        assertNull(HostCanonicalizer.toAscii("example%.com"))
    }

    @Test
    fun `toAscii_should_handle_emoji_domains`() {
        // Some emoji might be valid in domain names
        val emojiDomain = "😀.com"
        val result = HostCanonicalizer.toAscii(emojiDomain)
        // Result could be null or a punycode representation
        // The important thing is it doesn't crash
        assertTrue("Should handle emoji domains without crashing", result == null || result.startsWith("xn--"))
    }

    @Test
    fun `toAscii_should_handle_rtl_domains`() {
        // Right-to-left script domains
        val arabicDomain = "مثال.com"
        val result = HostCanonicalizer.toAscii(arabicDomain)
        assertNotNull("Arabic domain should be converted", result)
        assertTrue("Should be punycode", result!!.contains("xn--"))
    }

    @Test
    fun `toAscii_should_handle_mixed_scripts`() {
        // Mixed script domains (might be invalid according to IDN rules)
        val mixedScript = "example中文.com"
        val result = HostCanonicalizer.toAscii(mixedScript)
        // Could be null due to mixed script restrictions
        assertTrue("Mixed script handling should not crash", result == null || result.contains("xn--"))
    }

    @Test
    fun `toAscii_should_preserve_ip_addresses`() {
        // IPv4 addresses should pass through unchanged
        assertEquals("192.168.1.1", HostCanonicalizer.toAscii("192.168.1.1"))
        assertEquals("127.0.0.1", HostCanonicalizer.toAscii("127.0.0.1"))
        assertEquals("10.0.0.1", HostCanonicalizer.toAscii("10.0.0.1"))
    }

    @Test
    fun `toAscii_should_handle_localhost`() {
        assertEquals("localhost", HostCanonicalizer.toAscii("localhost"))
        assertEquals("localhost", HostCanonicalizer.toAscii("LOCALHOST"))
        assertEquals("localhost", HostCanonicalizer.toAscii("LocalHost"))
    }

    @Test
    fun `toAscii_should_handle_edge_case_dots`() {
        // Test various dot-related edge cases
        assertEquals("", HostCanonicalizer.toAscii("."))
        assertEquals("", HostCanonicalizer.toAscii(".."))
        assertEquals("", HostCanonicalizer.toAscii("..."))
        
        // Leading dots
        assertNull(HostCanonicalizer.toAscii(".example.com"))
        assertNull(HostCanonicalizer.toAscii("..example.com"))
    }

    @Test
    fun `toAscii_should_handle_unicode_normalization`() {
        // Test Unicode normalization - same character represented differently
        val composed = "café.com" // é as single character
        val decomposed = "cafe\u0301.com" // e + combining acute accent
        
        val result1 = HostCanonicalizer.toAscii(composed)
        val result2 = HostCanonicalizer.toAscii(decomposed)
        
        // Both should normalize to the same result
        assertNotNull("Composed form should be handled", result1)
        assertNotNull("Decomposed form should be handled", result2)
        assertEquals("Unicode normalization should produce same result", result1, result2)
    }

    @Test
    fun `toAscii_should_handle_zero_width_characters`() {
        // Zero-width characters that might be used in attacks
        val domainWithZWS = "exam\u200Bple.com" // Zero-width space
        val domainWithZWNJ = "exam\u200Cple.com" // Zero-width non-joiner
        
        // These should either be handled gracefully or rejected
        val result1 = HostCanonicalizer.toAscii(domainWithZWS)
        val result2 = HostCanonicalizer.toAscii(domainWithZWNJ)
        
        // The important thing is no crash occurs
        assertTrue("Zero-width space handling should not crash", result1 == null || result1.isNotEmpty())
        assertTrue("Zero-width non-joiner handling should not crash", result2 == null || result2.isNotEmpty())
    }
}
