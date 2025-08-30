package com.gologlu.detracktor.application.pattern

import org.junit.Test
import org.junit.Assert.*

class DefaultPatternEngineTest {

    private val engine = DefaultPatternEngine()

    @Test
    fun `validate should accept valid exact patterns`() {
        val spec = PatternSpec("test", Kind.EXACT, Case.SENSITIVE)
        val result = engine.validate(spec)
        
        assertTrue("Valid exact pattern should be accepted", result.isSuccess)
    }

    @Test
    fun `validate should accept valid glob patterns`() {
        val validGlobs = listOf("*", "test*", "*test", "test?", "a*b?c")
        
        validGlobs.forEach { pattern ->
            val spec = PatternSpec(pattern, Kind.GLOB, Case.INSENSITIVE)
            val result = engine.validate(spec)
            
            assertTrue("Valid glob pattern '$pattern' should be accepted", result.isSuccess)
        }
    }

    @Test
    fun `validate should accept valid regex patterns`() {
        val validRegexes = listOf(".*", "test.*", "^test$", "[a-z]+", "\\d{3}")
        
        validRegexes.forEach { pattern ->
            val spec = PatternSpec(pattern, Kind.REGEX, Case.SENSITIVE)
            val result = engine.validate(spec)
            
            assertTrue("Valid regex pattern '$pattern' should be accepted", result.isSuccess)
        }
    }

    @Test
    fun `validate should reject invalid regex patterns`() {
        val invalidRegexes = listOf("[", "(?", "*+", "\\")
        
        invalidRegexes.forEach { pattern ->
            val spec = PatternSpec(pattern, Kind.REGEX, Case.SENSITIVE)
            val result = engine.validate(spec)
            
            assertTrue("Invalid regex pattern '$pattern' should be rejected", result.isFailure)
        }
    }

    @Test
    fun `compile exact pattern should match exactly`() {
        val spec = PatternSpec("test", Kind.EXACT, Case.SENSITIVE)
        val predicate = engine.compile(spec)
        
        assertTrue("Should match exact string", predicate("test"))
        assertFalse("Should not match different string", predicate("Test"))
        assertFalse("Should not match substring", predicate("testing"))
        assertFalse("Should not match superstring", predicate("tes"))
    }

    @Test
    fun `compile exact pattern case insensitive should ignore case`() {
        val spec = PatternSpec("Test", Kind.EXACT, Case.INSENSITIVE)
        val predicate = engine.compile(spec)
        
        assertTrue("Should match same case", predicate("Test"))
        assertTrue("Should match lowercase", predicate("test"))
        assertTrue("Should match uppercase", predicate("TEST"))
        assertTrue("Should match mixed case", predicate("tEsT"))
        assertFalse("Should not match different string", predicate("testing"))
    }

    @Test
    fun `compile glob pattern should handle wildcards`() {
        val testCases = mapOf(
            "*" to listOf("" to true, "anything" to true, "test" to true),
            "test*" to listOf("test" to true, "testing" to true, "test123" to true, "tes" to false),
            "*test" to listOf("test" to true, "mytest" to true, "123test" to true, "testing" to false),
            "test?" to listOf("test1" to true, "testA" to true, "test" to false, "test12" to false),
            "a*b?c" to listOf("abc" to false, "a1b2c" to true, "axbxc" to true, "ab1c" to true, "a1b23c" to false)
        )
        
        testCases.forEach { (pattern, cases) ->
            val spec = PatternSpec(pattern, Kind.GLOB, Case.SENSITIVE)
            val predicate = engine.compile(spec)
            
            cases.forEach { (input, expected) ->
                assertEquals("Pattern '$pattern' with input '$input'", expected, predicate(input))
            }
        }
    }

    @Test
    fun `compile glob pattern case insensitive should ignore case`() {
        val spec = PatternSpec("Test*", Kind.GLOB, Case.INSENSITIVE)
        val predicate = engine.compile(spec)
        
        assertTrue("Should match same case", predicate("Testing"))
        assertTrue("Should match lowercase", predicate("testing"))
        assertTrue("Should match uppercase", predicate("TESTING"))
        assertTrue("Should match mixed case", predicate("tEsTiNg"))
        assertFalse("Should not match non-matching pattern", predicate("other"))
    }

