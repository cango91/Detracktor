package com.gologlu.detracktor.data

/**
 * Configuration for performance optimization settings.
 */
data class PerformanceConfig(
    val enablePatternCaching: Boolean = true,
    val maxCacheSize: Int = 1000,
    val recompileOnConfigChange: Boolean = true
)
