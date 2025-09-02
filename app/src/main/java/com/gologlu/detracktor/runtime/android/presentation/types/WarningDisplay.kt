package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * Data structure for enhanced warning display in the UI.
 * Supports collapsible warning panels with detailed information.
 * Rule matches are now separated from warnings - only credentials and sensitive params are warnings.
 */
data class WarningDisplayData(
    val hasCredentials: Boolean,
    val sensitiveParams: List<String>,
    val isExpanded: Boolean = false
) {
    val hasWarnings: Boolean
        get() = hasCredentials || sensitiveParams.isNotEmpty()
    
    val warningCount: Int
        get() = (if (hasCredentials) 1 else 0) + 
                (if (sensitiveParams.isNotEmpty()) 1 else 0)
}

/**
 * Data structure for rule match display - separate from warnings
 */
data class RuleMatchDisplayData(
    val matchedRules: List<RuleMatchSummary>,
    val isExpanded: Boolean = false
) {
    val hasMatches: Boolean
        get() = matchedRules.isNotEmpty()
}

/**
 * Summary of a rule match for display purposes
 */
data class RuleMatchSummary(
    val description: String,
    val matchedParams: List<String>, // Only actually matched parameters
    val domain: String
)
