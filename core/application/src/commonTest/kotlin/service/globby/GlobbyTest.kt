package com.gologlu.detracktor.application.service.globby

import com.gologlu.detracktor.application.error.AppValidationException
import org.junit.Test
import org.junit.Assert.*

class GlobbyTest {

    @Test
    fun `matches_should_handle_exact_string_match`() {
        assertTrue(Globby.matches("hello", "hello"))
        assertFalse(Globby.matches("hello", "world"))
        assertFalse(Globby.matches("hello", "Hello"))
    }

    @Test
    fun `matches_should_handle_single_wildcard`() {
        assertTrue(Globby.matches("?", "a"))
        assertTrue(Globby.matches("?", "1"))
        assertTrue(Globby.matches("?", "!"))
        assertFalse(Globby.matches("?", ""))
        assertFalse(Globby.matches("?", "ab"))
    }

    @Test
    fun `matches_should_handle_star_wildcard`() {
        assertTrue(Globby.matches("*", ""))
        assertTrue(Globby.matches("*", "a"))
        assertTrue(Globby.matches("*", "hello"))
        assertTrue(Globby.matches("*", "very long string"))
    }

    @Test
    fun `matches_should_handle_prefix_patterns`() {
        assertTrue(Globby.matches("utm_*", "utm_"))
        assertTrue(Globby.matches("utm_*", "utm_source"))
        assertTrue(Globby.matches("utm_*", "utm_campaign"))
        assertFalse(Globby.matches("utm_*", "utm"))
        assertFalse(Globby.matches("utm_*", "gclid"))
    }

    @Test
    fun `matches_should_handle_suffix_patterns`() {
        assertTrue(Globby.matches("*_id", "_id"))
        assertTrue(Globby.matches("*_id", "user_id"))
        assertTrue(Globby.matches("*_id", "session_id"))
        assertFalse(Globby.matches("*_id", "id"))
        assertFalse(Globby.matches("*_id", "user_id_extra"))
    }

    @Test
    fun `matches_should_handle_middle_patterns`() {
        assertTrue(Globby.matches("utm_*_param", "utm_source_param"))
        assertTrue(Globby.matches("utm_*_param", "utm__param"))
        assertTrue(Globby.matches("utm_*_param", "utm_very_long_name_param"))
        assertFalse(Globby.matches("utm_*_param", "utm_source"))
        assertFalse(Globby.matches("utm_*_param", "source_param"))
    }

    @Test
    fun `matches_should_handle_multiple_wildcards`() {
        assertTrue(Globby.matches("*_*", "_"))
        assertTrue(Globby.matches("*_*", "a_b"))
        assertTrue(Globby.matches("*_*", "utm_source"))
        assertTrue(Globby.matches("*_*", "very_long_parameter_name"))
        assertFalse(Globby.matches("*_*", "nounderscores"))
    }

    @Test
    fun `matches_should_handle_question_mark_patterns`() {
        assertTrue(Globby.matches("param?", "param1"))
        assertTrue(Globby.matches("param?", "paramX"))
        assertTrue(Globby.matches("?param", "1param"))
        assertTrue(Globby.matches("pa?am", "param"))
        assertFalse(Globby.matches("param?", "param"))
        assertFalse(Globby.matches("param?", "param12"))
    }

    @Test
    fun `matches_should_handle_mixed_wildcards`() {
        assertTrue(Globby.matches("utm_*?", "utm_a"))
        assertTrue(Globby.matches("utm_*?", "utm_source1"))
        assertTrue(Globby.matches("?utm_*", "1utm_source"))
        assertFalse(Globby.matches("utm_*?", "utm_"))
        assertFalse(Globby.matches("?utm_*", "utm_source"))
    }

    @Test
    fun `matches_should_handle_escaped_asterisk`() {
        assertTrue(Globby.matches("\\*", "*"))
        assertTrue(Globby.matches("param\\*name", "param*name"))
        assertTrue(Globby.matches("utm_\\*_source", "utm_*_source"))
        assertFalse(Globby.matches("\\*", "anything"))
        assertFalse(Globby.matches("param\\*name", "paramXname"))
    }

