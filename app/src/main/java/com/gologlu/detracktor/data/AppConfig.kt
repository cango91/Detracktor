package com.gologlu.detracktor.data

/**
 * App configuration with versioning and advanced features.
 * Simplified version without backwards compatibility complexity.
 */
data class AppConfig(
    val version: Int = 2,
    val removeAllParams: Boolean = false,
    val rules: List<CleaningRule> = emptyList(),
    val hostNormalization: HostNormalizationConfig = HostNormalizationConfig(),
    val performance: PerformanceConfig = PerformanceConfig()
)
