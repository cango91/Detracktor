package com.gologlu.detracktor.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

class QueryPairsTest {

    // Helper function to create simple glob-like predicates for testing
    private fun createGlobPredicate(pattern: String, caseSensitive: Boolean = false): (String) -> Boolean {
        return { input ->
            val inputToMatch = if (caseSensitive) input else input.lowercase(Locale.ROOT)
            val patternToMatch = if (caseSensitive) pattern else pattern.lowercase(Locale.ROOT)
            
            when {
                patternToMatch == "*" -> true
                patternToMatch.endsWith("*") -> inputToMatch.startsWith(patternToMatch.dropLast(1))
                patternToMatch.startsWith("*") -> inputToMatch.endsWith(patternToMatch.drop(1))
                else -> patternToMatch == inputToMatch
            }
        }
    }

    @Test
    fun `from should preserve exact order of parameters`() {
        val queryPairs = QueryPairs.from("c=3&a=1&b=2&a=4")
        
        assertEquals("c=3&a=1&b=2&a=4", queryPairs.asString())
    }

    @Test
    fun `getAll should return values in order of appearance`() {
        val queryPairs = QueryPairs.from("a=first&b=other&a=second&a=third")
        
        assertEquals(listOf("first", "second", "third"), queryPairs.getAll("a"))
        assertEquals(listOf("other"), queryPairs.getAll("b"))
        assertEquals(emptyList<String>(), queryPairs.getAll("nonexistent"))
    }

    @Test
    fun `getFirst should return first occurrence`() {
        val queryPairs = QueryPairs.from("a=first&a=second&b=only")
        
        assertEquals("first", queryPairs.getFirst("a"))
        assertEquals("only", queryPairs.getFirst("b"))
        assertNull(queryPairs.getFirst("nonexistent"))
    }

    @Test
    fun `add should append to end`() {
        val queryPairs = QueryPairs.from("a=1&b=2")
            .add("c", "3")
            .add("a", "4")
        
        assertEquals("a=1&b=2&c=3&a=4", queryPairs.asString())
    }

    @Test
    fun `remove should remove all occurrences of parameter name`() {
        val queryPairs = QueryPairs.from("a=1&b=2&a=3&c=4")
            .remove("a")
        
        assertEquals("b=2&c=4", queryPairs.asString())
    }

    @Test
    fun `removeWhere should remove parameters matching predicate`() {
        val queryPairs = QueryPairs.from("keep1=1&remove1=2&keep2=3&remove2=4")
            .removeWhere { it.startsWith("remove") }
        
        assertEquals("keep1=1&keep2=3", queryPairs.asString())
    }

    @Test
    fun `removeAnyOf with predicates should work correctly`() {
        val queryPairs = QueryPairs.from("test1=1&keep=2&test2=3&other=4")
        val predicates = listOf(createGlobPredicate("test*"))
        
        val result = queryPairs.removeAnyOf(predicates)
        
        assertEquals("keep=2&other=4", result.asString())
    }

    @Test
    fun `toQueryMap should convert correctly`() {
        val queryPairs = QueryPairs.from("a=1&b=2&a=3")
        val queryMap = queryPairs.toQueryMap()
        
        assertEquals(listOf("1", "3"), queryMap.get("a"))
        assertEquals(listOf("2"), queryMap.get("b"))
    }

    @Test
    fun `empty should create empty QueryPairs`() {
        val queryPairs = QueryPairs.empty()
        
        assertTrue(queryPairs.isEmpty())
        assertEquals(0, queryPairs.size())
        assertEquals("", queryPairs.asString())
    }

    @Test
    fun `of should create QueryPairs from vararg pairs`() {
        val queryPairs = QueryPairs.of("a" to "1", "b" to "2", "a" to "3")
        
        assertEquals("a=1&b=2&a=3", queryPairs.asString())
        assertEquals(3, queryPairs.size())
    }

