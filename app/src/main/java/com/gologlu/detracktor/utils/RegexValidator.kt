package com.gologlu.detracktor.utils

import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * Custom exception thrown when regex compilation or execution times out
 */
class RegexTimeoutException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Result of regex pattern validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val isReDoSVulnerable: Boolean = false
)

/**
 * Utility class for validating regex patterns against ReDoS (Regular Expression Denial of Service) vulnerabilities
 * and providing safe regex compilation with timeout protection.
 */
object RegexValidator {
    private const val TAG = "RegexValidator"
    private const val DEFAULT_TIMEOUT_MS = 1000L
    
    // Known ReDoS vulnerability patterns
    private val redosPatterns = listOf(
        // Nested quantifiers - (a+)+ pattern
        Regex("""\([^)]*\+[^)]*\)\+"""),
        Regex("""\([^)]*\*[^)]*\)\+"""),
        Regex("""\([^)]*\+[^)]*\)\*"""),
        Regex("""\([^)]*\*[^)]*\)\*"""),
        
        // Alternation with overlap - (a|a)* pattern
        Regex("""\([^|)]*\|[^|)]*\)\*"""),
        Regex("""\([^|)]*\|[^|)]*\)\+"""),
        
        // Complex nested structures
        Regex("""\([^)]*\([^)]*[\+\*][^)]*\)[^)]*\)[\+\*]"""),
        
        // Exponential alternation patterns
        Regex("""\([^)]*\|[^)]*\|[^)]*\)[\+\*]"""),
        
        // Overlapping character classes with quantifiers
        Regex("""\[[^\]]*\][\+\*]\[[^\]]*\][\+\*]""")
    )
    
    /**
     * Validates a regex pattern for security vulnerabilities and syntax correctness
     * 
     * @param pattern The regex pattern to validate
     * @param timeoutMs Maximum time allowed for validation (default: 1000ms)
     * @return ValidationResult containing validation status and potential issues
     */
    fun validatePattern(pattern: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ValidationResult {
        if (pattern.isBlank()) {
            return ValidationResult(false, "Pattern cannot be empty")
        }
        
        // Check for ReDoS vulnerability patterns
        val isReDoSVulnerable = isReDoSVulnerable(pattern)
        if (isReDoSVulnerable) {
            Log.w(TAG, "ReDoS vulnerability detected in pattern: $pattern")
            return ValidationResult(
                false, 
                "Pattern contains potential ReDoS vulnerability (nested quantifiers or overlapping alternation)",
                true
            )
        }
        
        // Test compilation with timeout
        return try {
            val regex = compileWithTimeout(pattern, timeoutMs)
            if (regex != null) {
                ValidationResult(true)
            } else {
                ValidationResult(false, "Pattern compilation timed out after ${timeoutMs}ms")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pattern validation failed: $pattern", e)
            ValidationResult(false, "Invalid regex pattern: ${e.message}")
        }
    }
    
    /**
     * Compiles a regex pattern with timeout protection
     * 
     * @param pattern The regex pattern to compile
     * @param timeoutMs Maximum time allowed for compilation
     * @param options Regex options to apply
     * @return Compiled Regex object or null if timeout/error occurs
     */
    fun compileWithTimeout(
        pattern: String, 
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        options: Set<RegexOption> = emptySet()
    ): Regex? {
        return try {
            runBlocking {
                withTimeout(timeoutMs.milliseconds) {
                    Regex(pattern, options)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Regex compilation timed out for pattern: $pattern")
            throw RegexTimeoutException("Regex compilation timed out after ${timeoutMs}ms", e)
        } catch (e: Exception) {
            Log.w(TAG, "Regex compilation failed for pattern: $pattern", e)
            null
        }
    }
    
    /**
     * Checks if a regex pattern is vulnerable to ReDoS attacks
     * 
     * @param pattern The regex pattern to check
     * @return true if the pattern contains known ReDoS vulnerability patterns
     */
    fun isReDoSVulnerable(pattern: String): Boolean {
        return redosPatterns.any { vulnerabilityPattern ->
            try {
                vulnerabilityPattern.containsMatchIn(pattern)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking ReDoS pattern: $pattern", e)
                false
            }
        }
    }
    
    /**
     * Tests a compiled regex against a test string with timeout protection
     * 
     * @param regex The compiled regex to test
     * @param testString The string to test against
     * @param timeoutMs Maximum time allowed for the test
     * @return true if the regex matches, false if no match or timeout
     */
    fun testWithTimeout(
        regex: Regex, 
        testString: String, 
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Boolean {
        return try {
            runBlocking {
                withTimeout(timeoutMs.milliseconds) {
                    regex.containsMatchIn(testString)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Regex test timed out for pattern: ${regex.pattern}")
            throw RegexTimeoutException("Regex test timed out after ${timeoutMs}ms", e)
        } catch (e: Exception) {
            Log.w(TAG, "Regex test failed for pattern: ${regex.pattern}", e)
            false
        }
    }
    
    /**
     * Provides safe regex matching with automatic fallback for problematic patterns
     * 
     * @param pattern The regex pattern
     * @param input The input string to match against
     * @param timeoutMs Maximum time allowed for matching
     * @return MatchResult containing success status and match information
     */
    fun safeMatch(
        pattern: String, 
        input: String, 
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): MatchResult {
        val validation = validatePattern(pattern, timeoutMs)
        if (!validation.isValid) {
            return MatchResult(false, validation.errorMessage)
        }
        
        return try {
            val regex = compileWithTimeout(pattern, timeoutMs)
                ?: return MatchResult(false, "Failed to compile pattern")
            
            val matches = testWithTimeout(regex, input, timeoutMs)
            MatchResult(true, null, matches)
        } catch (e: RegexTimeoutException) {
            MatchResult(false, e.message, false)
        } catch (e: Exception) {
            Log.w(TAG, "Safe match failed for pattern: $pattern", e)
            MatchResult(false, "Match operation failed: ${e.message}", false)
        }
    }
    
    /**
     * Result of a safe regex matching operation
     */
    data class MatchResult(
        val success: Boolean,
        val errorMessage: String? = null,
        val matches: Boolean = false
    )
}
