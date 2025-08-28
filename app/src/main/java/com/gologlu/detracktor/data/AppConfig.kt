package com.gologlu.detracktor.data

/**
 * Simplified configuration structure.
 * Removes complex privacy settings and focuses on core rule management.
 */
data class AppConfig(
    val version: Int,
    val rules: List<CleaningRule>,
    val removeAllParams: Boolean = false
)