    @Test
    fun `from should handle null input`() {
        val queryPairs = QueryPairs.from(null)
        
        assertTrue(queryPairs.isEmpty())
        assertEquals("", queryPairs.asString())
    }

    @Test
    fun `from should handle empty string`() {
        val queryPairs = QueryPairs.from("")
        
        assertTrue(queryPairs.isEmpty())
        assertEquals("", queryPairs.asString())
    }

    @Test
    fun `from should handle empty values`() {
        val queryPairs = QueryPairs.from("a=&b=value&c=")
        
        assertEquals("a=&b=value&c=", queryPairs.asString())
        assertEquals(listOf("", ""), queryPairs.getAll("a").plus(queryPairs.getAll("c")))
    }

    @Test
    fun `round trip should preserve exact order and duplicates`() {
        val original = "z=last&a=first&a=second&b=middle&a=third"
        val queryPairs = QueryPairs.from(original)
        val roundTrip = queryPairs.asString()
        
        assertEquals("Round trip should preserve exact order", original, roundTrip)
    }

    // New tests for QueryToken functionality and wire format preservation

    @Test
    fun `from should preserve hasEquals flag for different formats`() {
        val queryPairs = QueryPairs.from("flag&key=&value=data")
        val tokens = queryPairs.getTokens()
        
        assertEquals(3, tokens.size)
        
        // "flag" - no equals sign
        assertEquals("flag", tokens[0].rawKey)
        assertEquals(false, tokens[0].hasEquals)
        assertEquals("", tokens[0].rawValue)
        
        // "key=" - has equals sign but empty value
        assertEquals("key", tokens[1].rawKey)
        assertEquals(true, tokens[1].hasEquals)
        assertEquals("", tokens[1].rawValue)
        
        // "value=data" - has equals sign and value
        assertEquals("value", tokens[2].rawKey)
        assertEquals(true, tokens[2].hasEquals)
        assertEquals("data", tokens[2].rawValue)
    }

    @Test
    fun `asString should preserve exact wire format`() {
        // Test various combinations that should round-trip exactly
        val testCases = listOf(
            "flag",
            "flag=",
            "key=value",
            "flag&key=&value=data",
            "a=1&flag&b=2&empty=&c=3",
            "encoded%20key=encoded%20value"
        )
        
        testCases.forEach { original ->
            val queryPairs = QueryPairs.from(original)
            val roundTrip = queryPairs.asString()
            assertEquals("Failed round-trip for: $original", original, roundTrip)
        }
    }

    @Test
    fun `QueryToken should decode keys and values correctly`() {
        val queryPairs = QueryPairs.from("encoded%20key=encoded%20value&normal=data")
        val tokens = queryPairs.getTokens()
        
        assertEquals(2, tokens.size)
        
        // First token - encoded
        assertEquals("encoded%20key", tokens[0].rawKey)
        assertEquals("encoded%20value", tokens[0].rawValue)
        assertEquals("encoded key", tokens[0].decodedKeyOrRaw())
        assertEquals("encoded value", tokens[0].decodedValueOrRaw())
        
        // Second token - normal
        assertEquals("normal", tokens[1].rawKey)
        assertEquals("data", tokens[1].rawValue)
        assertEquals("normal", tokens[1].decodedKeyOrRaw())
        assertEquals("data", tokens[1].decodedValueOrRaw())
    }

    @Test
    fun `getAll and getFirst should work with decoded keys`() {
        val queryPairs = QueryPairs.from("encoded%20key=value1&encoded%20key=value2&other=value3")
        
        // Should match using decoded key
        assertEquals(listOf("value1", "value2"), queryPairs.getAll("encoded key"))
        assertEquals("value1", queryPairs.getFirst("encoded key"))
        
        // Should not match using raw key
        assertEquals(emptyList<String>(), queryPairs.getAll("encoded%20key"))
        assertNull(queryPairs.getFirst("encoded%20key"))
    }

