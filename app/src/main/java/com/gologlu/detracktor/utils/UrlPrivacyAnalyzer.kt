package com.gologlu.detracktor.utils

import android.net.Uri
import java.util.regex.Pattern

/**
 * Privacy levels for URL display
 */
enum class UrlPrivacyLevel {
    HIDDEN,     // Content should not be displayed at all
    BLURRED,    // Content should be displayed with blur effect
    VISIBLE     // Content can be displayed normally
}

/**
 * Types of clipboard content for privacy categorization
 */
enum class ClipboardContentType {
    URI,            // Valid URI that can be processed
    SENSITIVE_URI,  // URI with sensitive information
    NON_URI         // Non-URI content that should be hidden
}

/**
 * User privacy settings for content display
 */
data class PrivacySettings(
    val hideNonUriContent: Boolean = true,
    val blurSensitiveParams: Boolean = true,
    val blurCredentials: Boolean = true,
    val showBlurToggle: Boolean = true
)

/**
 * Analyzes URLs and clipboard content for privacy concerns and determines
 * appropriate display levels to protect sensitive information.
 */
class UrlPrivacyAnalyzer {
    
    companion object {
        // Common sensitive query parameter patterns
        private val SENSITIVE_PARAM_PATTERNS = listOf(
            Pattern.compile("(?i)(password|pwd|pass|secret|key|token|auth|session|sid|api_key|access_token|refresh_token)"),
            Pattern.compile("(?i)(oauth|jwt|bearer|credential|login|signin|signup)"),
            Pattern.compile("(?i)(private|confidential|secure|hidden|internal)")
        )
        
        // Pattern for detecting user:password@hostname format
        private val CREDENTIALS_PATTERN = Pattern.compile("://[^@/]+:[^@/]+@")
        
        // Pattern for detecting if content looks like a URI
        private val URI_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://")
        
        // Common non-sensitive parameters that can be shown
        private val NON_SENSITIVE_PARAMS = setOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
            "ref", "source", "from", "lang", "locale", "theme", "view", "page", "tab",
            "sort", "order", "limit", "offset", "q", "query", "search"
        )
    }
    
    /**
     * Analyzes a URL to determine its privacy level
     */
    fun analyzeUrlPrivacy(url: String): UrlPrivacyLevel {
        return when {
            hasSensitiveCredentials(url) -> UrlPrivacyLevel.BLURRED
            hasSecretQueryParams(url) -> UrlPrivacyLevel.BLURRED
            else -> UrlPrivacyLevel.VISIBLE
        }
    }
    
    /**
     * Detects if URL contains user:password credentials
     */
    fun hasSensitiveCredentials(url: String): Boolean {
        return CREDENTIALS_PATTERN.matcher(url).find()
    }
    
    /**
     * Checks if URI has potentially secret query parameters
     */
    fun hasSecretQueryParams(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            hasSecretQueryParams(uri)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if URI has potentially secret query parameters
     */
    fun hasSecretQueryParams(uri: Uri): Boolean {
        val queryParameterNames = uri.queryParameterNames ?: return false
        
        return queryParameterNames.any { paramName ->
            // Check if parameter name matches sensitive patterns
            val isSensitive = SENSITIVE_PARAM_PATTERNS.any { pattern ->
                pattern.matcher(paramName).find()
            }
            
            // Also check parameter values for suspicious patterns
            val hasLongValue = try {
                val value = uri.getQueryParameter(paramName)
                value != null && value.length > 8 && value.matches(Regex("[A-Za-z0-9+/=_-]+"))
            } catch (e: Exception) {
                false
            }
            
            // Check for common sensitive parameter names (case insensitive)
            val commonSensitiveParams = setOf(
                "token", "api_key", "apikey", "access_token", "auth_token", 
                "password", "pwd", "pass", "secret", "key", "session", "sid"
            )
            val isCommonSensitive = commonSensitiveParams.contains(paramName.lowercase())
            
            isSensitive || isCommonSensitive || (hasLongValue && !NON_SENSITIVE_PARAMS.contains(paramName.lowercase()))
        }
    }
    
    /**
     * Categorizes clipboard content type for privacy handling
     */
    fun categorizeContent(content: String): ClipboardContentType {
        val trimmedContent = content.trim()
        
        return when {
            !isLikelyUri(trimmedContent) -> ClipboardContentType.NON_URI
            hasSensitiveCredentials(trimmedContent) || hasSecretQueryParams(trimmedContent) -> 
                ClipboardContentType.SENSITIVE_URI
            else -> ClipboardContentType.URI
        }
    }
    
    /**
     * Determines if content should be displayed based on type and settings
     */
    fun shouldDisplayContent(
        content: String, 
        contentType: ClipboardContentType, 
        settings: PrivacySettings
    ): Boolean {
        return when (contentType) {
            ClipboardContentType.NON_URI -> !settings.hideNonUriContent
            ClipboardContentType.SENSITIVE_URI -> true // Display but with blur
            ClipboardContentType.URI -> true
        }
    }
    
    /**
     * Determines if content should be blurred based on analysis and settings
     */
    fun shouldBlurContent(
        content: String,
        contentType: ClipboardContentType,
        settings: PrivacySettings
    ): Boolean {
        return when (contentType) {
            ClipboardContentType.SENSITIVE_URI -> 
                settings.blurSensitiveParams || (settings.blurCredentials && hasSensitiveCredentials(content))
            ClipboardContentType.URI -> 
                settings.blurCredentials && hasSensitiveCredentials(content)
            ClipboardContentType.NON_URI -> false
        }
    }
    
    /**
     * Creates a privacy-safe preview of the content
     */
    fun createPrivacySafePreview(content: String, maxLength: Int = 100): String {
        val trimmedContent = content.trim()
        
        return when (categorizeContent(trimmedContent)) {
            ClipboardContentType.NON_URI -> "[Non-URI content hidden for privacy]"
            ClipboardContentType.SENSITIVE_URI -> {
                val preview = if (trimmedContent.length > maxLength) {
                    trimmedContent.substring(0, maxLength) + "..."
                } else {
                    trimmedContent
                }
                sanitizeUrlForPreview(preview)
            }
            ClipboardContentType.URI -> {
                if (trimmedContent.length > maxLength) {
                    trimmedContent.substring(0, maxLength) + "..."
                } else {
                    trimmedContent
                }
            }
        }
    }
    
    /**
     * Sanitizes URL for preview by masking sensitive parts
     */
    private fun sanitizeUrlForPreview(url: String): String {
        var sanitized = url
        
        // Mask credentials
        sanitized = CREDENTIALS_PATTERN.matcher(sanitized).replaceAll("://[credentials]@")
        
        // Mask sensitive query parameters
        try {
            val uri = Uri.parse(sanitized)
            if (uri.query != null) {
                val queryParams = uri.queryParameterNames
                var maskedQuery = uri.query!!
                
                queryParams?.forEach { paramName ->
                    val isSensitive = SENSITIVE_PARAM_PATTERNS.any { pattern ->
                        pattern.matcher(paramName).find()
                    }
                    
                    if (isSensitive) {
                        val paramValue = uri.getQueryParameter(paramName)
                        if (paramValue != null) {
                            maskedQuery = maskedQuery.replace("$paramName=$paramValue", "$paramName=[hidden]")
                        }
                    }
                }
                
                sanitized = sanitized.replace(uri.query!!, maskedQuery)
            }
        } catch (e: Exception) {
            // If parsing fails, return original with basic masking
        }
        
        return sanitized
    }
    
    /**
     * Checks if content looks like a URI
     */
    private fun isLikelyUri(content: String): Boolean {
        return URI_PATTERN.matcher(content).find()
    }
}
