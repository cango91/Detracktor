package com.gologlu.detracktor.data

import com.gologlu.detracktor.utils.PrivacySettings

/**
 * App configuration with versioning, advanced features, and privacy/security settings.
 * Simplified version without backwards compatibility complexity.
 */
data class AppConfig(
    val version: Int = 2,
    val removeAllParams: Boolean = false,
    val rules: List<CleaningRule> = emptyList(),
    val hostNormalization: HostNormalizationConfig = HostNormalizationConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    
    // Privacy and security settings
    val privacy: PrivacySettings = PrivacySettings(),
    val security: SecurityConfig = SecurityConfig()
)

/**
 * Security configuration for regex validation and safe operations
 */
data class SecurityConfig(
    val enableRegexValidation: Boolean = true,
    val regexTimeoutMs: Long = 1000L,
    val maxRegexLength: Int = 1000,
    val rejectHighRiskPatterns: Boolean = true,
    val enablePerformanceTesting: Boolean = true
)
