package com.gologlu.detracktor.utils

import android.util.Log
import com.gologlu.detracktor.data.ClipboardAnalysis

/**
 * Utility class for filtering sensitive clipboard content and providing privacy-conscious
 * content display decisions for the Detracktor app.
 */
object ClipboardContentFilter {
    private const val TAG = "ClipboardContentFilter"
    
    // Patterns that indicate potentially sensitive content
    private val sensitivePatterns = listOf(
        // Password-like patterns
        Regex("""(?i)password\s*[:=]\s*\S+"""),
        Regex("""(?i)pass\s*[:=]\s*\S+"""),
        Regex("""(?i)pwd\s*[:=]\s*\S+"""),
        
        // API keys and tokens
        Regex("""(?i)api[_-]?key\s*[:=]\s*\S+"""),
        Regex("""(?i)token\s*[:=]\s*\S+"""),
        Regex("""(?i)secret\s*[:=]\s*\S+"""),
        Regex("""(?i)auth[_-]?key\s*[:=]\s*\S+"""),
        
        // Credit card patterns (basic detection)
        Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b"""),
        
        // Social security numbers (US format)
        Regex("""\b\d{3}-\d{2}-\d{4}\b"""),
        
        // Email addresses in sensitive contexts
        Regex("""(?i)email\s*[:=]\s*[^\s@]+@[^\s@]+\.[^\s@]+"""),
        
        // Phone numbers
        Regex("""\b\+?1?[-.\s]?\(?[0-9]{3}\)?[-.\s]?[0-9]{3}[-.\s]?[0-9]{4}\b"""),
        
        // Base64 encoded content (potential secrets)
        Regex("""[A-Za-z0-9+/]{20,}={0,2}"""),
        
        // JWT tokens
        Regex("""eyJ[A-Za-z0-9+/=]+\.[A-Za-z0-9+/=]+\.[A-Za-z0-9+/=]*"""),
        
        // Private keys
        Regex("""-----BEGIN [A-Z ]+PRIVATE KEY-----"""),
        
        // Database connection strings
        Regex("""(?i)(jdbc|mongodb|mysql|postgresql)://[^\s]+"""),
        
        // Generic key-value pairs that might be sensitive
        Regex("""(?i)(client[_-]?secret|private[_-]?key|access[_-]?token)\s*[:=]\s*\S+""")
    )
    
    // Patterns for common non-sensitive content that should be displayed
    private val safePatternsForNonUri = listOf(
        // Very simple text (single words only, no complex phrases)
        Regex("""^[a-zA-Z]{1,20}$"""),                    // Single words only
        Regex("""^[a-zA-Z]{1,15}\s[a-zA-Z]{1,15}$"""),    // Two simple words only (like "Hello world")
        
        // File paths (generally safe to display)
        Regex("""^[a-zA-Z]:[\\\/][^<>:"|?*\n\r]*$"""),
        Regex("""^\/[^<>:"|?*\n\r]*$"""),
        
        // Simple numbers or codes without context
        Regex("""^\d{1,10}$"""),
        
        // Common application names or simple identifiers
        Regex("""^[a-zA-Z0-9._-]{1,50}$""")
    )
    
    /**
     * Determines whether clipboard content should be displayed in the UI based on privacy considerations
     * 
     * @param analysis The clipboard analysis result
     * @return true if content is safe to display, false if it should be hidden for privacy
     */
    fun shouldDisplayContent(analysis: ClipboardAnalysis): Boolean {
        // Always display valid URLs as they are the primary use case
        if (analysis.isValidUrl) {
            return true
        }
        
        val content = analysis.originalUrl
        
        // Don't display empty or very long content
        if (content.isBlank() || content.length > 500) {
            Log.d(TAG, "Content filtered: empty or too long")
            return false
        }
        
        // Check for sensitive patterns
        if (containsSensitiveContent(content)) {
            Log.d(TAG, "Content filtered: contains sensitive patterns")
            return false
        }
        
        // Check if content matches safe patterns for non-URI content
        if (isSafeNonUriContent(content)) {
            Log.d(TAG, "Content approved: matches safe non-URI patterns")
            return true
        }
        
        // Default to not displaying non-URI content for privacy
        Log.d(TAG, "Content filtered: non-URI content with unknown safety")
        return false
    }
    
    /**
     * Checks if content contains patterns that indicate sensitive information
     * 
     * @param content The content to check
     * @return true if sensitive patterns are detected
     */
    private fun containsSensitiveContent(content: String): Boolean {
        return sensitivePatterns.any { pattern ->
            try {
                pattern.containsMatchIn(content)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking sensitive pattern", e)
                false
            }
        }
    }
    
    /**
     * Checks if non-URI content matches patterns that are generally safe to display
     * 
     * @param content The content to check
     * @return true if content matches safe patterns
     */
    private fun isSafeNonUriContent(content: String): Boolean {
        return safePatternsForNonUri.any { pattern ->
            try {
                pattern.matches(content)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking safe pattern", e)
                false
            }
        }
    }
    
    /**
     * Provides a safe display representation of content for debugging or logging purposes
     * 
     * @param content The original content
     * @param maxLength Maximum length of the safe representation
     * @return A privacy-safe representation of the content
     */
    fun getSafeDisplayText(content: String, maxLength: Int = 50): String {
        if (content.isBlank()) {
            return "[empty]"
        }
        
        // If content contains sensitive patterns, return a generic placeholder
        if (containsSensitiveContent(content)) {
            return "[sensitive content hidden]"
        }
        
        // For non-sensitive content, truncate if needed
        return if (content.length <= maxLength) {
            content
        } else {
            "${content.take(maxLength - 3)}..."
        }
    }
    
    /**
     * Analyzes content and provides a privacy assessment
     * 
     * @param content The content to analyze
     * @return PrivacyAssessment with detailed information about content safety
     */
    fun assessPrivacy(content: String): PrivacyAssessment {
        if (content.isBlank()) {
            return PrivacyAssessment(
                isSafe = false,
                reason = "Empty content",
                shouldDisplay = false
            )
        }
        
        if (content.length > 500) {
            return PrivacyAssessment(
                isSafe = false,
                reason = "Content too long",
                shouldDisplay = false
            )
        }
        
        val hasSensitiveContent = containsSensitiveContent(content)
        if (hasSensitiveContent) {
            return PrivacyAssessment(
                isSafe = false,
                reason = "Contains sensitive patterns",
                shouldDisplay = false
            )
        }
        
        val isSafePattern = isSafeNonUriContent(content)
        if (isSafePattern) {
            return PrivacyAssessment(
                isSafe = true,
                reason = "Matches safe content patterns",
                shouldDisplay = true
            )
        }
        
        return PrivacyAssessment(
            isSafe = false,
            reason = "Unknown content type - defaulting to private",
            shouldDisplay = false
        )
    }
    
    /**
     * Result of privacy assessment for clipboard content
     */
    data class PrivacyAssessment(
        val isSafe: Boolean,
        val reason: String,
        val shouldDisplay: Boolean
    )
}
