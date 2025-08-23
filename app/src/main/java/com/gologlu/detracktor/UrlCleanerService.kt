package com.gologlu.detracktor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.gologlu.detracktor.data.*
import java.net.URL

/**
 * URL cleaning service with hierarchical pattern matching and host normalization.
 */
class UrlCleanerService(private val context: Context) {
    
    companion object {
        private const val TAG = "UrlCleanerService"
    }
    
    private val configManager = ConfigManager(context)
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    /**
     * Process an intent that may contain a URL to clean
     */
    fun processIntent(intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (url != null && isValidHttpUrl(url)) {
            val result = cleanAndCopyUrl(url)
            showToast(result, url)
        } else {
            // No URL in intent, try clipboard
            val result = cleanClipboardUrl()
            showToast(result)
        }
    }
    
    /**
     * Clean URL from clipboard and copy back if changed
     */
    fun cleanClipboardUrl(): CleaningResult {
        val clipData = clipboardManager.primaryClip
        if (clipData == null || clipData.itemCount == 0) {
            return CleaningResult.CLIPBOARD_EMPTY
        }
        
        val clipText = clipData.getItemAt(0).text?.toString()
        if (clipText.isNullOrEmpty() || !isValidHttpUrl(clipText)) {
            return CleaningResult.CLIPBOARD_EMPTY
        }
        
        return cleanAndCopyUrl(clipText)
    }
    
    /**
     * Clean a URL and copy to clipboard if changed
     */
    private fun cleanAndCopyUrl(originalUrl: String): CleaningResult {
        val cleanedUrl = cleanUrl(originalUrl)
        
        return if (cleanedUrl != originalUrl) {
            copyToClipboard(cleanedUrl)
            CleaningResult.CLEANED_AND_COPIED
        } else {
            CleaningResult.NO_CHANGE
        }
    }
    
