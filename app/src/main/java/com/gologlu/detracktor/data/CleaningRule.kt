package com.gologlu.detracktor.data

/**
 * Cleaning rule with priority and compiled patterns for hierarchical matching.
 * Simplified version without backwards compatibility complexity.
 */
data class CleaningRule(
    val hostPattern: String,
    val params: List<String>,
    val priority: RulePriority,
    val patternType: PatternType = PatternType.WILDCARD,
    val enabled: Boolean = true,
    val description: String? = null
)
