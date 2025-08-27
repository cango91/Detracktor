package com.gologlu.detracktor.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for RegexValidator to ensure ReDoS vulnerability detection and safe regex compilation
 */
class RegexValidatorTest {

    @Test
    fun testValidatePattern_emptyPattern() {
        val result = RegexValidator.validatePattern("")
        assertFalse("Empty pattern should be invalid", result.isValid)
        assertEquals("Pattern cannot be empty", result.errorMessage)
        assertFalse(result.isReDoSVulnerable)
    }

    @Test
    fun testValidatePattern_blankPattern() {
        val result = RegexValidator.validatePattern("   ")
        assertFalse("Blank pattern should be invalid", result.isValid)
        assertEquals("Pattern cannot be empty", result.errorMessage)
        assertFalse(result.isReDoSVulnerable)
    }

    @Test
    fun testValidatePattern_validSimplePattern() {
        val result = RegexValidator.validatePattern("example\\.com")
        assertTrue("Simple pattern should be valid", result.isValid)
        assertNull(result.errorMessage)
        assertFalse(result.isReDoSVulnerable)
    }

    @Test
    fun testValidatePattern_validComplexPattern() {
        val result = RegexValidator.validatePattern("^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        assertTrue("Complex but safe pattern should be valid", result.isValid)
        assertNull(result.errorMessage)
        assertFalse(result.isReDoSVulnerable)
    }

    @Test
    fun testIsReDoSVulnerable_nestedQuantifiers() {
        // Test various ReDoS vulnerability patterns
        val vulnerablePatterns = listOf(
            "(a+)+b",           // Classic nested quantifier
            "(a*)+b",           // Nested quantifier with *
            "(a+)*b",           // Mixed quantifiers
            "(a*)*b",           // Double star quantifier
            "(a|a)*b",          // Alternation with overlap
            "(a|a)+b",          // Alternation with overlap and +
            "(a|b|c)*d",        // Multiple alternation with quantifier
            "([a-z]+)+@",       // Character class with nested quantifier
            "(\\w+)+\\.",       // Word character with nested quantifier
        )

        vulnerablePatterns.forEach { pattern ->
            assertTrue("Pattern '$pattern' should be detected as ReDoS vulnerable", 
                RegexValidator.isReDoSVulnerable(pattern))
        }
    }

    @Test
    fun testIsReDoSVulnerable_safePatterns() {
        val safePatterns = listOf(
            "example\\.com",
            "^https?://",
            "[a-zA-Z0-9]+",
            "\\d{3}-\\d{2}-\\d{4}",
            "user@domain\\.com",
            "a+b",              // Single quantifier is safe
            "(abc)+",           // Single group with quantifier is safe
            "a|b",              // Simple alternation without quantifier
            "[a-z]*",           // Character class with single quantifier
        )

        safePatterns.forEach { pattern ->
            assertFalse("Pattern '$pattern' should NOT be detected as ReDoS vulnerable", 
                RegexValidator.isReDoSVulnerable(pattern))
        }
    }

    @Test
    fun testValidatePattern_rejectsReDoSPatterns() {
        val vulnerablePattern = "(a+)+b"
        val result = RegexValidator.validatePattern(vulnerablePattern)
        
        assertFalse("ReDoS vulnerable pattern should be rejected", result.isValid)
        assertTrue("Should be marked as ReDoS vulnerable", result.isReDoSVulnerable)
        assertTrue("Error message should mention ReDoS", 
            result.errorMessage?.contains("ReDoS vulnerability") == true)
    }

    @Test
    fun testCompileWithTimeout_validPattern() {
        val pattern = "example\\.com"
        val regex = RegexValidator.compileWithTimeout(pattern, 1000L)
        
        assertNotNull("Valid pattern should compile successfully", regex)
        assertTrue("Compiled regex should match expected input", 
            regex!!.matches("example.com"))
        assertFalse("Compiled regex should not match unexpected input", 
            regex.matches("other.com"))
    }

    @Test
    fun testCompileWithTimeout_invalidPattern() {
        val invalidPattern = "[unclosed"
        val regex = RegexValidator.compileWithTimeout(invalidPattern, 1000L)
        
        assertNull("Invalid pattern should return null", regex)
    }

    @Test
    fun testCompileWithTimeout_withOptions() {
        val pattern = "EXAMPLE"
        val regex = RegexValidator.compileWithTimeout(
            pattern, 
            1000L, 
            setOf(RegexOption.IGNORE_CASE)
        )
        
        assertNotNull("Pattern should compile with options", regex)
        assertTrue("Should match with case insensitive option", 
            regex!!.matches("example"))
        assertTrue("Should match original case", 
            regex.matches("EXAMPLE"))
    }

    @Test
    fun testSafeMatch_validPattern() {
        val pattern = "example\\.com"
        val input = "example.com"
        val result = RegexValidator.safeMatch(pattern, input)
        
        assertTrue("Safe match should succeed", result.success)
        assertTrue("Should match the input", result.matches)
        assertNull("Should not have error message", result.errorMessage)
    }

    @Test
    fun testSafeMatch_noMatch() {
        val pattern = "example\\.com"
        val input = "other.com"
        val result = RegexValidator.safeMatch(pattern, input)
        
        assertTrue("Safe match should succeed", result.success)
        assertFalse("Should not match the input", result.matches)
        assertNull("Should not have error message", result.errorMessage)
    }

    @Test
    fun testSafeMatch_reDoSPattern() {
        val vulnerablePattern = "(a+)+b"
        val input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val result = RegexValidator.safeMatch(vulnerablePattern, input)
        
        assertFalse("ReDoS pattern should be rejected", result.success)
        assertFalse("Should not match due to validation failure", result.matches)
        assertTrue("Should have error message about ReDoS", 
            result.errorMessage?.contains("ReDoS vulnerability") == true)
    }

    @Test
    fun testSafeMatch_invalidPattern() {
        val invalidPattern = "[unclosed"
        val input = "test"
        val result = RegexValidator.safeMatch(invalidPattern, input)
        
        assertFalse("Invalid pattern should fail", result.success)
        assertFalse("Should not match due to validation failure", result.matches)
        assertNotNull("Should have error message", result.errorMessage)
        // The exact error message may vary, but it should indicate a problem with the pattern
        assertTrue("Should have error message about pattern issue", 
            result.errorMessage?.isNotEmpty() == true)
    }

    @Test
    fun testTestWithTimeout_normalOperation() {
        val pattern = "test"
        val regex = Regex(pattern)
        val input = "test"
        
        val result = RegexValidator.testWithTimeout(regex, input, 1000L)
        assertTrue("Should match successfully", result)
    }

    @Test
    fun testTestWithTimeout_noMatch() {
        val pattern = "test"
        val regex = Regex(pattern)
        val input = "other"
        
        val result = RegexValidator.testWithTimeout(regex, input, 1000L)
        assertFalse("Should not match", result)
    }

    @Test
    fun testValidatePattern_complexSafePattern() {
        // Test a complex but safe regex pattern commonly used in URL validation
        val pattern = "^https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[\\w&=%.])*)?(?:#(?:[\\w.])*)?)?$"
        val result = RegexValidator.validatePattern(pattern)
        
        assertTrue("Complex safe pattern should be valid", result.isValid)
        assertFalse("Should not be flagged as ReDoS vulnerable", result.isReDoSVulnerable)
        assertNull("Should not have error message", result.errorMessage)
    }

    @Test
    fun testValidatePattern_edgeCasePatterns() {
        val edgeCases = mapOf(
            "." to true,                    // Single dot should be valid
            ".*" to true,                   // Single quantifier should be valid
            "a{1,3}" to true,               // Bounded quantifier should be valid
            "a{1000}" to true,              // Large but bounded quantifier should be valid
            "(?:abc)+" to true,             // Non-capturing group with quantifier should be valid
            "a(?=b)" to true,               // Lookahead should be valid
            "a(?!b)" to true,               // Negative lookahead should be valid
        )

        edgeCases.forEach { (pattern, shouldBeValid) ->
            val result = RegexValidator.validatePattern(pattern)
            if (shouldBeValid) {
                assertTrue("Pattern '$pattern' should be valid", result.isValid)
            } else {
                assertFalse("Pattern '$pattern' should be invalid", result.isValid)
            }
        }
    }

    @Test
    fun testPerformanceWithLargeInput() {
        // Test that safe patterns perform well even with large inputs
        val safePattern = "example\\.com"
        val largeInput = "x".repeat(10000) + "example.com"
        
        val startTime = System.currentTimeMillis()
        val result = RegexValidator.safeMatch(safePattern, largeInput, 5000L)
        val endTime = System.currentTimeMillis()
        
        assertTrue("Safe pattern should complete successfully", result.success)
        assertTrue("Should find match in large input", result.matches)
        assertTrue("Should complete within reasonable time", (endTime - startTime) < 1000)
    }
}
