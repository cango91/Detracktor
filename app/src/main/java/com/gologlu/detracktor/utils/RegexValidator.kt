package com.gologlu.detracktor.utils

import kotlinx.coroutines.*
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Result of regex validation containing safety metrics and compilation status
 */
data class RegexValidationResult(
    val isValid: Boolean,
    val isSafe: Boolean,
    val errorMessage: String? = null,
    val compiledRegex: Regex? = null,
    val riskLevel: RegexRiskLevel = RegexRiskLevel.UNKNOWN,
    val suggestions: List<String> = emptyList()
)

/**
 * Risk levels for regex patterns
 */
enum class RegexRiskLevel {
    LOW,        // Safe pattern with minimal risk
    MEDIUM,     // Potentially risky but acceptable
    HIGH,       // High risk of ReDoS, should be avoided
    CRITICAL,   // Very high risk, should be rejected
    UNKNOWN     // Unable to determine risk level
}

/**
 * Validates regex patterns for ReDoS vulnerabilities and provides safe compilation
 * with timeout protection to prevent application freezing.
 */
class RegexValidator {
    
    companion object {
        // Default timeout for regex operations (in milliseconds)
        private const val DEFAULT_TIMEOUT_MS = 1000L
        
        // Maximum allowed pattern length
        private const val MAX_PATTERN_LENGTH = 1000
        
        // Patterns that indicate potential ReDoS vulnerabilities
        private val REDOS_RISK_PATTERNS = listOf(
            // Nested quantifiers
            Pattern.compile("\\([^)]*[*+?].*\\)[*+?]"),
            // Alternation with overlapping patterns
            Pattern.compile("\\([^|]*\\|[^)]*\\)[*+?]"),
            // Catastrophic backtracking patterns
            Pattern.compile("\\([^)]*[*+?][^)]*\\)[*+?]"),
            // Multiple consecutive quantifiers
            Pattern.compile("[*+?]{2,}"),
            // Nested groups with quantifiers
            Pattern.compile("\\(.*\\([^)]*[*+?].*\\).*\\)[*+?]")
        )
        
        // Safe pattern alternatives
        private val SAFE_ALTERNATIVES = mapOf(
            ".*" to "[\\s\\S]*?",
            ".+" to "[\\s\\S]+?",
            "(.*)" to "([\\s\\S]*?)",
            "(.+)" to "([\\s\\S]+?)"
        )
    }
    
    /**
     * Validates a regex pattern for safety and ReDoS vulnerabilities
     */
    fun validateRegexSafety(pattern: String): RegexValidationResult {
        // Basic validation
        if (pattern.isBlank()) {
            return RegexValidationResult(
                isValid = false,
                isSafe = false,
                errorMessage = "Pattern cannot be empty",
                riskLevel = RegexRiskLevel.UNKNOWN
            )
        }
        
        if (pattern.length > MAX_PATTERN_LENGTH) {
            return RegexValidationResult(
                isValid = false,
                isSafe = false,
                errorMessage = "Pattern too long (max $MAX_PATTERN_LENGTH characters)",
                riskLevel = RegexRiskLevel.HIGH
            )
        }
        
        // Check for syntax errors
        try {
            Pattern.compile(pattern)
        } catch (e: PatternSyntaxException) {
            return RegexValidationResult(
                isValid = false,
                isSafe = false,
                errorMessage = "Invalid regex syntax: ${e.message}",
                riskLevel = RegexRiskLevel.UNKNOWN
            )
        }
        
        // Analyze for ReDoS risks
        val riskLevel = analyzeReDoSRisk(pattern)
        val suggestions = generateSuggestions(pattern, riskLevel)
        
        // Attempt safe compilation
        val compiledRegex = compileWithTimeout(pattern, DEFAULT_TIMEOUT_MS)
        
        return RegexValidationResult(
            isValid = true,
            isSafe = riskLevel in listOf(RegexRiskLevel.LOW, RegexRiskLevel.MEDIUM),
            compiledRegex = compiledRegex,
            riskLevel = riskLevel,
            suggestions = suggestions,
            errorMessage = if (compiledRegex == null) "Pattern compilation timed out" else null
        )
    }
    