    @Test
    fun `remove should work with decoded keys but preserve raw format`() {
        val queryPairs = QueryPairs.from("encoded%20key=value1&other=value2&encoded%20key=value3")
            .remove("encoded key") // Remove using decoded key
        
        assertEquals("other=value2", queryPairs.asString())
    }

    @Test
    fun `removeWhereDecoded should work with decoded keys`() {
        val queryPairs = QueryPairs.from("encoded%20space=1&normal=2&another%20space=3")
            .removeWhereDecoded { it.contains(" ") } // Remove keys containing space
        
        assertEquals("normal=2", queryPairs.asString())
    }

    @Test
    fun `predicate matching should work with case sensitivity`() {
        val queryPairs = QueryPairs.from("Test=1&test=2&OTHER=3&other=4")
        
        // Case insensitive predicate (default)
        val insensitivePredicate = createGlobPredicate("test*", caseSensitive = false)
        val resultInsensitive = queryPairs.removeWhere(insensitivePredicate)
        assertEquals("OTHER=3&other=4", resultInsensitive.asString())
        
        // Case sensitive predicate
        val sensitivePredicate = createGlobPredicate("test*", caseSensitive = true)
        val resultSensitive = queryPairs.removeWhere(sensitivePredicate)
        assertEquals("Test=1&OTHER=3&other=4", resultSensitive.asString())
    }

    @Test
    fun `empty parameter handling should preserve wire format`() {
        val testCases = mapOf(
            "=" to listOf(QueryToken("", true, "")),
            "&" to listOf(QueryToken("", false, ""), QueryToken("", false, "")), // Empty segments are preserved now
            "a=&=b&c" to listOf(
                QueryToken("a", true, ""),
                QueryToken("", true, "b"), // =b is now preserved for lossless parsing
                QueryToken("c", false, "")
            )
        )
        
        testCases.forEach { (input, expectedTokens) ->
            val queryPairs = QueryPairs.from(input)
            val actualTokens = queryPairs.getTokens()
            assertEquals("Failed for input: $input", expectedTokens, actualTokens)
        }
    }

    @Test
    fun `removeAnyOf should handle multiple predicates correctly`() {
        val queryPairs = QueryPairs.from("apple=1&banana=2&cherry=3&date=4")
        val predicates = listOf(
            { name: String -> name.startsWith("a") }, // matches "apple"
            { name: String -> name.endsWith("y") }    // matches "cherry"
        )
        
        val result = queryPairs.removeAnyOf(predicates)
        
        assertEquals("banana=2&date=4", result.asString())
    }

    @Test
    fun `removeAnyOf should handle empty predicate list`() {
        val queryPairs = QueryPairs.from("a=1&b=2&c=3")
        val result = queryPairs.removeAnyOf(emptyList())
        
        assertEquals("a=1&b=2&c=3", result.asString())
    }

    @Test
    fun `property based round trip test`() {
        // Test that for any valid query string, from(x).asString() == x
        val testInputs = listOf(
            "a=1",
            "a=1&b=2",
            "flag",
            "flag&key=value",
            "a=&b=&c=",
            "flag1&flag2&flag3",
            "a=1&flag&b=2&empty=&c=3",
            "complex%20key=complex%20value&simple=data",
            "duplicate=1&duplicate=2&duplicate=3",
            "order=z&order=a&order=m" // Test order preservation
        )
        
        testInputs.forEach { input ->
            val queryPairs = QueryPairs.from(input)
            val output = queryPairs.asString()
            assertEquals("Round-trip failed for: $input", input, output)
        }
    }

