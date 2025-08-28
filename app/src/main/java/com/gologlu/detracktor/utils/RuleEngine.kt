package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.CleaningRule
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Simplified rule engine that compiles regex patterns and matches URLs.
 * Replaces the complex PatternType + RulePriority system with straightforward regex matching.
 */
class RuleEngine {
    
    data class CompiledPattern(
        val rule: CleaningRule,
        val hostPattern: Pattern,
        val paramPatterns: List<Pattern>
    )
    
    /**
     * Compile rules into regex patterns for efficient matching.
     * Rules are sorted by priority (lower number = higher priority).
     */
    fun compileRules(rules: List<CleaningRule>): List<CompiledPattern> {
        return rules
            .filter { it.enabled }
            .sortedBy { it.priority }
            .mapNotNull { rule ->
                try {
                    val hostPattern = Pattern.compile(rule.hostPattern, Pattern.CASE_INSENSITIVE)
                    val paramPatterns = rule.parameterPatterns.mapNotNull { paramPattern ->
                        try {
                            Pattern.compile(paramPattern, Pattern.CASE_INSENSITIVE)
                        } catch (e: PatternSyntaxException) {
                            null // Skip invalid parameter patterns
                        }
                    }
                    CompiledPattern(rule, hostPattern, paramPatterns)
                } catch (e: PatternSyntaxException) {
                    null // Skip rules with invalid host patterns
                }
            }
    }
    
    /**
     * Find matching rules for a given URL.
     * Returns list of rule IDs that match the URL's host.
     */
    fun matchRules(url: String, compiledRules: List<CompiledPattern>): List<String> {
        val host = extractHost(url) ?: return emptyList()
        
        return compiledRules
            .filter { it.hostPattern.matcher(host).find() }
            .map { it.rule.id }
    }
    
    /**
     * Get parameter patterns for matching rules.
     * Returns all parameter patterns from rules that match the URL.
     */
    fun getMatchingParameterPatterns(url: String, compiledRules: List<CompiledPattern>): List<Pattern> {
        val host = extractHost(url) ?: return emptyList()
        
        return compiledRules
            .filter { it.hostPattern.matcher(host).find() }
            .flatMap { it.paramPatterns }
    }
    
    /**
     * Extract host from URL.
     * Handles various URL formats including those with credentials.
     */
    private fun extractHost(url: String): String? {
        return try {
            val cleanUrl = url.trim()
            
            // Remove protocol
            val withoutProtocol = when {
                cleanUrl.startsWith("https://") -> cleanUrl.substring(8)
                cleanUrl.startsWith("http://") -> cleanUrl.substring(7)
                cleanUrl.startsWith("ftp://") -> cleanUrl.substring(6)
                else -> cleanUrl
            }
            
            // Remove credentials if present (user:pass@host)
            val withoutCredentials = if (withoutProtocol.contains("@")) {
                withoutProtocol.substringAfter("@")
            } else {
                withoutProtocol
            }
            
            // Extract host (everything before first slash, colon, or question mark)
            val host = withoutCredentials.split("/", "?", "#", ":")[0]
            
            if (host.isBlank()) null else host.lowercase()
        } catch (e: Exception) {
            null
        }
    }
}
