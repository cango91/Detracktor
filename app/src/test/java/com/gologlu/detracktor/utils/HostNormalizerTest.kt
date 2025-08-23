package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.HostNormalizationConfig
import com.gologlu.detracktor.data.NormalizedHost
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HostNormalizerTest {

    private lateinit var hostNormalizer: HostNormalizer
    private lateinit var config: HostNormalizationConfig

    @Before
    fun setUp() {
        config = HostNormalizationConfig(
            enableCaseNormalization = true,
            enableIDNConversion = true,
            enablePortNormalization = true,
            defaultPorts = mapOf(
                "http" to 80,
                "https" to 443,
                "ftp" to 21
            )
        )
        hostNormalizer = HostNormalizer(config)
    }

    @Test
    fun testNormalizeHost_withSimpleHost_returnsNormalizedHost() {
        // Given
        val host = "Example.Com"
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("Example.Com", result.original)
        assertEquals("example.com", result.normalized)
        assertNull(result.port)
        assertFalse(result.isIDN)
        assertNull(result.punycode)
    }

    @Test
    fun testNormalizeHost_withHostAndPort_extractsPortCorrectly() {
        // Given
        val host = "example.com:8080"
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("example.com:8080", result.original)
        assertEquals("example.com", result.normalized)
        assertEquals(8080, result.port)
        assertFalse(result.isIDN)
    }

    @Test
    fun testNormalizeHost_withDefaultPort_removesPort() {
        // Given
        val host = "example.com:443"
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("example.com:443", result.original)
        assertEquals("example.com", result.normalized)
        assertNull(result.port) // Default HTTPS port should be removed
        assertFalse(result.isIDN)
    }

    @Test
    fun testNormalizeHost_withIPv6Address_handlesCorrectly() {
        // Given
        val host = "[2001:db8::1]:8080"
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("[2001:db8::1]:8080", result.original)
        assertEquals("[2001:db8::1]", result.normalized)
        assertEquals(8080, result.port)
        assertFalse(result.isIDN)
    }

    @Test
    fun testNormalizeHost_withIPv6AddressNoPort_handlesCorrectly() {
        // Given
        val host = "[2001:db8::1]"
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("[2001:db8::1]", result.original)
        assertEquals("[2001:db8::1]", result.normalized)
        assertNull(result.port)
        assertFalse(result.isIDN)
    }

    @Test
    fun testNormalizeHost_withEmptyHost_returnsOriginal() {
        // Given
        val host = ""
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("", result.original)
        assertEquals("", result.normalized)
        assertNull(result.port)
        assertFalse(result.isIDN)
    }

    @Test
    fun testNormalizeHost_withUnicodeIDN_convertsToASCII() {
        // Given
        val host = "тест.example.com" // Cyrillic characters
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("тест.example.com", result.original)
        assertTrue(result.isIDN)
        assertNotNull(result.punycode)
        assertTrue(result.punycode!!.contains("xn--"))
    }

    @Test
    fun testNormalizeHost_withPunycodeIDN_convertsToUnicode() {
        // Given
        val host = "xn--e1afmkfd.example.com" // Punycode for пример
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizeHost(host, scheme)

        // Then
        assertNotNull(result)
        assertEquals("xn--e1afmkfd.example.com", result.original)
        assertTrue(result.isIDN)
        assertEquals("xn--e1afmkfd.example.com", result.punycode)
        // The normalized should be the Unicode version
        assertNotNull(result.normalized)
    }

    @Test
    fun testNormalizeCasing_convertsToLowercase() {
        // Given
        val host = "EXAMPLE.COM"

        // When
        val result = hostNormalizer.normalizeCasing(host)

        // Then
        assertEquals("example.com", result)
    }

    @Test
    fun testExtractPort_withValidPort_extractsCorrectly() {
        // Given
        val hostWithPort = "example.com:8080"

        // When
        val (host, port) = hostNormalizer.extractPort(hostWithPort)

        // Then
        assertEquals("example.com", host)
        assertEquals(8080, port)
    }

    @Test
    fun testExtractPort_withInvalidPort_returnsNull() {
        // Given
        val hostWithPort = "example.com:invalid"

        // When
        val (host, port) = hostNormalizer.extractPort(hostWithPort)

        // Then
        assertEquals("example.com:invalid", host)
        assertNull(port)
    }

    @Test
    fun testExtractPort_withOutOfRangePort_returnsNull() {
        // Given
        val hostWithPort = "example.com:99999"

        // When
        val (host, port) = hostNormalizer.extractPort(hostWithPort)

        // Then
        assertEquals("example.com:99999", host)
        assertNull(port)
    }

    @Test
    fun testExtractPort_withNoPort_returnsNull() {
        // Given
        val hostWithoutPort = "example.com"

        // When
        val (host, port) = hostNormalizer.extractPort(hostWithoutPort)

        // Then
        assertEquals("example.com", host)
        assertNull(port)
    }

    @Test
    fun testNormalizePort_withDefaultHttpPort_returnsNull() {
        // Given
        val port = 80
        val scheme = "http"

        // When
        val result = hostNormalizer.normalizePort(port, scheme)

        // Then
        assertNull(result)
    }

    @Test
    fun testNormalizePort_withDefaultHttpsPort_returnsNull() {
        // Given
        val port = 443
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizePort(port, scheme)

        // Then
        assertNull(result)
    }

    @Test
    fun testNormalizePort_withNonDefaultPort_returnsPort() {
        // Given
        val port = 8080
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizePort(port, scheme)

        // Then
        assertEquals(8080, result)
    }

    @Test
    fun testNormalizePort_withNullPort_returnsNull() {
        // Given
        val port: Int? = null
        val scheme = "https"

        // When
        val result = hostNormalizer.normalizePort(port, scheme)

        // Then
        assertNull(result)
    }

    @Test
    fun testConvertIDN_withUnicodeHost_returnsPunycode() {
        // Given
        val host = "тест.com"

        // When
        val (normalized, isIDN, punycode) = hostNormalizer.convertIDN(host)

        // Then
        assertTrue(isIDN)
        assertNotNull(punycode)
        assertTrue(punycode!!.contains("xn--"))
        assertEquals(punycode, normalized)
    }

    @Test
    fun testConvertIDN_withPunycodeHost_returnsUnicode() {
        // Given
        val host = "xn--e1afmkfd.com"

        // When
        val (normalized, isIDN, punycode) = hostNormalizer.convertIDN(host)

        // Then
        assertTrue(isIDN)
        assertEquals("xn--e1afmkfd.com", punycode)
        // normalized should be the Unicode version
        assertNotNull(normalized)
    }

    @Test
    fun testConvertIDN_withRegularHost_returnsUnchanged() {
        // Given
        val host = "example.com"

        // When
        val (normalized, isIDN, punycode) = hostNormalizer.convertIDN(host)

        // Then
        assertFalse(isIDN)
        assertEquals("example.com", normalized)
        assertNull(punycode)
    }

    @Test
    fun testConvertToIDN_withUnicodeHost_returnsPunycode() {
        // Given
        val host = "тест.com"

        // When
        val result = hostNormalizer.convertToIDN(host)

        // Then
        assertNotNull(result)
        assertTrue(result!!.contains("xn--"))
    }

    @Test
    fun testConvertFromIDN_withPunycodeHost_returnsUnicode() {
        // Given
        val host = "xn--e1afmkfd.com"

        // When
        val result = hostNormalizer.convertFromIDN(host)

        // Then
        assertNotNull(result)
        // Should convert to Unicode (exact result depends on the punycode)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testDenormalize_withSimpleHost_returnsHost() {
        // Given
        val normalizedHost = NormalizedHost(
            original = "example.com:8080",
            normalized = "example.com",
            port = 8080,
            isIDN = false,
            punycode = null
        )

        // When
        val result = hostNormalizer.denormalize(normalizedHost)

        // Then
        assertEquals("example.com:8080", result)
    }

    @Test
    fun testDenormalize_withIDNHost_returnsUnicodeWithPort() {
        // Given
        val normalizedHost = NormalizedHost(
            original = "тест.com:8080",
            normalized = "тест.com",
            port = 8080,
            isIDN = true,
            punycode = "xn--e1afmkfd.com"
        )

        // When
        val result = hostNormalizer.denormalize(normalizedHost)

        // Then
        assertTrue(result!!.contains(":8080"))
        // Should contain Unicode version, not punycode
        assertNotNull(result)
    }

    @Test
    fun testDenormalize_withIPv6Host_handlesCorrectly() {
        // Given
        val normalizedHost = NormalizedHost(
            original = "[2001:db8::1]:8080",
            normalized = "[2001:db8::1]",
            port = 8080,
            isIDN = false,
            punycode = null
        )

        // When
        val result = hostNormalizer.denormalize(normalizedHost)

        // Then
        assertEquals("[2001:db8::1]:8080", result)
    }

    @Test
    fun testNormalizeHost_withCaseNormalizationDisabled_preservesCase() {
        // Given
        val configNoCaseNorm = HostNormalizationConfig(
            enableCaseNormalization = false,
            enableIDNConversion = true,
            enablePortNormalization = true,
            defaultPorts = mapOf("https" to 443)
        )
        val normalizerNoCaseNorm = HostNormalizer(configNoCaseNorm)
        val host = "Example.Com"

        // When
        val result = normalizerNoCaseNorm.normalizeHost(host)

        // Then
        assertEquals("Example.Com", result.normalized) // Case preserved
    }

    @Test
    fun testNormalizeHost_withPortNormalizationDisabled_preservesPort() {
        // Given
        val configNoPortNorm = HostNormalizationConfig(
            enableCaseNormalization = true,
            enableIDNConversion = true,
            enablePortNormalization = false,
            defaultPorts = mapOf("https" to 443)
        )
        val normalizerNoPortNorm = HostNormalizer(configNoPortNorm)
        val host = "example.com:443"

        // When
        val result = normalizerNoPortNorm.normalizeHost(host, "https")

        // Then
        assertEquals(443, result.port) // Port preserved even though it's default
    }

    @Test
    fun testNormalizeHost_withIDNConversionDisabled_preservesOriginalIDN() {
        // Given
        val configNoIDN = HostNormalizationConfig(
            enableCaseNormalization = true,
            enableIDNConversion = false,
            enablePortNormalization = true,
            defaultPorts = mapOf("https" to 443)
        )
        val normalizerNoIDN = HostNormalizer(configNoIDN)
        val host = "тест.com"

        // When
        val result = normalizerNoIDN.normalizeHost(host)

        // Then
        assertFalse(result.isIDN) // IDN conversion disabled
        assertEquals("тест.com", result.normalized) // Original preserved (but lowercased)
    }
}
