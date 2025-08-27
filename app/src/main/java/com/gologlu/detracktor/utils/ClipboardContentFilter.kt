package com.gologlu.detracktor.utils

import android.net.Uri

/**
 * Filters and categorizes clipboard content for privacy protection.
 * Works in conjunction with UrlPrivacyAnalyzer to provide comprehensive
 * clipboard content analysis and filtering.
 */
class ClipboardContentFilter {
    
    private val privacyAnalyzer = UrlPrivacyAnalyzer()
    
    companion object {
        // Maximum length for content preview
        private const val MAX_PREVIEW_LENGTH = 200
        
        // Patterns that indicate potentially sensitive non-URI content
        private val SENSITIVE_PATTERNS = listOf(
            // Credit card numbers
            Regex("\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"),
            // Social security numbers
            Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            // Phone numbers
            Regex("\\b(?:\\+?1[\\s-]?)?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{4}\\b"),
            // Email addresses (when not part of URI)
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            // API keys and tokens (long alphanumeric strings)
            Regex("\\b[A-Za-z0-9]{32,}\\b"),
            // Passwords (common patterns)
            Regex("(?i)password\\s*[:=]\\s*\\S+"),
            // Private keys
            Regex("-----BEGIN [A-Z ]+PRIVATE KEY-----")
        )
        
        // File extensions that might contain sensitive data
        private val SENSITIVE_FILE_EXTENSIONS = setOf(
            "key", "pem", "p12", "pfx", "jks", "keystore",
            "wallet", "dat", "conf", "config", "env"
        )
    }
    
    /**
     * Analyzes and filters clipboard content for privacy and security
     */
    fun analyzeAndFilter(content: String, settings: PrivacySettings): FilteredContent {
        val trimmedContent = content.trim()
        
        if (trimmedContent.isEmpty()) {
            return FilteredContent(
                originalContent = content,
                filteredContent = "",
                contentType = ClipboardContentType.NON_URI,
                privacyLevel = UrlPrivacyLevel.VISIBLE,
                shouldDisplay = false,
                shouldBlur = false,
                riskFactors = emptyList()
            )
        }
        
        // Categorize the content type
        val contentType = categorizeContent(trimmedContent)
        val riskFactors = identifyRiskFactors(trimmedContent, contentType)
        
        // Determine privacy level with special handling for EMBEDDED_CREDENTIALS
        val privacyLevel = when (contentType) {
            ClipboardContentType.NON_URI -> {
                if (riskFactors.isNotEmpty()) UrlPrivacyLevel.HIDDEN
                else if (settings.hideNonUriContent) UrlPrivacyLevel.HIDDEN
                else UrlPrivacyLevel.VISIBLE
            }
            ClipboardContentType.SENSITIVE_URI -> {
                // Special case: if EMBEDDED_CREDENTIALS is the only risk factor, don't blur
                if (riskFactors.size == 1 && riskFactors.first().type == RiskFactorType.EMBEDDED_CREDENTIALS) {
                    UrlPrivacyLevel.VISIBLE
                } else {
                    UrlPrivacyLevel.BLURRED
                }
            }
            ClipboardContentType.URI -> {
                val basePrivacyLevel = privacyAnalyzer.analyzeUrlPrivacy(trimmedContent)
                // Apply same exception logic for URIs with embedded credentials
                if (basePrivacyLevel == UrlPrivacyLevel.BLURRED && 
                    riskFactors.size == 1 && 
                    riskFactors.first().type == RiskFactorType.EMBEDDED_CREDENTIALS) {
                    UrlPrivacyLevel.VISIBLE
                } else {
                    basePrivacyLevel
                }
            }
        }
        
        // Determine display and blur settings with EMBEDDED_CREDENTIALS exception
        val shouldDisplay = privacyAnalyzer.shouldDisplayContent(trimmedContent, contentType, settings)
        val shouldBlur = shouldDisplay && 
            privacyAnalyzer.shouldBlurContent(trimmedContent, contentType, settings) &&
            // Don't blur if EMBEDDED_CREDENTIALS is the only risk factor
            !(riskFactors.size == 1 && riskFactors.first().type == RiskFactorType.EMBEDDED_CREDENTIALS)
        
        // Create filtered content
        val filteredContent = createFilteredContent(trimmedContent, contentType, privacyLevel, settings)
        
        return FilteredContent(
            originalContent = content,
            filteredContent = filteredContent,
            contentType = contentType,
            privacyLevel = privacyLevel,
            shouldDisplay = shouldDisplay,
            shouldBlur = shouldBlur,
            riskFactors = riskFactors
        )
    }
    
    /**
     * Categorizes clipboard content type
     */
    fun categorizeContent(content: String): ClipboardContentType {
        return privacyAnalyzer.categorizeContent(content)
    }
    
