package com.gologlu.detracktor.data

/**
 * Configuration for performance optimization and security settings.
 */
data class PerformanceConfig(
    val enablePatternCaching: Boolean = true,
    val maxCacheSize: Int = 1000,
    val recompileOnConfigChange: Boolean = true,
    val regexTimeoutMs: Long = 1000L, // Timeout for regex operations to prevent ReDoS
    val enableRegexValidation: Boolean = true // Enable ReDoS vulnerability detection
)