    @Test
    fun `from should preserve =value tokens for lossless parsing`() {
        // Test the new lossless parsing that preserves =value tokens
        val queryPairs = QueryPairs.from("=value&key=data&=another")
        val tokens = queryPairs.getTokens()
        
        assertEquals(3, tokens.size)
        
        // First token: "=value" (empty key, non-empty value)
        assertEquals("", tokens[0].rawKey)
        assertEquals(true, tokens[0].hasEquals)
        assertEquals("value", tokens[0].rawValue)
        
        // Second token: "key=data" (normal case)
        assertEquals("key", tokens[1].rawKey)
        assertEquals(true, tokens[1].hasEquals)
        assertEquals("data", tokens[1].rawValue)
        
        // Third token: "=another" (empty key, non-empty value)
        assertEquals("", tokens[2].rawKey)
        assertEquals(true, tokens[2].hasEquals)
        assertEquals("another", tokens[2].rawValue)
        
        // Verify round-trip
        assertEquals("=value&key=data&=another", queryPairs.asString())
    }

    @Test
    fun `from should preserve lone equals sign for lossless parsing`() {
        val queryPairs = QueryPairs.from("key=value&=&flag")
        val tokens = queryPairs.getTokens()
        
        assertEquals(3, tokens.size)
        
        // Second token should be lone "=" (empty key, empty value, but hasEquals=true)
        assertEquals("", tokens[1].rawKey)
        assertEquals(true, tokens[1].hasEquals)
        assertEquals("", tokens[1].rawValue)
        
        // Verify round-trip
        assertEquals("key=value&=&flag", queryPairs.asString())
    }

    @Test
    fun `from should handle complex edge case combinations`() {
        // Test complex combinations of edge cases
        val testCases = listOf(
            "=value" to listOf(QueryToken("", true, "value")),
            "=" to listOf(QueryToken("", true, "")),
            "flag&=value&key=" to listOf(
                QueryToken("flag", false, ""),
                QueryToken("", true, "value"),
                QueryToken("key", true, "")
            ),
            "a=1&=&b=2&=empty" to listOf(
                QueryToken("a", true, "1"),
                QueryToken("", true, ""),
                QueryToken("b", true, "2"),
                QueryToken("", true, "empty")
            )
        )
        
        testCases.forEach { (input, expectedTokens) ->
            val queryPairs = QueryPairs.from(input)
            val actualTokens = queryPairs.getTokens()
            assertEquals("Failed for input: $input", expectedTokens, actualTokens)
            
            // Verify round-trip
            assertEquals("Round-trip failed for: $input", input, queryPairs.asString())
        }
    }

    @Test
    fun `addRaw should add parameters with raw encoding`() {
        val queryPairs = QueryPairs.from("existing=value")
            .addRaw("raw%20key", "raw%20value")
        
        assertEquals("existing=value&raw%20key=raw%20value", queryPairs.asString())
        
        // The raw values should be stored as-is
        val tokens = queryPairs.getTokens()
        assertEquals("raw%20key", tokens[1].rawKey)
        assertEquals("raw%20value", tokens[1].rawValue)
        
        // But decoded access should work
        assertEquals("raw key", tokens[1].decodedKey)
        assertEquals("raw value", tokens[1].decodedValue)
    }

    @Test
    fun `addDecoded should encode parameters before storage`() {
        val queryPairs = QueryPairs.from("existing=value")
            .addDecoded("decoded key", "decoded value")
        
        val tokens = queryPairs.getTokens()
        assertEquals(2, tokens.size)
        
        // The decoded values should be encoded for storage
        assertEquals("decoded%20key", tokens[1].rawKey)
        assertEquals("decoded%20value", tokens[1].rawValue)
        
        // But decoded access should return original values
        assertEquals("decoded key", tokens[1].decodedKey)
        assertEquals("decoded value", tokens[1].decodedValue)
        
        // String representation should show encoded form
        assertEquals("existing=value&decoded%20key=decoded%20value", queryPairs.asString())
    }

