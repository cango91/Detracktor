package com.gologlu.detracktor.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.data.SecurityConfig
import com.gologlu.detracktor.utils.RegexRiskLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for RegexValidator that test Android-specific functionality
 * and performance characteristics under real device conditions.
 */
@RunWith(AndroidJUnit4::class)
class RegexValidatorInstrumentedTest {

    private lateinit var validator: RegexValidator
    private lateinit var securityConfig: SecurityConfig

    @Before
    fun setUp() {
        validator = RegexValidator()
        securityConfig = SecurityConfig(
            enableRegexValidation = true,
            regexTimeoutMs = 1000L,
            maxRegexLength = 1000,
            rejectHighRiskPatterns = true,
            enablePerformanceTesting = true
        )
    }

    @Test
    fun testReDoSProtectionOnDevice() {
        val maliciousPatterns = listOf(
            "(a+)+b",
            "(a|a)*b",
            "a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}a{50,100}"
        )
        
        maliciousPatterns.forEach { pattern ->
            val result = validator.validateRegexSafety(pattern)
            assertFalse("Pattern should be detected as unsafe: $pattern", result.isSafe)
            assertTrue("Should detect ReDoS risk", result.riskLevel in listOf(
                RegexRiskLevel.HIGH, 
                RegexRiskLevel.CRITICAL
            ))
        }
    }

    @Test
    fun testTimeoutProtectionOnDevice() {
        val complexPattern = "(a+)+b"
        val testString = "a".repeat(100) + "c" // No 'b' at end to cause backtracking
        
        val startTime = System.currentTimeMillis()
        val result = validator.compileWithTimeout(complexPattern, securityConfig.regexTimeoutMs)
        val endTime = System.currentTimeMillis()
        
        assertTrue("Should complete within timeout", endTime - startTime < securityConfig.regexTimeoutMs + 500)
        assertNull("Should fail compilation due to timeout", result)
    }

    @Test
    fun testSafePatternCompilation() {
        val safePatterns = listOf(
            "example\\.com",
            ".*\\.google\\.com",
            "[a-zA-Z0-9]+",
            "https?://.*"
        )
        
        safePatterns.forEach { pattern ->
            val result = validator.validateRegexSafety(pattern)
            assertTrue("Safe pattern should be valid: $pattern", result.isSafe)
            assertTrue("Should not detect ReDoS risk", result.riskLevel in listOf(
                RegexRiskLevel.LOW, 
                RegexRiskLevel.MEDIUM
            ))
        }
    }

    @Test
    fun testPerformanceOnDevice() {
        val testPattern = ".*\\.example\\.com"
        val testStrings = listOf(
            "subdomain.example.com",
            "www.example.com",
            "not.matching.org",
            "example.com"
        )
        
        val regex = validator.compileWithTimeout(testPattern, securityConfig.regexTimeoutMs)
        assertNotNull("Pattern should compile successfully", regex)
        
        // Test performance with multiple matches
        val startTime = System.currentTimeMillis()
        testStrings.forEach { testString ->
            regex?.matches(testString)
        }
        val endTime = System.currentTimeMillis()
        
        assertTrue("Pattern matching should be fast", endTime - startTime < 100)
    }

    @Test
    fun testComplexValidPatterns() {
        val complexPatterns = mapOf(
            "^https?://([a-zA-Z0-9-]+\\.)*example\\.com(/.*)?$" to true,
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$" to true,
            "^\\d{4}-\\d{2}-\\d{2}$" to true,
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" to true
        )
        
        complexPatterns.forEach { (pattern, shouldBeValid) ->
            val result = validator.validateRegexSafety(pattern)
            assertEquals("Pattern validation result: $pattern", shouldBeValid, result.isSafe)
        }
    }

    @Test
    fun testSecurityConfigIntegration() {
        val longPattern = "a".repeat(150)
        val result = validator.validateRegexSafety(longPattern)
        
        assertFalse("Long pattern should be rejected", result.isSafe)
        assertFalse("Should not be valid due to length", result.isValid)
        assertTrue("Should indicate high risk", result.riskLevel in listOf(
            RegexRiskLevel.HIGH, 
            RegexRiskLevel.CRITICAL
        ))
    }
}
