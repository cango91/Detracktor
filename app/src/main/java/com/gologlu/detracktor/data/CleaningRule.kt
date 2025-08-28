package com.gologlu.detracktor.data

/**
 * Simplified cleaning rule with regex pattern and numeric priority.
 * Replaces the over-engineered PatternType + RulePriority system.
 */
data class CleaningRule(
    val id: String,
    val hostPattern: String,  // Always treated as regex
    val parameterPatterns: List<String>,  // Parameter names/patterns to remove
    val priority: Int,  // Lower number = higher priority
    val enabled: Boolean = true,
    val description: String? = null
)