    @Test
    fun `addDecoded should handle special characters correctly`() {
        val queryPairs = QueryPairs.empty()
            .addDecoded("key with spaces", "value & symbols = test")
            .addDecoded("café", "naïve résumé")
        
        val result = queryPairs.asString()
        
        // Should be properly encoded
        assertTrue("Should encode spaces", result.contains("%20"))
        assertTrue("Should encode ampersand", result.contains("%26"))
        assertTrue("Should encode equals", result.contains("%3D"))
        
        // But decoded access should work correctly
        assertEquals("value & symbols = test", queryPairs.getFirst("key with spaces"))
        assertEquals("naïve résumé", queryPairs.getFirst("café"))
    }

    @Test
    fun `non-ASCII character preservation in round-trip`() {
        // Test that non-ASCII characters are preserved without corruption
        val testCases = listOf(
            "café=naïve",
            "ö=ü",
            "🚀=rocket",
            "東京=Tokyo",
            "Москва=Moscow"
        )
        
        testCases.forEach { input ->
            val queryPairs = QueryPairs.from(input)
            val output = queryPairs.asString()
            assertEquals("Non-ASCII round-trip failed for: $input", input, output)
            
            // Also verify decoded access works
            val tokens = queryPairs.getTokens()
            assertEquals(input.substringBefore("="), tokens[0].decodedKey)
            assertEquals(input.substringAfter("="), tokens[0].decodedValue)
        }
    }

    @Test
    fun `mixed encoded and literal non-ASCII handling`() {
        // Test mixed scenarios with both encoded sequences and literal non-ASCII
        val queryPairs = QueryPairs.from("café%20bar=naïve%20test&🚀%20rocket=test%21")
        
        assertEquals(2, queryPairs.size())
        
        // First parameter: "café bar" = "naïve test"
        assertEquals("naïve test", queryPairs.getFirst("café bar"))
        
        // Second parameter: "🚀 rocket" = "test!"
        assertEquals("test!", queryPairs.getFirst("🚀 rocket"))
        
        // Verify round-trip preserves the exact format
        assertEquals("café%20bar=naïve%20test&🚀%20rocket=test%21", queryPairs.asString())
    }

    // New tests for implementation plan fixes

    @Test
    fun `from should preserve empty segments for lossless round-trip`() {
        // Test that empty segments between & are preserved (using split(..., -1))
        val testCases = listOf(
            "a=1&&b=2" to "a=1&&b=2",
            "a=1&&&b=2" to "a=1&&&b=2", 
            "a=1&" to "a=1&",
            "&a=1" to "&a=1",
            "&&" to "&&"
        )
        
        testCases.forEach { (input, expected) ->
            val queryPairs = QueryPairs.from(input)
            val output = queryPairs.asString()
            assertEquals("Empty segment preservation failed for: $input", expected, output)
        }
    }

    @Test
    fun `from should support semicolon delimiter when enabled`() {
        // Test semicolon delimiter support
        val queryPairs = QueryPairs.from("a=1;b=2;c=3", acceptSemicolon = true)
        
        assertEquals(3, queryPairs.size())
        assertEquals("1", queryPairs.getFirst("a"))
        assertEquals("2", queryPairs.getFirst("b"))
        assertEquals("3", queryPairs.getFirst("c"))
        
        // Note: asString() always uses & delimiter for output
        assertEquals("a=1&b=2&c=3", queryPairs.asString())
    }

    @Test
    fun `from should support mixed ampersand and semicolon delimiters`() {
        // Test mixed delimiters when semicolon support is enabled
        val queryPairs = QueryPairs.from("a=1&b=2;c=3&d=4", acceptSemicolon = true)
        
        assertEquals(4, queryPairs.size())
        assertEquals("1", queryPairs.getFirst("a"))
        assertEquals("2", queryPairs.getFirst("b"))
        assertEquals("3", queryPairs.getFirst("c"))
        assertEquals("4", queryPairs.getFirst("d"))
        
        assertEquals("a=1&b=2&c=3&d=4", queryPairs.asString())
    }

