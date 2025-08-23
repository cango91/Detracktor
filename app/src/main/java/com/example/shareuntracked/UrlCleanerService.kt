package com.example.shareuntracked

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.shareuntracked.data.AppConfig
import com.example.shareuntracked.data.CleaningResult
import com.example.shareuntracked.data.CleaningRule
import java.net.URL

/**
 * Core service for URL cleaning operations
 */
class UrlCleanerService(private val context: Context) {
    
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
     * Main URL cleaning logic
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
                // Apply custom rules
                cleanUrlWithRules(uri, config.rules)
            }
        } catch (e: Exception) {
            // If parsing fails, return original URL
            url
        }
    }
    
    /**
     * Clean URL using custom rules
     */
    private fun cleanUrlWithRules(uri: Uri, rules: List<CleaningRule>): String {
        val host = uri.host ?: return uri.toString()
        val queryParams = uri.queryParameterNames.toMutableSet()
        val paramsToRemove = mutableSetOf<String>()
        
        // Find applicable rules for this host
        val applicableRules = rules.filter { rule ->
            matchesHostPattern(host, rule.hostPattern)
        }
        
        // Collect parameters to remove based on rules
        for (rule in applicableRules) {
            for (paramPattern in rule.params) {
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
        
        return builder.build().toString()
    }
    
    /**
     * Check if host matches a pattern (supports wildcards)
     */
    private fun matchesHostPattern(host: String, pattern: String): Boolean {
        return when {
            pattern == "*" -> true
            pattern.startsWith("*.") -> {
                val domain = pattern.substring(2)
                host == domain || host.endsWith(".$domain")
            }
            else -> host == pattern
        }
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
}
