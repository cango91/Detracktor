package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.CleaningRule
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for the simplified RuleEngine.
 * Focuses on regex compilation and URL matching functionality.
 */
class RuleEngineTest {
    
    private val ruleEngine = RuleEngine()
    
    @Test
    fun testCompileRules_validRules() {
        val rules = listOf(
            CleaningRule(
                id = "test-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*", "gclid"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        
        assertEquals(1, compiled.size)
        assertEquals("test-rule", compiled[0].rule.id)
        assertEquals(2, compiled[0].paramPatterns.size)
    }
    
    @Test
    fun testCompileRules_invalidHostPattern() {
        val rules = listOf(
            CleaningRule(
                id = "invalid-rule",
                hostPattern = "[invalid-regex",
                parameterPatterns = listOf("utm_.*"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        
        assertEquals(0, compiled.size) // Invalid rule should be skipped
    }
    
    @Test
    fun testCompileRules_disabledRule() {
        val rules = listOf(
            CleaningRule(
                id = "disabled-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*"),
                priority = 1,
                enabled = false
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        
        assertEquals(0, compiled.size) // Disabled rule should be skipped
    }
    
    @Test
    fun testCompileRules_priorityOrdering() {
        val rules = listOf(
            CleaningRule(
                id = "low-priority",
                hostPattern = ".*\\.example\\..*",
                parameterPatterns = listOf("test"),
                priority = 10,
                enabled = true
            ),
            CleaningRule(
                id = "high-priority",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        
        assertEquals(2, compiled.size)
        assertEquals("high-priority", compiled[0].rule.id) // Should be first due to lower priority number
        assertEquals("low-priority", compiled[1].rule.id)
    }
    
    @Test
    fun testMatchRules_googleUrl() {
        val rules = listOf(
            CleaningRule(
                id = "google-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*", "gclid"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("https://www.google.com/search?q=test", compiled)
        
        assertEquals(1, matches.size)
        assertEquals("google-rule", matches[0])
    }
    
    @Test
    fun testMatchRules_noMatch() {
        val rules = listOf(
            CleaningRule(
                id = "google-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("https://www.example.com/page", compiled)
        
        assertEquals(0, matches.size)
    }
    
    @Test
    fun testMatchRules_multipleMatches() {
        val rules = listOf(
            CleaningRule(
                id = "google-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*"),
                priority = 1,
                enabled = true
            ),
            CleaningRule(
                id = "generic-rule",
                hostPattern = ".*",
                parameterPatterns = listOf("fbclid"),
                priority = 10,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("https://www.google.com/search", compiled)
        
        assertEquals(2, matches.size)
        assertTrue(matches.contains("google-rule"))
        assertTrue(matches.contains("generic-rule"))
    }
    
    @Test
    fun testGetMatchingParameterPatterns() {
        val rules = listOf(
            CleaningRule(
                id = "google-rule",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*", "gclid"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val patterns = ruleEngine.getMatchingParameterPatterns("https://www.google.com/search", compiled)
        
        assertEquals(2, patterns.size)
    }
    
    @Test
    fun testMatchRules_urlWithCredentials() {
        val rules = listOf(
            CleaningRule(
                id = "test-rule",
                hostPattern = ".*\\.example\\..*",
                parameterPatterns = listOf("param"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("https://user:pass@www.example.com/page", compiled)
        
        assertEquals(1, matches.size)
        assertEquals("test-rule", matches[0])
    }
    
    @Test
    fun testMatchRules_urlWithPort() {
        val rules = listOf(
            CleaningRule(
                id = "test-rule",
                hostPattern = ".*\\.example\\..*",
                parameterPatterns = listOf("param"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("https://www.example.com:8080/page", compiled)
        
        assertEquals(1, matches.size)
        assertEquals("test-rule", matches[0])
    }
    
    @Test
    fun testMatchRules_invalidUrl() {
        val rules = listOf(
            CleaningRule(
                id = "test-rule",
                hostPattern = ".*\\.example\\..*",
                parameterPatterns = listOf("param"),
                priority = 1,
                enabled = true
            )
        )
        
        val compiled = ruleEngine.compileRules(rules)
        val matches = ruleEngine.matchRules("not-a-url", compiled)
        
        assertEquals(0, matches.size)
    }
}
