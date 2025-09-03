package com.gologlu.detracktor.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

class QueryMapTest {

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
    fun `asString should render single values correctly`() {
        val queryMap = QueryMap.from("key1=value1&key2=value2")
        assertEquals("key1=value1&key2=value2", queryMap.asString())
    }

    @Test
    fun `asString should render multi-values correctly with ampersand separator`() {
        val queryMap = QueryMap.from("key=value1&key=value2&other=test")
        val result = queryMap.asString()
        
        // Should render as key=value1&key=value2&other=test, not key=value1=value2&other=test
        assertTrue("Multi-values should be separated by &", result.contains("key=value1&key=value2"))
        assertTrue("Other parameters should be preserved", result.contains("other=test"))
    }

    @Test
    fun `asString should handle empty values`() {
        val queryMap = QueryMap.from("key1=&key2=value2")
        val result = queryMap.asString()
        assertTrue("Empty values should be preserved", result.contains("key1="))
        assertTrue("Non-empty values should be preserved", result.contains("key2=value2"))
    }

    @Test
    fun `removeAnyOf should work with multiple predicates`() {
        val queryMap = QueryMap.from("test1=value1&test2=value2&other=value3")
        val predicates = listOf(
            createGlobPredicate("test*"),
            createGlobPredicate("other")
        )
        
        val result = queryMap.removeAnyOf(predicates)
        
        // All parameters should be removed since predicates match all keys
        assertTrue("All matching parameters should be removed", result.isEmpty())
    }

    @Test
    fun `removeWhere should preserve non-matching parameters`() {
        val queryMap = QueryMap.from("keep1=value1&remove=value2&keep2=value3")
        val predicate = { name: String -> name == "remove" }
        
        val result = queryMap.removeWhere(predicate)
        
        assertEquals(2, result.get("keep1").size + result.get("keep2").size)
        assertTrue("Non-matching parameters should be preserved", result.get("remove").isEmpty())
    }

    @Test
    fun `removeAnyOf should handle empty predicate list`() {
        val queryMap = QueryMap.from("a=1&b=2&c=3")
        val result = queryMap.removeAnyOf(emptyList())
        
        assertEquals("a=1&b=2&c=3", result.asString())
    }

    @Test
    fun `from should parse query string correctly`() {
        val queryMap = QueryMap.from("a=1&b=2&a=3")
        
        assertEquals(listOf("1", "3"), queryMap.get("a"))
        assertEquals(listOf("2"), queryMap.get("b"))
    }

    @Test
    fun `from should handle null input`() {
        val queryMap = QueryMap.from(null)
        assertTrue("Null input should create empty QueryMap", queryMap.isEmpty())
    }

    @Test
    fun `from should handle empty string`() {
        val queryMap = QueryMap.from("")
        assertTrue("Empty string should create empty QueryMap", queryMap.isEmpty())
    }

    @Test
    fun `round trip should preserve data and exact order`() {
        val original = "key1=value1&key2=value2&key1=value3"
        val queryMap = QueryMap.from(original)
        val roundTrip = QueryMap.from(queryMap.asString())
        
        assertEquals("Round trip should preserve key1 values", queryMap.get("key1"), roundTrip.get("key1"))
        assertEquals("Round trip should preserve key2 values", queryMap.get("key2"), roundTrip.get("key2"))
        assertEquals("Round trip should preserve exact order", original, queryMap.asString())
        assertEquals("Round trip should preserve exact order", original, roundTrip.asString())
    }

    @Test
    fun `QueryMap should now preserve exact order through QueryPairs`() {
        val queryMap = QueryMap.from("c=3&a=1&b=2&a=4")
        
        // Should preserve exact order, not alphabetical or first-occurrence order
        assertEquals("c=3&a=1&b=2&a=4", queryMap.asString())
        
        // Values should still be grouped correctly
        assertEquals(listOf("1", "4"), queryMap.get("a"))
        assertEquals(listOf("2"), queryMap.get("b"))
        assertEquals(listOf("3"), queryMap.get("c"))
    }

