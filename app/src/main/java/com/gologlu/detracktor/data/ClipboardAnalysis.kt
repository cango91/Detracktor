package com.gologlu.detracktor.data

import com.gologlu.detracktor.utils.ClipboardContentType
import com.gologlu.detracktor.utils.UrlPrivacyLevel
import com.gologlu.detracktor.utils.RiskFactor

/**
 * Analysis result for clipboard content showing what parameters would be removed
 * and privacy/security information
 */
data class ClipboardAnalysis(
    val originalUrl: String,
    val cleanedUrl: String,
    val isValidUrl: Boolean,
    val hasChanges: Boolean,
    val parametersToRemove: List<String>,
    val parametersToKeep: List<String>,
    val matchingRules: List<String>,
    
    // Privacy and security fields
    val contentType: ClipboardContentType = ClipboardContentType.URI,
    val privacyLevel: UrlPrivacyLevel = UrlPrivacyLevel.VISIBLE,
    val hasSensitiveData: Boolean = false,
    val shouldDisplay: Boolean = true,
    val shouldBlur: Boolean = false,
    val riskFactors: List<RiskFactor> = emptyList(),
    val safePreview: String = originalUrl
)