    /**
     * Determines if content should be displayed based on type and settings
     */
    fun shouldDisplayContent(
        content: String, 
        contentType: ClipboardContentType, 
        settings: PrivacySettings
    ): Boolean {
        return privacyAnalyzer.shouldDisplayContent(content, contentType, settings)
    }
    
    /**
     * Creates a privacy-safe preview of content
     */
    fun createSafePreview(content: String, maxLength: Int = MAX_PREVIEW_LENGTH): String {
        return privacyAnalyzer.createPrivacySafePreview(content, maxLength)
    }
    
    /**
     * Identifies risk factors in the content
     */
    private fun identifyRiskFactors(content: String, contentType: ClipboardContentType): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()
        
        when (contentType) {
            ClipboardContentType.NON_URI -> {
                // Check for sensitive patterns in non-URI content
                SENSITIVE_PATTERNS.forEach { pattern ->
                    if (pattern.containsMatchIn(content)) {
                        riskFactors.add(
                            RiskFactor(
                                type = RiskFactorType.SENSITIVE_DATA,
                                description = "Contains potentially sensitive information",
                                severity = RiskSeverity.HIGH
                            )
                        )
                    }
                }
                
                // Check for file paths with sensitive extensions
                SENSITIVE_FILE_EXTENSIONS.forEach { ext ->
                    if (content.contains(".$ext", ignoreCase = true)) {
                        riskFactors.add(
                            RiskFactor(
                                type = RiskFactorType.SENSITIVE_FILE,
                                description = "Contains reference to potentially sensitive file",
                                severity = RiskSeverity.MEDIUM
                            )
                        )
                    }
                }
                
                // Check for very long strings that might be tokens
                if (content.length > 100 && content.matches(Regex("[A-Za-z0-9+/=_-]+"))) {
                    riskFactors.add(
                        RiskFactor(
                            type = RiskFactorType.POTENTIAL_TOKEN,
                            description = "Long alphanumeric string that might be a token or key",
                            severity = RiskSeverity.MEDIUM
                        )
                    )
                }
            }
            
            ClipboardContentType.SENSITIVE_URI, ClipboardContentType.URI -> {
                // Check for credentials in URI
                if (privacyAnalyzer.hasSensitiveCredentials(content)) {
                    riskFactors.add(
                        RiskFactor(
                            type = RiskFactorType.EMBEDDED_CREDENTIALS,
                            description = "EMBEDDED_CREDENTIALS - ALWAYS HIDDEN",
                            severity = RiskSeverity.HIGH
                        )
                    )
                }
                
                // Check for sensitive query parameters
                if (privacyAnalyzer.hasSecretQueryParams(content)) {
                    riskFactors.add(
                        RiskFactor(
                            type = RiskFactorType.SENSITIVE_PARAMS,
                            description = "URI contains potentially sensitive query parameters",
                            severity = RiskSeverity.MEDIUM
                        )
                    )
                }
            }
        }
        
        return riskFactors
    }
    
    /**
     * Creates filtered content based on privacy level and settings
     */
    private fun createFilteredContent(
        content: String,
        contentType: ClipboardContentType,
        privacyLevel: UrlPrivacyLevel,
        settings: PrivacySettings
    ): String {
        return when (privacyLevel) {
            UrlPrivacyLevel.HIDDEN -> "[Content hidden for privacy]"
            UrlPrivacyLevel.BLURRED -> {
                when (contentType) {
                    ClipboardContentType.NON_URI -> "[Non-URI content blurred]"
                    else -> privacyAnalyzer.createPrivacySafePreview(content)
                }
            }
            UrlPrivacyLevel.VISIBLE -> {
                if (content.length > MAX_PREVIEW_LENGTH) {
                    content.substring(0, MAX_PREVIEW_LENGTH) + "..."
                } else {
                    content
                }
            }
        }
    }
}

/**
 * Result of content filtering analysis
 */
data class FilteredContent(
    val originalContent: String,
    val filteredContent: String,
    val contentType: ClipboardContentType,
    val privacyLevel: UrlPrivacyLevel,
    val shouldDisplay: Boolean,
    val shouldBlur: Boolean,
    val riskFactors: List<RiskFactor>
)

/**
 * Risk factor identified in content
 */
data class RiskFactor(
    val type: RiskFactorType,
    val description: String,
    val severity: RiskSeverity
)

/**
 * Types of risk factors
 */
enum class RiskFactorType {
    SENSITIVE_DATA,         // Contains sensitive personal/financial data
    EMBEDDED_CREDENTIALS,   // Contains embedded username/password
    SENSITIVE_PARAMS,       // Contains sensitive query parameters
    POTENTIAL_TOKEN,        // Might be an API token or key
    SENSITIVE_FILE          // References sensitive file types
}

/**
 * Severity levels for risk factors
 */
enum class RiskSeverity {
    LOW,        // Minor privacy concern
    MEDIUM,     // Moderate privacy risk
    HIGH,       // High privacy risk
    CRITICAL    // Critical privacy/security risk
}
