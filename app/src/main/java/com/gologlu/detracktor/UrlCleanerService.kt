package com.gologlu.detracktor

import android.content.Context
import com.gologlu.detracktor.data.UrlAnalysis
import com.gologlu.detracktor.utils.RuleEngine
import com.gologlu.detracktor.utils.UrlAnalyzer
import android.util.LruCache

/**
 * Simplified URL cleaning service using the new analysis system.
 * Replaces the complex privacy analysis with focused URL cleaning functionality.
 */
class UrlCleanerService(private val context: Context) {
    
    private val configManager = ConfigManager(context)
    private val ruleEngine = RuleEngine()
    private val urlAnalyzer = UrlAnalyzer()
    
    // Cache compiled rules for performance
    private var compiledRulesCache: List<RuleEngine.CompiledPattern>? = null
    private var lastConfigVersion: Int = -1
    
    // Cache analysis results for performance
    private val analysisCache = LruCache<String, UrlAnalysis>(100)
    
    /**
     * Clean a URL by removing tracking parameters based on configured rules.
     */
    fun cleanUrl(url: String): String {
        val analysis = analyzeClipboardContent(url)
        return analysis.cleanedUrl
    }
    
    /**
     * Analyze clipboard content and return detailed analysis.
     * This is the main entry point for URL analysis.
     */
    fun analyzeClipboardContent(url: String): UrlAnalysis {
        // Check cache first
        val cached = analysisCache.get(url)
        if (cached != null) {
            return cached
        }
        
        val compiledRules = getCompiledRules()
        val matchingRuleIds = ruleEngine.matchRules(url, compiledRules)
        val matchingParameterPatterns = ruleEngine.getMatchingParameterPatterns(url, compiledRules)
        
        val analysis = urlAnalyzer.analyzeUrl(url, matchingParameterPatterns)
        
        // Update with matching rule IDs
        val finalAnalysis = analysis.copy(matchingRules = matchingRuleIds)
        
        // Cache the result
        analysisCache.put(url, finalAnalysis)
        
        return finalAnalysis
    }
    
    /**
     * Check if a URL contains tracking parameters that can be cleaned.
     */
    fun hasTrackingParameters(url: String): Boolean {
        val analysis = analyzeClipboardContent(url)
        return analysis.originalUrl != analysis.cleanedUrl
    }
    
    /**
     * Check if a URL contains embedded credentials.
     */
    fun hasEmbeddedCredentials(url: String): Boolean {
        val analysis = analyzeClipboardContent(url)
        return analysis.hasEmbeddedCredentials
    }
    
    /**
     * Get compiled rules, using cache when possible.
     */
    private fun getCompiledRules(): List<RuleEngine.CompiledPattern> {
        val config = configManager.loadConfig()
        
        // Check if we need to recompile rules
        if (compiledRulesCache == null || lastConfigVersion != config.version) {
            compiledRulesCache = ruleEngine.compileRules(config.rules)
            lastConfigVersion = config.version
            
            // Clear analysis cache when rules change
            analysisCache.evictAll()
        }
        
        return compiledRulesCache ?: emptyList()
    }
    
    /**
     * Clear all caches (useful when configuration changes).
     */
    fun clearCaches() {
        compiledRulesCache = null
        analysisCache.evictAll()
        lastConfigVersion = -1
    }
    
    /**
     * Get statistics about the service state.
     */
    fun getServiceStats(): ServiceStats {
        val config = configManager.loadConfig()
        val enabledRules = config.rules.count { it.enabled }
        val totalRules = config.rules.size
        val cacheSize = analysisCache.size()
        
        return ServiceStats(
            totalRules = totalRules,
            enabledRules = enabledRules,
            cacheSize = cacheSize,
            removeAllParams = config.removeAllParams
        )
    }
    
    data class ServiceStats(
        val totalRules: Int,
        val enabledRules: Int,
        val cacheSize: Int,
        val removeAllParams: Boolean
    )
}