    @Test
    fun `from should not treat semicolon as delimiter by default`() {
        // Test that semicolon is treated as literal character by default
        val queryPairs = QueryPairs.from("a=1;b=2")
        
        assertEquals(1, queryPairs.size())
        assertEquals("1;b=2", queryPairs.getFirst("a"))
        assertEquals("a=1;b=2", queryPairs.asString())
    }

    @Test
    fun `addDecoded should handle Unicode surrogate pairs correctly`() {
        // Test Unicode astral characters (surrogate pairs) with the fixed percent-encoding
        val astralChar = "𝕏" // Mathematical double-struck X (U+1D54F)
        val emojiChar = "🚀" // Rocket emoji (U+1F680)
        
        val queryPairs = QueryPairs.empty()
            .addDecoded("key", astralChar)
            .addDecoded("emoji", emojiChar)
        
        // Verify decoded access works correctly
        assertEquals(astralChar, queryPairs.getFirst("key"))
        assertEquals(emojiChar, queryPairs.getFirst("emoji"))
        
        // Verify the encoded form is correct (should be UTF-8 byte encoding)
        val result = queryPairs.asString()
        assertTrue("Should contain percent-encoded UTF-8 bytes", result.contains("%"))
        
        // Verify round-trip through parsing works
        val reparsed = QueryPairs.from(result)
        assertEquals(astralChar, reparsed.getFirst("key"))
        assertEquals(emojiChar, reparsed.getFirst("emoji"))
    }

    @Test
    fun `percent encoding should handle Unicode correctly`() {
        // Test that the fixed percentEncodeUtf8 method handles Unicode properly
        val testCases = listOf(
            "café" to "%C3%A9", // Latin characters should be UTF-8 encoded
            "naïve" to "%C3%AF", // Accented characters should be UTF-8 encoded
            "test space" to "test%20space", // Spaces should be encoded
            "test&equals=" to "test%26equals%3D", // Special chars should be encoded
            "🚀" to "%F0%9F%9A%80", // Emoji should be UTF-8 encoded
            "𝕏" to "%F0%9D%95%8F" // Astral character should be UTF-8 encoded
        )
        
        testCases.forEach { (input, expectedPattern) ->
            val queryPairs = QueryPairs.empty().addDecoded("key", input)
            val result = queryPairs.asString()
            
            assertTrue("Should contain encoded bytes for: $input", result.contains(expectedPattern))
            
            // Verify round-trip decoding works
            val reparsed = QueryPairs.from(result)
            assertEquals("Round-trip failed for: $input", input, reparsed.getFirst("key"))
        }
    }