    @Test
    fun `matches_should_handle_escaped_question_mark`() {
        assertTrue(Globby.matches("\\?", "?"))
        assertTrue(Globby.matches("param\\?name", "param?name"))
        assertTrue(Globby.matches("what\\?", "what?"))
        assertFalse(Globby.matches("\\?", "a"))
        assertFalse(Globby.matches("param\\?name", "paramXname"))
    }

    @Test
    fun `matches_should_handle_escaped_backslash`() {
        assertTrue(Globby.matches("\\\\", "\\"))
        assertTrue(Globby.matches("path\\\\file", "path\\file"))
        assertTrue(Globby.matches("\\\\*", "\\anything"))
        assertFalse(Globby.matches("\\\\", "\\\\"))
        assertFalse(Globby.matches("path\\\\file", "path/file"))
    }

    @Test
    fun `matches_should_handle_complex_escape_sequences`() {
        assertTrue(Globby.matches("\\*\\?\\\\", "*?\\"))
        assertTrue(Globby.matches("param\\*\\?end", "param*?end"))
        assertTrue(Globby.matches("\\**", "*anything"))
        assertTrue(Globby.matches("*\\*", "prefix*"))
        assertFalse(Globby.matches("\\*\\?\\\\", "*?"))
    }

    @Test
    fun `matches_should_handle_backtracking_scenarios`() {
        // Complex backtracking case - test each one individually to find the failing case
        val testCases = listOf(
            Triple("*a*b*c", "xaxbxc", true),
            Triple("*a*b*c", "aabbcc", true),
            Triple("*a*b*c", "abc", true),
            Triple("*a*b*c*", "xyzabcxyz", true), // Fixed: added trailing * to match the xyz
            Triple("*a*b*c", "ab", false),
            Triple("*a*b*c", "ac", false),
            Triple("*a*b*c", "cba", false)
        )
        
        for ((pattern, input, expected) in testCases) {
            val result = Globby.matches(pattern, input)
            if (result != expected) {
                fail("FAILED: matches('$pattern', '$input') = $result, expected $expected")
            }
        }
    }

    @Test
    fun `matches_should_handle_greedy_matching`() {
        // Test that * is greedy and backtracks correctly
        assertTrue(Globby.matches("*ab", "aaaaab"))
        assertTrue(Globby.matches("*ab", "xyzab"))
        assertTrue(Globby.matches("a*a", "aaa"))
        assertTrue(Globby.matches("a*a", "abcda"))
        assertFalse(Globby.matches("*ab", "aaaaaa"))
    }

    @Test
    fun `matches_should_handle_empty_inputs`() {
        assertTrue(Globby.matches("", ""))
        assertTrue(Globby.matches("*", ""))
        assertFalse(Globby.matches("", "a"))
        assertFalse(Globby.matches("a", ""))
    }

    @Test
    fun `matches_should_handle_unicode_characters`() {
        assertTrue(Globby.matches("café", "café"))
        assertTrue(Globby.matches("*é", "café"))
        assertTrue(Globby.matches("测试*", "测试参数"))
        assertTrue(Globby.matches("?", "€"))
        assertTrue(Globby.matches("*", "🚀"))
        assertFalse(Globby.matches("café", "cafe"))
    }

    @Test
    fun `matches_should_handle_long_strings`() {
        val longPattern = "prefix_" + "*".repeat(10) + "_suffix"
        val longString = "prefix_" + "x".repeat(1000) + "_suffix"
        assertTrue(Globby.matches(longPattern, longString))
        
        val veryLongString = "a".repeat(10000)
        assertTrue(Globby.matches("*", veryLongString))
        assertTrue(Globby.matches("a*", veryLongString))
        assertTrue(Globby.matches("*a", veryLongString))
    }

    // Validation tests
    @Test
    fun `requireValid_should_accept_valid_patterns`() {
        // Should not throw
        Globby.requireValid("utm_*", "test.pattern")
        Globby.requireValid("?param", "test.pattern")
        Globby.requireValid("param\\*name", "test.pattern")
        Globby.requireValid("a".repeat(256), "test.pattern")
    }