    @Test
    fun `QueryMap should preserve duplicates in exact order`() {
        val queryMap = QueryMap.from("param=first&other=middle&param=second&param=third")
        
        assertEquals("param=first&other=middle&param=second&param=third", queryMap.asString())
        assertEquals(listOf("first", "second", "third"), queryMap.get("param"))
        assertEquals(listOf("middle"), queryMap.get("other"))
    }

    @Test
    fun `QueryMap operations should maintain order`() {
        val original = QueryMap.from("a=1&b=2&c=3")
        
        // Adding should append to end
        val withAdded = original.set("d", "4")
        assertEquals("a=1&b=2&c=3&d=4", withAdded.asString())
        
        // Removing should preserve order of remaining items
        val withRemoved = original.remove("b")
        assertEquals("a=1&c=3", withRemoved.asString())
    }

    @Test
    fun `QueryMap set with multiple values should preserve order`() {
        val queryMap = QueryMap.from("a=1&b=2")
        val updated = queryMap.set("c", listOf("first", "second", "third"))
        
        assertEquals("a=1&b=2&c=first&c=second&c=third", updated.asString())
        assertEquals(listOf("first", "second", "third"), updated.get("c"))
    }

    @Test
    fun `QueryMap toQueryPairs should provide access to underlying representation`() {
        val queryMap = QueryMap.from("a=1&b=2&a=3")
        val queryPairs = queryMap.toQueryPairs()
        
        assertEquals("a=1&b=2&a=3", queryPairs.asString())
        assertEquals(queryMap.asString(), queryPairs.asString())
        assertEquals(queryMap.get("a"), queryPairs.getAll("a"))
    }

    @Test
    fun `QueryMap from QueryPairs should work correctly`() {
        val queryPairs = QueryPairs.from("x=10&y=20&x=30")
        val queryMap = QueryMap.from(queryPairs)
        
        assertEquals("x=10&y=20&x=30", queryMap.asString())
        assertEquals(listOf("10", "30"), queryMap.get("x"))
        assertEquals(listOf("20"), queryMap.get("y"))
    }

    @Test
    fun `setDecoded should encode single value before storage`() {
        val queryMap = QueryMap.from("existing=value")
            .setDecoded("decoded key", "decoded value")
        
        // Should be encoded in the string representation
        val result = queryMap.asString()
        assertTrue("Should contain encoded key", result.contains("decoded%20key"))
        assertTrue("Should contain encoded value", result.contains("decoded%20value"))
        
        // But decoded access should work correctly
        assertEquals(listOf("decoded value"), queryMap.get("decoded key"))
    }

    @Test
    fun `setDecoded should encode multiple values before storage`() {
        val queryMap = QueryMap.from("existing=value")
            .setDecoded("test key", listOf("value 1", "value & 2", "value = 3"))
        
        val result = queryMap.asString()
        
        // Should contain encoded forms
        assertTrue("Should encode spaces", result.contains("%20"))
        assertTrue("Should encode ampersand", result.contains("%26"))
        assertTrue("Should encode equals", result.contains("%3D"))
        
        // But decoded access should return original values
        assertEquals(listOf("value 1", "value & 2", "value = 3"), queryMap.get("test key"))
    }

    @Test
    fun `setDecoded should replace existing values`() {
        val queryMap = QueryMap.from("key=old1&key=old2&other=keep")
            .setDecoded("key", "new value")
        
        // Should replace all old values with the new one
        assertEquals(listOf("new value"), queryMap.get("key"))
        assertEquals(listOf("keep"), queryMap.get("other"))
        
        // Should maintain order (key appears first, other appears last)
        val result = queryMap.asString()
        assertTrue("Should maintain relative order", result.indexOf("key=") < result.indexOf("other="))
    }