    @Test
    fun `lossless round-trip with complex Unicode and empty segments`() {
        // Test simpler cases first
        val simpleUnicode = "🚀=rocket"
        val simpleQueryPairs = QueryPairs.from(simpleUnicode)
        val simpleRoundTrip = simpleQueryPairs.asString()
        assertEquals("Simple Unicode round-trip failed", simpleUnicode, simpleRoundTrip)
        
        // Test café case
        val cafeQuery = "café=naïve"
        val cafeQueryPairs = QueryPairs.from(cafeQuery)
        val cafeRoundTrip = cafeQueryPairs.asString()
        assertEquals("Café round-trip failed", cafeQuery, cafeRoundTrip)
        
        // Test empty segments
        val emptyQuery = "a=1&&b=2"
        val emptyQueryPairs = QueryPairs.from(emptyQuery)
        val emptyRoundTrip = emptyQueryPairs.asString()
        assertEquals("Empty segments round-trip failed", emptyQuery, emptyRoundTrip)
        
        // Test =value case specifically
        val equalValueQuery = "=value"
        val equalValueQueryPairs = QueryPairs.from(equalValueQuery)
        val equalValueTokens = equalValueQueryPairs.getTokens()
        assertEquals("Should have 1 token", 1, equalValueTokens.size)
        assertEquals("Raw key should be empty", "", equalValueTokens[0].rawKey)
        assertEquals("Raw value should be 'value'", "value", equalValueTokens[0].rawValue)
        assertEquals("Should have equals", true, equalValueTokens[0].hasEquals)
        assertEquals("Decoded key should be empty", "", equalValueTokens[0].decodedKey)
        assertEquals("Decoded value should be 'value'", "value", equalValueTokens[0].decodedValue)
        assertEquals("getFirst(\"\") should return 'value'", "value", equalValueQueryPairs.getFirst(""))
        
        // Now test the complex case
        val complexQuery = "🚀=rocket&café=naïve&&empty=&=value&flag&𝕏=math"
        val queryPairs = QueryPairs.from(complexQuery)
        val roundTrip = queryPairs.asString()
        
        assertEquals("Complex lossless round-trip failed", complexQuery, roundTrip)
        
        // Verify all values are accessible
        assertEquals("rocket", queryPairs.getFirst("🚀"))
        assertEquals("naïve", queryPairs.getFirst("café"))
        assertEquals("", queryPairs.getFirst("empty"))
        assertEquals("", queryPairs.getFirst("flag"))
        assertEquals("math", queryPairs.getFirst("𝕏"))
        
        // For empty key, getFirst("") should return the FIRST occurrence
        // In this query: "🚀=rocket&café=naïve&&empty=&=value&flag&𝕏=math"
        // The && creates an empty segment (token 2) that comes before =value (token 4)
        // So getFirst("") should return "" (from the empty segment), not "value"
        assertEquals("", queryPairs.getFirst(""))
        
        // But getAll("") should return both empty values in order
        assertEquals(listOf("", "value"), queryPairs.getAll(""))
    }

    @Test
    fun `semicolon delimiter with empty segments`() {
        // Test semicolon delimiter combined with empty segment preservation
        val queryPairs = QueryPairs.from("a=1;;b=2;", acceptSemicolon = true)
        
        // Should preserve empty segments even with semicolon delimiter
        assertEquals("a=1&&b=2&", queryPairs.asString())
        assertEquals(4, queryPairs.size()) // a=1, empty, b=2, empty
    }

    @Test
    fun `filterKeys should keep only parameters matching predicate`() {
        val queryPairs = QueryPairs.from("keep1=1&remove1=2&keep2=3&remove2=4")
            .filterKeys { it.startsWith("keep") }
        
        assertEquals("keep1=1&keep2=3", queryPairs.asString())
    }

    @Test
    fun `filterKeys should preserve order of matching parameters`() {
        val queryPairs = QueryPairs.from("z=last&a=first&b=middle&a=second")
            .filterKeys { it == "a" }
        
        assertEquals("a=first&a=second", queryPairs.asString())
    }

    @Test
    fun `filterKeys should work with decoded keys`() {
        val queryPairs = QueryPairs.from("encoded%20key=1&normal=2&another%20encoded=3")
            .filterKeys { it.contains(" ") }
        
        assertEquals("encoded%20key=1&another%20encoded=3", queryPairs.asString())
    }

    @Test
    fun `filterKeys should return empty when no matches`() {
        val queryPairs = QueryPairs.from("a=1&b=2&c=3")
            .filterKeys { it.startsWith("x") }
        
        assertTrue(queryPairs.isEmpty())
        assertEquals("", queryPairs.asString())
    }

    @Test
    fun `filterKeys should return all when all match`() {
        val original = "test1=1&test2=2&test3=3"
        val queryPairs = QueryPairs.from(original)
            .filterKeys { it.startsWith("test") }
        
        assertEquals(original, queryPairs.asString())
    }

    @Test
    fun `filterKeys should handle empty QueryPairs`() {
        val queryPairs = QueryPairs.empty()
            .filterKeys { true }
        
        assertTrue(queryPairs.isEmpty())
        assertEquals("", queryPairs.asString())
    }
}
