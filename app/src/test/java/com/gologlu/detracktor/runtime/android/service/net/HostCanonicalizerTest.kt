package com.gologlu.detracktor.runtime.android.service.net

import com.gologlu.detracktor.runtime.android.test.TestData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Test class for HostCanonicalizer.
 * Tests host canonicalization with international domain support using real implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class HostCanonicalizerTest {
    
    @Test
    fun `toAscii handles simple ASCII domains correctly`() {
        // Given
        val testCases = listOf(
            "example.com" to "example.com",
            "test.org" to "test.org",
            "subdomain.example.com" to "subdomain.example.com",
            "localhost" to "localhost"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for input: $input")
        }
    }
    
    @Test
    fun `toAscii handles uppercase domains correctly`() {
        // Given
        val testCases = listOf(
            "EXAMPLE.COM" to "example.com",
            "Test.ORG" to "test.org",
            "SUBDOMAIN.EXAMPLE.COM" to "subdomain.example.com",
            "MiXeD.CaSe.CoM" to "mixed.case.com"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for input: $input")
        }
    }
    
    @Test
    fun `toAscii handles international domain names correctly`() {
        // Use test data from TestData.HostData
        val testCases = TestData.HostData.getHostCanonicalizationCases()
        
        testCases.forEach { testCase ->
            // When
            val result = HostCanonicalizer.toAscii(testCase.input)
            
            // Then
            assertEquals(testCase.expected, result, "Failed for ${testCase.description}: ${testCase.input}")
        }
    }
    
    @Test
    fun `toAscii handles Japanese domains correctly`() {
        // Given
        val testCases = listOf(
            "例え.テスト" to "xn--r8jz45g.xn--zckzah",
            "日本.jp" to "xn--wgv71a.jp", // Corrected based on actual Java IDN output
            "テスト.example.com" to "xn--zckzah.example.com"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for Japanese domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles German domains with umlauts correctly`() {
        // Given
        val testCases = listOf(
            "münchen.de" to "xn--mnchen-3ya.de",
            "bücher.example.com" to "xn--bcher-kva.example.com",
            "straße.de" to "strasse.de" // ß converts to ss, not punycode
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for German domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles Cyrillic domains correctly`() {
        // Given
        val testCases = listOf(
            "россия.рф" to "xn--h1alffa9f.xn--p1ai",
            "тест.example.com" to "xn--e1aybc.example.com",
            "москва.рф" to "xn--80adxhks.xn--p1ai"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for Cyrillic domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles Arabic domains correctly`() {
        // Given
        val testCases = listOf(
            "مثال.إختبار" to "xn--mgbh0fb.xn--kgbechtv",
            "العربية.example.com" to "xn--mgbcd4a2b0d2b.example.com" // Corrected based on actual Java IDN output
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for Arabic domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles Chinese domains correctly`() {
        // Given
        val testCases = listOf(
            "测试.中国" to "xn--0zwm56d.xn--fiqs8s",
            "例子.example.com" to "xn--fsqu00a.example.com",
            "中文.测试" to "xn--fiq228c.xn--0zwm56d"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for Chinese domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles emoji domains correctly`() {
        // Given
        val testCases = listOf(
            "🌟.example" to "xn--ch8h.example", // Corrected based on actual Java IDN output
            "💻.test.com" to "xn--3s8h.test.com", // Corrected based on actual Python IDN output
            "🚀.space" to "xn--158h.space" // Corrected based on actual Python IDN output
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for emoji domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles mixed script domains correctly`() {
        // Given
        val testCases = listOf(
            "test-例え.com" to "xn--test--c63dw88r.com", // Corrected based on actual Python IDN output
            "example-тест.org" to "xn--example--j8g4jjc.org", // Corrected based on actual Python IDN output
            "mixed-测试.net" to "xn--mixed--nf3nq86q.net" // Corrected based on actual Python IDN output
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for mixed script domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles subdomains with international characters correctly`() {
        // Given
        val testCases = listOf(
            "www.例え.テスト" to "www.xn--r8jz45g.xn--zckzah",
            "api.münchen.de" to "api.xn--mnchen-3ya.de",
            "mail.россия.рф" to "mail.xn--h1alffa9f.xn--p1ai",
            "测试.中文.example.com" to "xn--0zwm56d.xn--fiq228c.example.com"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for subdomain: $input")
        }
    }
    
    @Test
    fun `toAscii handles edge cases correctly`() {
        // Given
        val testCases = listOf(
            "" to null, // Empty string returns null immediately
            "localhost" to "localhost",
            "127.0.0.1" to "127.0.0.1",
            "::1" to "::1",
            "example.com." to "example.com", // Trailing dot gets removed
            ".example.com" to null // Leading dot is invalid
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for edge case: '$input'")
        }
    }
    
    @Test
    fun `toAscii handles already punycode domains correctly`() {
        // Given - domains that are already in punycode format
        val testCases = listOf(
            "xn--r8jz45g.xn--zckzah" to "xn--r8jz45g.xn--zckzah",
            "xn--mnchen-3ya.de" to "xn--mnchen-3ya.de",
            "xn--h1alffa9f.xn--p1ai" to "xn--h1alffa9f.xn--p1ai"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for already punycode domain: $input")
        }
    }
    
    @Test
    fun `toAscii handles complex real-world domains correctly`() {
        // Given - real-world international domains
        val testCases = listOf(
            // Real international TLDs
            "example.中国" to "example.xn--fiqs8s",
            "test.рф" to "test.xn--p1ai",
            "sample.みんな" to "sample.xn--q9jyb4c",
            
            // Real international domains with subdomains
            "www.президент.рф" to "www.xn--d1abbgf6aiiy.xn--p1ai",
            "mail.правительство.рф" to "mail.xn--80aealotwbjpid2k.xn--p1ai"
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed for real-world domain: $input")
        }
    }
    
    @Test
    fun `toAscii is consistent across multiple calls`() {
        // Given
        val testInputs = listOf(
            "例え.テスト",
            "münchen.de",
            "россия.рф",
            "测试.中国",
            "🌟.example"
        )
        
        testInputs.forEach { input ->
            // When - call multiple times
            val result1 = HostCanonicalizer.toAscii(input)
            val result2 = HostCanonicalizer.toAscii(input)
            val result3 = HostCanonicalizer.toAscii(input)
            
            // Then - results should be consistent
            assertEquals(result1, result2, "Inconsistent results for: $input")
            assertEquals(result2, result3, "Inconsistent results for: $input")
        }
    }
    
    @Test
    fun `toAscii handles normalization correctly`() {
        // Given - test Unicode normalization
        val testCases = listOf(
            // These should normalize to the same result
            "café.com" to "xn--caf-dma.com",
            "cafe\u0301.com" to "xn--caf-dma.com" // Using combining accent
        )
        
        testCases.forEach { (input, expected) ->
            // When
            val result = HostCanonicalizer.toAscii(input)
            
            // Then
            assertEquals(expected, result, "Failed normalization for: $input")
        }
    }
    
    @Test
    fun `toAscii performance is acceptable for batch operations`() {
        // Given - large batch of international domains
        val testDomains = TestData.HostData.getHostCanonicalizationCases().map { it.input } +
                listOf(
                    "例え.テスト", "münchen.de", "россия.рф", "测试.中国",
                    "🌟.example", "café.com", "test.org", "example.com"
                )
        
        // When - process batch
        val startTime = System.currentTimeMillis()
        val results = testDomains.map { HostCanonicalizer.toAscii(it) }
        val endTime = System.currentTimeMillis()
        
        // Then - should complete in reasonable time
        val duration = endTime - startTime
        assertEquals(testDomains.size, results.size)
        // Performance assertion - should complete batch in under 1 second
        assert(duration < 1000) { "Batch processing took too long: ${duration}ms" }
    }
}