    @Test
    fun `setDecoded with multiple values should replace existing values`() {
        val queryMap = QueryMap.from("param=old&other=keep")
            .setDecoded("param", listOf("new1", "new2", "new3"))
        
        assertEquals(listOf("new1", "new2", "new3"), queryMap.get("param"))
        assertEquals(listOf("keep"), queryMap.get("other"))
        
        // Should preserve order: param values first, then other
        val result = queryMap.asString()
        assertTrue("Should contain all new values", result.contains("param=new1&param=new2&param=new3"))
    }

    @Test
    fun `setDecoded should handle special characters correctly`() {
        val queryMap = QueryMap.empty()
            .setDecoded("café", "naïve résumé")
            .setDecoded("symbols", "test & = ? # %")
        
        // Verify decoded access works
        assertEquals(listOf("naïve résumé"), queryMap.get("café"))
        assertEquals(listOf("test & = ? # %"), queryMap.get("symbols"))
        
        // Verify encoding in string representation
        val result = queryMap.asString()
        assertTrue("Should encode special characters", result.contains("%"))
        
        // Verify round-trip through parsing
        val reparsed = QueryMap.from(result)
        assertEquals(listOf("naïve résumé"), reparsed.get("café"))
        assertEquals(listOf("test & = ? # %"), reparsed.get("symbols"))
    }

    @Test
    fun `setDecoded should handle Unicode correctly`() {
        val queryMap = QueryMap.empty()
            .setDecoded("emoji", "🚀 rocket")
            .setDecoded("math", "𝕏 symbol")
        
        // Verify decoded access
        assertEquals(listOf("🚀 rocket"), queryMap.get("emoji"))
        assertEquals(listOf("𝕏 symbol"), queryMap.get("math"))
        
        // Verify round-trip preservation
        val result = queryMap.asString()
        val reparsed = QueryMap.from(result)
        assertEquals(listOf("🚀 rocket"), reparsed.get("emoji"))
        assertEquals(listOf("𝕏 symbol"), reparsed.get("math"))
    }

    @Test
    fun `setDecoded should work with empty values`() {
        val queryMap = QueryMap.from("existing=value")
            .setDecoded("empty key", "")
            .setDecoded("empty list", emptyList())
        
        assertEquals(listOf(""), queryMap.get("empty key"))
        assertEquals(emptyList<String>(), queryMap.get("empty list"))
        
        // Empty key should still appear in string representation
        val result = queryMap.asString()
        assertTrue("Should contain empty key", result.contains("empty%20key="))
        assertFalse("Should not contain empty list key", result.contains("empty%20list"))
    }

    @Test
    fun `setDecoded should maintain order when replacing parameters`() {
        val queryMap = QueryMap.from("first=1&middle=2&last=3")
            .setDecoded("middle", "new middle")
        
        assertEquals(listOf("1"), queryMap.get("first"))
        assertEquals(listOf("new middle"), queryMap.get("middle"))
        assertEquals(listOf("3"), queryMap.get("last"))
        
        // Should maintain the original position of 'middle'
        val result = queryMap.asString()
        val firstPos = result.indexOf("first=")
        val middlePos = result.indexOf("middle=")
        val lastPos = result.indexOf("last=")
        
        assertTrue("Order should be preserved", firstPos < middlePos && middlePos < lastPos)
    }

    @Test
    fun `setDecoded should handle case-sensitive keys correctly`() {
        val queryMap = QueryMap.from("Key=1&key=2&KEY=3")
            .setDecoded("key", "updated")
        
        // Should only replace the exact case match
        assertEquals(listOf("1"), queryMap.get("Key"))
        assertEquals(listOf("updated"), queryMap.get("key"))
        assertEquals(listOf("3"), queryMap.get("KEY"))
    }

    private fun QueryMap.Companion.empty(): QueryMap = QueryMap.from("")
}