    @Test
    fun `requireValid_should_reject_empty_pattern`() {
        try {
            Globby.requireValid("", "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `requireValid_should_reject_too_long_pattern`() {
        try {
            Globby.requireValid("a".repeat(257), "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("too long") == true)
        }
    }

    @Test
    fun `requireValid_should_reject_control_characters`() {
        val controlChars = listOf('\u0000', '\u0001', '\u001F', '\u007F')
        
        for (char in controlChars) {
            try {
                Globby.requireValid("param${char}name", "test.pattern")
                fail("Should have thrown AppValidationException for control character $char")
            } catch (e: AppValidationException) {
                assertTrue(e.message?.contains("Control character") == true)
            }
        }
    }

    @Test
    fun `requireValid_should_reject_newline_characters`() {
        try {
            Globby.requireValid("param\nname", "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("Newline") == true)
        }
    }

    @Test
    fun `requireValid_should_reject_carriage_return`() {
        try {
            Globby.requireValid("param\rname", "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("Carriage return") == true)
        }
    }

    @Test
    fun `requireValid_should_reject_tab_characters`() {
        try {
            Globby.requireValid("param\tname", "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("Tab") == true)
        }
    }

    @Test
    fun `requireValid_should_reject_trailing_backslash`() {
        try {
            Globby.requireValid("param\\", "test.pattern")
            fail("Should have thrown AppValidationException")
        } catch (e: AppValidationException) {
            assertTrue(e.message?.contains("Trailing backslash") == true)
        }
    }

    @Test
    fun `requireValid_should_accept_escaped_characters`() {
        // Should not throw for any escaped character
        Globby.requireValid("\\a", "test.pattern")
        Globby.requireValid("\\1", "test.pattern")
        Globby.requireValid("\\!", "test.pattern")
        Globby.requireValid("\\\\", "test.pattern")
        Globby.requireValid("\\*", "test.pattern")
        Globby.requireValid("\\?", "test.pattern")
    }

    @Test
    fun `matches_should_handle_edge_case_patterns`() {
        // Pattern longer than input
        assertFalse(Globby.matches("verylongpattern", "short"))
        
        // Multiple consecutive wildcards
        assertTrue(Globby.matches("**", "anything"))
        assertTrue(Globby.matches("???", "abc"))
        assertFalse(Globby.matches("???", "ab"))
        
        // Wildcards at boundaries
        assertTrue(Globby.matches("*a", "a"))
        assertTrue(Globby.matches("a*", "a"))
        assertTrue(Globby.matches("?a", "ba"))
        assertTrue(Globby.matches("a?", "ab"))
    }

    @Test
    fun `matches_should_handle_malformed_escape_in_matching`() {
        // These patterns would be caught by requireValid, but test matching behavior
        // if somehow a malformed pattern gets through
        assertFalse(Globby.matches("trailing\\", "anything"))
    }

    @Test
    fun `matches_should_be_case_sensitive`() {
        assertFalse(Globby.matches("UTM_*", "utm_source"))
        assertFalse(Globby.matches("utm_*", "UTM_SOURCE"))
        assertTrue(Globby.matches("UTM_*", "UTM_SOURCE"))
        assertTrue(Globby.matches("utm_*", "utm_source"))
    }

    @Test
    fun `matches_should_handle_special_parameter_names`() {
        // Real-world parameter names that might cause issues
        assertTrue(Globby.matches("*", ""))
        assertTrue(Globby.matches("*", "_"))
        assertTrue(Globby.matches("*", "__"))
        assertTrue(Globby.matches("*", "123"))
        assertTrue(Globby.matches("*", "param-with-dashes"))
        assertTrue(Globby.matches("*", "param.with.dots"))
        assertTrue(Globby.matches("*", "param_with_underscores"))
        assertTrue(Globby.matches("*", "ALLCAPS"))
        assertTrue(Globby.matches("*", "mixedCASE"))
    }
}
