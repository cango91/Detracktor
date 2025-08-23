package com.gologlu.detracktor.data

/**
 * Compiled rule for runtime performance optimization.
 * Contains pre-compiled regex patterns and normalized host patterns for fast matching.
 */
data class CompiledRule(
    val originalRule: CleaningRule,
    val compiledHostPattern: Regex?,
    val compiledParamPatterns: List<Regex>,
    val normalizedHostPattern: String,
    val specificity: Int
)
