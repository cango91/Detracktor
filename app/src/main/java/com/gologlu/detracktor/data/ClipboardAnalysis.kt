package com.gologlu.detracktor.data

import com.gologlu.detracktor.utils.UrlPrivacyAnalyzer

/**
 * Analysis result for clipboard content showing what parameters would be removed
 */
data class ClipboardAnalysis(
    val originalUrl: String,
    val cleanedUrl: String,
    val isValidUrl: Boolean,
    val hasChanges: Boolean,
    val parametersToRemove: List<String>,
    val parametersToKeep: List<String>,
    val matchingRules: List<String>,
    val shouldDisplayContent: Boolean = true, // Privacy control for content display
    val privacyAnalysis: UrlPrivacyAnalyzer.UrlPrivacyAnalysis? = null // Enhanced privacy analysis for URLs
)