    @Test
    fun `compile regex pattern should work correctly`() {
        val testCases = mapOf(
            ".*" to listOf("" to true, "anything" to true),
            "^test$" to listOf("test" to true, "testing" to false, "mytest" to false),
            "[a-z]+" to listOf("abc" to true, "ABC" to false, "123" to false),
            "\\d{3}" to listOf("123" to true, "12" to false, "1234" to false, "abc" to false)
        )
        
        testCases.forEach { (pattern, cases) ->
            val spec = PatternSpec(pattern, Kind.REGEX, Case.SENSITIVE)
            val predicate = engine.compile(spec)
            
            cases.forEach { (input, expected) ->
                assertEquals("Regex '$pattern' with input '$input'", expected, predicate(input))
            }
        }
    }

    @Test
    fun `compile regex pattern case insensitive should ignore case`() {
        val spec = PatternSpec("[A-Z]+", Kind.REGEX, Case.INSENSITIVE)
        val predicate = engine.compile(spec)
        
        assertTrue("Should match uppercase", predicate("ABC"))
        assertTrue("Should match lowercase", predicate("abc"))
        assertTrue("Should match mixed case", predicate("AbC"))
        assertFalse("Should not match numbers", predicate("123"))
    }

    @Test
    fun `glob pattern should escape regex special characters`() {
        val spec = PatternSpec("test.log", Kind.GLOB, Case.SENSITIVE)
        val predicate = engine.compile(spec)
        
        assertTrue("Should match exact string with dot", predicate("test.log"))
        assertFalse("Should not treat dot as regex wildcard", predicate("testXlog"))
    }

    @Test
    fun `supportedKinds should return all pattern kinds`() {
        val kinds = engine.supportedKinds()
        
        assertEquals("Should support all pattern kinds", setOf(Kind.EXACT, Kind.GLOB, Kind.REGEX), kinds)
    }

    @Test
    fun `supportedCases should return all case modes`() {
        val cases = engine.supportedCases()
        
        assertEquals("Should support all case modes", setOf(Case.SENSITIVE, Case.INSENSITIVE), cases)
    }

    @Test
    fun `supports should return true for supported combinations`() {
        val supportedSpecs = listOf(
            PatternSpec("test", Kind.EXACT, Case.SENSITIVE),
            PatternSpec("test*", Kind.GLOB, Case.INSENSITIVE),
            PatternSpec(".*", Kind.REGEX, Case.SENSITIVE)
        )
        
        supportedSpecs.forEach { spec ->
            assertTrue("Should support ${spec.kind} with ${spec.case}", engine.supports(spec))
        }
    }

    @Test
    fun `compile should throw for invalid patterns`() {
        val invalidSpec = PatternSpec("[", Kind.REGEX, Case.SENSITIVE)
        
        try {
            engine.compile(invalidSpec)
            fail("Should throw exception for invalid regex pattern")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain error message", e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun `integration test with domain predicate usage`() {
        // Test that compiled predicates work with domain QueryPairs
        val queryPairs = com.gologlu.detracktor.domain.model.QueryPairs.from("test1=1&keep=2&test2=3&other=4")
        
        // Create predicate using application layer
        val spec = PatternSpec("test*", Kind.GLOB, Case.INSENSITIVE)
        val predicate = engine.compile(spec)
        
        // Use predicate with domain
        val result = queryPairs.removeWhere(predicate)
        
        assertEquals("keep=2&other=4", result.asString())
    }

    @Test
    fun `multiple predicates integration test`() {
        // Test multiple predicates with domain QueryPairs
        val queryPairs = com.gologlu.detracktor.domain.model.QueryPairs.from("apple=1&banana=2&cherry=3&date=4")
        
        // Create multiple predicates using application layer
        val predicates = listOf(
            engine.compile(PatternSpec("a*", Kind.GLOB, Case.INSENSITIVE)), // matches "apple"
            engine.compile(PatternSpec("*y", Kind.GLOB, Case.INSENSITIVE))  // matches "cherry"
        )
        
        // Use predicates with domain
        val result = queryPairs.removeAnyOf(predicates)
        
        assertEquals("banana=2&date=4", result.asString())
    }
}
