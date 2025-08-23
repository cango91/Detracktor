package com.gologlu.detracktor.utils

import android.util.Log
import com.gologlu.detracktor.data.CompiledRule
import com.gologlu.detracktor.data.CleaningRule
import com.gologlu.detracktor.data.PatternType
import com.gologlu.detracktor.data.PerformanceConfig
import android.util.LruCache
import java.util.concurrent.TimeUnit

/**
 * Rule compilation system with pattern caching and performance optimization.
 * Pre-compiles regex patterns and caches them for fast runtime matching.
 */
class RuleCompiler(private val config: PerformanceConfig) {
    
    companion object {
        private const val TAG = "RuleCompiler"
        private const val CACHE_EXPIRY_HOURS = 24L
    }
    
    // LRU cache for compiled rules
    private val compiledRulesCache: LruCache<String, List<CompiledRule>> = LruCache(config.maxCacheSize)
    
    // LRU cache for individual regex patterns
    private val patternCache: LruCache<String, Regex> = LruCache(config.maxCacheSize * 2)
    
    /**
     * Compile a list of rules with caching support.
     */
    fun compileRules(rules: List<CleaningRule>): List<CompiledRule> {
        if (!config.enablePatternCaching) {
            return compileRulesInternal(rules)
        }
        
        val cacheKey = generateCacheKey(rules)
        
        return compiledRulesCache.get(cacheKey) ?: run {
            val compiled = compileRulesInternal(rules)
            compiledRulesCache.put(cacheKey, compiled)
            compiled
        }
    }
    
    /**
     * Compile a single rule.
     */
    fun compileRule(rule: CleaningRule): CompiledRule {
        try {
            val compiledHostPattern = compileHostPattern(rule.hostPattern, rule.patternType)
            val compiledParamPatterns = compileParamPatterns(rule.params)
            val normalizedHostPattern = normalizeHostPattern(rule.hostPattern)
            val specificity = RuleSpecificity.calculate(rule)
            
            return CompiledRule(
                originalRule = rule,
                compiledHostPattern = compiledHostPattern,
                compiledParamPatterns = compiledParamPatterns,
                normalizedHostPattern = normalizedHostPattern,
                specificity = specificity
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compile rule: ${rule.hostPattern}", e)
            // Return a fallback compiled rule
            return CompiledRule(
                originalRule = rule,
                compiledHostPattern = null,
                compiledParamPatterns = emptyList(),
                normalizedHostPattern = rule.hostPattern.lowercase(),
                specificity = 0
            )
        }
    }
    
    /**
     * Compile host pattern based on pattern type.
     */
    fun compileHostPattern(pattern: String, type: PatternType): Regex? {
        if (type == PatternType.EXACT) {
            return null // Exact matches don't need regex
        }
        
        val cacheKey = "${type.name}:$pattern"
        
        return if (config.enablePatternCaching) {
            patternCache.get(cacheKey) ?: run {
                val compiled = compileHostPatternInternal(pattern, type)
                if (compiled != null) {
                    patternCache.put(cacheKey, compiled)
                }
                compiled
            }
        } else {
            compileHostPatternInternal(pattern, type)
        }
    }
    
    /**
     * Compile parameter patterns into regex list.
     */
    fun compileParamPatterns(params: List<String>): List<Regex> {
        return params.mapNotNull { param ->
            try {
                val cacheKey = "param:$param"
                
                if (config.enablePatternCaching) {
                    patternCache.get(cacheKey) ?: run {
                        val compiled = compileParamPattern(param)
                        patternCache.put(cacheKey, compiled)
                        compiled
                    }
                } else {
                    compileParamPattern(param)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to compile param pattern: $param", e)
                null
            }
        }
    }
    
    /**
     * Invalidate all caches.
     */
    fun invalidateCache() {
        compiledRulesCache.evictAll()
        patternCache.evictAll()
        Log.d(TAG, "Pattern caches invalidated")
    }
    
    /**
     * Get cached compiled rules if available.
     */
    fun getCachedRules(): List<CompiledRule>? {
        // This would require storing the cache key, which we don't have here
        // In practice, this would be called with a specific cache key
        return null
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "compiledRulesCache" to mapOf(
                "size" to compiledRulesCache.size(),
                "maxSize" to compiledRulesCache.maxSize(),
                "hitCount" to compiledRulesCache.hitCount(),
                "missCount" to compiledRulesCache.missCount()
            ),
            "patternCache" to mapOf(
                "size" to patternCache.size(),
                "maxSize" to patternCache.maxSize(),
                "hitCount" to patternCache.hitCount(),
                "missCount" to patternCache.missCount()
            )
        )
    }
    
    // Private implementation methods
    
    private fun compileRulesInternal(rules: List<CleaningRule>): List<CompiledRule> {
        val compiledRules = rules.map { compileRule(it) }
        return RuleSpecificity.sortBySpecificity(compiledRules)
    }
    
    private fun compileHostPatternInternal(pattern: String, type: PatternType): Regex? {
        return when (type) {
            PatternType.EXACT -> null
            PatternType.WILDCARD -> compileWildcardPattern(pattern)
            PatternType.REGEX -> Regex(pattern, RegexOption.IGNORE_CASE)
            PatternType.PATH_PATTERN -> compilePathPattern(pattern)
        }
    }
    
    private fun compileWildcardPattern(pattern: String): Regex {
        // Convert wildcard pattern to regex
        val regexPattern = pattern
            .replace(".", "\\.")  // Escape dots
            .replace("*", ".*")   // Convert * to .*
            .replace("?", ".")    // Convert ? to .
        
        return Regex("^$regexPattern$", RegexOption.IGNORE_CASE)
    }
    
    private fun compilePathPattern(pattern: String): Regex {
        // Handle path-specific patterns like "example.com/path/*"
        val escapedPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .replace("/", "\\/")
        
        return Regex("^$escapedPattern$", RegexOption.IGNORE_CASE)
    }
    
    private fun compileParamPattern(param: String): Regex {
        // Handle parameter patterns with wildcards
        val regexPattern = param
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        
        return Regex("^$regexPattern$", RegexOption.IGNORE_CASE)
    }
    
    private fun normalizeHostPattern(hostPattern: String): String {
        // Normalize host pattern for consistent matching
        return hostPattern.lowercase().trim()
    }
    
    private fun generateCacheKey(rules: List<CleaningRule>): String {
        // Generate a cache key based on rule content
        val ruleHashes = rules.map { rule ->
            "${rule.hostPattern}:${rule.patternType}:${rule.priority}:${rule.params.joinToString(",")}"
        }
        return ruleHashes.joinToString("|").hashCode().toString()
    }
}
