package com.gologlu.detracktor.domain.model

import org.junit.Test
import org.junit.Assert.*

class QueryTokenTest {

    @Test
    fun `percentDecodeUtf8 preserves ASCII characters`() {
        val token = QueryToken("hello", true, "world")
        assertEquals("hello", token.decodedKey)
        assertEquals("world", token.decodedValue)
    }

    @Test
    fun `percentDecodeUtf8 preserves non-ASCII characters without corruption`() {
        // Test various non-ASCII characters that should NOT be corrupted
        val testCases = mapOf(
            "café" to "café",
            "naïve" to "naïve", 
            "résumé" to "résumé",
            "Москва" to "Москва", // Russian
            "東京" to "東京", // Japanese
            "🚀" to "🚀", // Emoji
            "ö" to "ö", // The specific case mentioned in the issue
            "ü" to "ü",
            "ñ" to "ñ"
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `percentDecodeUtf8 handles valid percent sequences`() {
        val testCases = mapOf(
            "hello%20world" to "hello world", // Space
            "%21" to "!", // Exclamation
            "%3D" to "=", // Equals
            "%26" to "&", // Ampersand
            "%2B" to "+", // Plus
            "%2F" to "/", // Forward slash
            "%3F" to "?", // Question mark
            "%23" to "#"  // Hash
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `percentDecodeUtf8 handles multi-byte UTF-8 sequences`() {
        val testCases = mapOf(
            // UTF-8 encoded sequences
            "%C3%A9" to "é", // é (U+00E9) encoded as UTF-8
            "%C3%B6" to "ö", // ö (U+00F6) encoded as UTF-8  
            "%E2%82%AC" to "€", // € (U+20AC) encoded as UTF-8
            "%F0%9F%9A%80" to "🚀", // 🚀 (U+1F680) encoded as UTF-8
            "%E6%9D%B1%E4%BA%AC" to "東京" // 東京 encoded as UTF-8
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `percentDecodeUtf8 handles mixed literal and encoded content`() {
        val testCases = mapOf(
            "café%20bar" to "café bar", // Literal non-ASCII + encoded space
            "hello%20ö" to "hello ö", // Encoded space + literal non-ASCII
            "%C3%A9café" to "écafé", // Encoded + literal non-ASCII
            "test%21ö%3D" to "test!ö=", // Mixed encoded and literal
            "🚀%20rocket" to "🚀 rocket" // Emoji + encoded space
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `percentDecodeUtf8 handles invalid percent sequences gracefully`() {
        val testCases = mapOf(
            "%GZ" to "%GZ", // Invalid hex digits
            "%1" to "%1", // Incomplete sequence
            "%" to "%", // Lone percent at end
            "%1G" to "%1G", // One valid, one invalid hex digit
            "test%ZZ" to "test%ZZ", // Invalid sequence in middle
            "%20%GZ%21" to " %GZ!" // Mix of valid and invalid
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `percentDecodeUtf8 handles contiguous percent sequences correctly`() {
        val testCases = mapOf(
            "%20%21%22" to " !\"", // Multiple single-byte sequences
            "%C3%A9%C3%B6" to "éö", // Multiple multi-byte sequences
            "%20%C3%A9%21" to " é!" // Mix of single and multi-byte
        )
        
        testCases.forEach { (input, expected) ->
            val token = QueryToken(input, true, input)
            assertEquals(expected, token.decodedKey)
            assertEquals(expected, token.decodedValue)
        }
    }

    @Test
    fun `asString preserves original format`() {
        val testCases = listOf(
            QueryToken("flag", false, "") to "flag",
            QueryToken("flag", true, "") to "flag=",
            QueryToken("key", true, "value") to "key=value",
            QueryToken("", true, "value") to "=value",
            QueryToken("", true, "") to "=",
            QueryToken("key%20name", true, "value%20data") to "key%20name=value%20data"
        )
        
        testCases.forEach { (token, expected) ->
            assertEquals(expected, token.asString())
        }
    }

    @Test
    fun `lazy evaluation works correctly`() {
        val token = QueryToken("hello%20world", true, "test%21")
        
        // First access should trigger computation
        assertEquals("hello world", token.decodedKey)
        assertEquals("test!", token.decodedValue)
        
        // Subsequent accesses should use cached values
        assertEquals("hello world", token.decodedKey)
        assertEquals("test!", token.decodedValue)
    }

    @Test
    fun `error handling in decodedKeyOrRaw and decodedValueOrRaw`() {
        // These methods should never actually throw since our implementation
        // handles all edge cases gracefully, but test the fallback behavior
        val token = QueryToken("normal", true, "normal")
        
        assertEquals("normal", token.decodedKeyOrRaw())
        assertEquals("normal", token.decodedValueOrRaw())
    }
}