    /**
     * Main URL cleaning logic with hierarchical matching
     */
    fun cleanUrl(url: String): String {
        if (!isValidHttpUrl(url)) {
            return url
        }
        
        return try {
            val uri = Uri.parse(url)
            val config = configManager.loadConfig()
            
            if (config.removeAllParams) {
                // Remove all query parameters
                uri.buildUpon().clearQuery().build().toString()
            } else {
                // Apply hierarchical rule matching
                cleanUrlWithHierarchicalRules(uri, configManager.getCompiledRules())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean URL: $url", e)
            // If parsing fails, return original URL
            url
        }
    }
    
    /**
     * Clean URL using hierarchical rule matching with host normalization
     */
    private fun cleanUrlWithHierarchicalRules(uri: Uri, compiledRules: List<CompiledRule>): String {
        val originalHost = uri.host ?: return uri.toString()
        
        // Normalize the host for consistent matching
        val normalizedUri = normalizeUrlHost(uri)
        val normalizedHost = normalizedUri.host ?: return uri.toString()
        
        // Find the best matching rule using hierarchical matching
        val bestRule = findBestMatchingRule(normalizedHost, compiledRules)
        
        return if (bestRule != null) {
            // Apply the best matching rule
            applyRuleToParameters(normalizedUri, bestRule)
        } else {
            // No matching rule found, return original URL
            uri.toString()
        }
    }
    
    /**
     * Normalize URL host using the host normalizer
     */
    private fun normalizeUrlHost(uri: Uri): Uri {
        val host = uri.host ?: return uri
        val scheme = uri.scheme ?: "https"
        
        val hostNormalizer = configManager.hostNormalizer
        val normalizedHost = hostNormalizer.normalizeHost(host, scheme)
        
        return uri.buildUpon()
            .authority(normalizedHost.normalized)
            .build()
    }
    
    /**
     * Find the best matching rule using hierarchical specificity
     */
    fun findBestMatchingRule(normalizedHost: String, rules: List<CompiledRule>): CompiledRule? {
        // Rules are already sorted by specificity (most specific first)
        for (rule in rules) {
            if (!rule.originalRule.enabled) continue
            
            if (matchesCompiledRule(normalizedHost, rule)) {
                Log.d(TAG, "Matched rule: ${rule.originalRule.hostPattern} (specificity: ${rule.specificity})")
                return rule
            }
        }
        
        Log.d(TAG, "No matching rule found for host: $normalizedHost")
        return null
    }
    
    /**
     * Check if a host matches a compiled rule
     */
    fun matchesCompiledRule(host: String, rule: CompiledRule): Boolean {
        return when (rule.originalRule.patternType) {
            PatternType.EXACT -> {
                host.equals(rule.normalizedHostPattern, ignoreCase = true)
            }
            PatternType.WILDCARD -> {
                rule.compiledHostPattern?.matches(host) ?: false
            }
            PatternType.REGEX -> {
                rule.compiledHostPattern?.matches(host) ?: false
            }
            PatternType.PATH_PATTERN -> {
                // For path patterns, we need to match against the full URL
                // This is a simplified implementation - in practice, you'd want to
                // pass the full URL path for matching
                rule.compiledHostPattern?.matches(host) ?: false
            }
        }
    }
    
    /**
     * Apply a compiled rule to URL parameters
     */
    private fun applyRuleToParameters(uri: Uri, rule: CompiledRule): String {
        val queryParams = uri.queryParameterNames.toMutableSet()
        val paramsToRemove = mutableSetOf<String>()
        
        // Use compiled parameter patterns for efficient matching
        for (paramPattern in rule.compiledParamPatterns) {
            paramsToRemove.addAll(queryParams.filter { param ->
                paramPattern.matches(param)
            })
        }
        
        // Also handle simple string patterns that weren't compiled to regex
        for (paramPattern in rule.originalRule.params) {
            if (paramPattern.endsWith("*")) {
                // Wildcard pattern
                val prefix = paramPattern.dropLast(1)
                paramsToRemove.addAll(queryParams.filter { it.startsWith(prefix) })
            } else {
                // Exact match
                if (queryParams.contains(paramPattern)) {
                    paramsToRemove.add(paramPattern)
                }
            }
        }
        
        // Build new URI without the specified parameters
        val builder = uri.buildUpon().clearQuery()
        for (param in queryParams) {
            if (!paramsToRemove.contains(param)) {
                val values = uri.getQueryParameters(param)
                for (value in values) {
                    builder.appendQueryParameter(param, value)
                }
            }
        }
        
        val result = builder.build().toString()
        
        if (paramsToRemove.isNotEmpty()) {
            Log.d(TAG, "Removed parameters: ${paramsToRemove.joinToString(", ")} using rule: ${rule.originalRule.hostPattern}")
        }
        
        return result
    }
    
    /**
     * Calculate rule specificity for debugging
     */
    fun calculateRuleSpecificity(rule: CleaningRule): Int {
        return com.gologlu.detracktor.utils.RuleSpecificity.calculate(rule)
    }
    
    /**
     * Validate if string is a valid HTTP/HTTPS URL
     */
    private fun isValidHttpUrl(url: String): Boolean {
        return try {
            val parsedUrl = URL(url)
            parsedUrl.protocol == "http" || parsedUrl.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("Cleaned URL", text)
        clipboardManager.setPrimaryClip(clip)
    }
    
    /**
     * Show appropriate toast message based on result
     */
    private fun showToast(result: CleaningResult, originalUrl: String? = null) {
        val message = when (result) {
            CleaningResult.CLIPBOARD_EMPTY -> "Clipboard empty"
            CleaningResult.NO_CHANGE -> "No change"
            CleaningResult.CLEANED_AND_COPIED -> "Cleaned → copied"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Get service statistics for debugging and monitoring
     */
    fun getServiceStats(): Map<String, Any> {
        val configStats = configManager.getConfigStats()
        val compiledRules = configManager.getCompiledRules()
        
        return mapOf(
            "configStats" to configStats,
            "compiledRulesCount" to compiledRules.size,
            "enabledRulesCount" to compiledRules.count { it.originalRule.enabled },
            "rulesByPriority" to compiledRules.groupBy { it.originalRule.priority }.mapValues { it.value.size },
            "rulesByPatternType" to compiledRules.groupBy { it.originalRule.patternType }.mapValues { it.value.size }
        )
    }
    
    /**
     * Test URL cleaning with detailed logging (for debugging)
     */
    fun testUrlCleaning(url: String): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val originalUrl = url
        val cleanedUrl = cleanUrl(url)
        val endTime = System.currentTimeMillis()
        
        val uri = Uri.parse(url)
        val host = uri.host ?: ""
        val normalizedHost = if (host.isNotEmpty()) {
            configManager.hostNormalizer.normalizeHost(host).normalized
        } else {
            ""
        }
        
        val matchingRule = if (normalizedHost.isNotEmpty()) {
            findBestMatchingRule(normalizedHost, configManager.getCompiledRules())
        } else {
            null
        }
        
        return mapOf(
            "originalUrl" to originalUrl,
            "cleanedUrl" to cleanedUrl,
            "changed" to (originalUrl != cleanedUrl),
            "processingTimeMs" to (endTime - startTime),
            "originalHost" to host,
            "normalizedHost" to normalizedHost,
            "matchingRule" to (matchingRule?.originalRule?.hostPattern ?: "none"),
            "ruleSpecificity" to (matchingRule?.specificity ?: 0),
            "removedParams" to getRemovedParameters(uri, Uri.parse(cleanedUrl))
        )
    }
    
    private fun getRemovedParameters(originalUri: Uri, cleanedUri: Uri): List<String> {
        val originalParams = originalUri.queryParameterNames
        val cleanedParams = cleanedUri.queryParameterNames
        return originalParams.subtract(cleanedParams).toList()
    }
}