    /**
     * Safely compiles a regex pattern with timeout protection
     */
    fun compileWithTimeout(pattern: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Regex? {
        return try {
            runBlocking {
                withTimeout(timeoutMs) {
                    // Test the pattern with a simple string first
                    val testRegex = Regex(pattern, RegexOption.IGNORE_CASE)
                    
                    // Perform a quick test to catch obvious ReDoS patterns
                    val testString = "a".repeat(100)
                    testRegex.find(testString)
                    
                    testRegex
                }
            }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Analyzes a pattern for ReDoS risk level
     */
    private fun analyzeReDoSRisk(pattern: String): RegexRiskLevel {
        var riskScore = 0
        
        // Check for known dangerous patterns
        REDOS_RISK_PATTERNS.forEach { riskPattern ->
            if (riskPattern.matcher(pattern).find()) {
                riskScore += 2
            }
        }
        
        // Additional risk factors
        val quantifierCount = pattern.count { it in "*+?" }
        val groupCount = pattern.count { it == '(' }
        val alternationCount = pattern.count { it == '|' }
        
        // Scoring based on complexity
        riskScore += when {
            quantifierCount > 5 -> 2
            quantifierCount > 2 -> 1
            else -> 0
        }
        
        riskScore += when {
            groupCount > 3 -> 2
            groupCount > 1 -> 1
            else -> 0
        }
        
        riskScore += when {
            alternationCount > 2 -> 1
            else -> 0
        }
        
        // Check for nested structures
        if (hasNestedQuantifiers(pattern)) {
            riskScore += 3
        }
        
        return when (riskScore) {
            0 -> RegexRiskLevel.LOW
            1, 2 -> RegexRiskLevel.MEDIUM
            3, 4 -> RegexRiskLevel.HIGH
            else -> RegexRiskLevel.CRITICAL
        }
    }
    
    /**
     * Checks for nested quantifiers which are high risk for ReDoS
     */
    private fun hasNestedQuantifiers(pattern: String): Boolean {
        var depth = 0
        var hasQuantifierAtDepth = false
        
        for (i in pattern.indices) {
            when (pattern[i]) {
                '(' -> {
                    depth++
                    hasQuantifierAtDepth = false
                }
                ')' -> {
                    if (i + 1 < pattern.length && pattern[i + 1] in "*+?" && hasQuantifierAtDepth) {
                        return true
                    }
                    depth--
                }
                '*', '+', '?' -> {
                    if (depth > 0) {
                        hasQuantifierAtDepth = true
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Generates suggestions for improving regex safety
     */
    private fun generateSuggestions(pattern: String, riskLevel: RegexRiskLevel): List<String> {
        val suggestions = mutableListOf<String>()
        
        when (riskLevel) {
            RegexRiskLevel.HIGH, RegexRiskLevel.CRITICAL -> {
                suggestions.add("Consider using more specific patterns instead of broad quantifiers")
                suggestions.add("Avoid nested quantifiers like (a+)+ or (a*)*")
                suggestions.add("Use possessive quantifiers (++, *+) if supported")
                
                // Check for specific problematic patterns and suggest alternatives
                SAFE_ALTERNATIVES.forEach { (dangerous, safe) ->
                    if (pattern.contains(dangerous)) {
                        suggestions.add("Replace '$dangerous' with '$safe' for better performance")
                    }
                }
            }
            RegexRiskLevel.MEDIUM -> {
                suggestions.add("Pattern appears safe but consider testing with large inputs")
                suggestions.add("Monitor performance if used frequently")
            }
            RegexRiskLevel.LOW -> {
                // No suggestions needed for low-risk patterns
            }
            RegexRiskLevel.UNKNOWN -> {
                suggestions.add("Unable to analyze pattern safety - proceed with caution")
            }
        }
        
        return suggestions
    }
    
    /**
     * Creates a safer version of a regex pattern by applying common fixes
     */
    fun createSaferPattern(pattern: String): String {
        var saferPattern = pattern
        
        // Apply safe alternatives
        SAFE_ALTERNATIVES.forEach { (dangerous, safe) ->
            saferPattern = saferPattern.replace(dangerous, safe)
        }
        
        // Remove obvious problematic patterns
        saferPattern = saferPattern.replace(Regex("\\*\\+|\\+\\*|\\?\\+|\\+\\?"), "+")
        
        return saferPattern
    }
    
    /**
     * Tests a compiled regex against various inputs to detect potential ReDoS
     */
    fun testRegexPerformance(regex: Regex, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val testInputs = listOf(
            "a".repeat(100),
            "a".repeat(1000) + "b",
            "x".repeat(50) + "y".repeat(50),
            "abcdefghijklmnopqrstuvwxyz".repeat(10),
            ""
        )
        
        return testInputs.all { input ->
            try {
                runBlocking {
                    withTimeout(timeoutMs) {
                        regex.find(input)
                        true
                    }
                }
            } catch (e: TimeoutCancellationException) {
                false
            } catch (e: Exception) {
                true // Other exceptions are acceptable
            }
        }
    }
}
