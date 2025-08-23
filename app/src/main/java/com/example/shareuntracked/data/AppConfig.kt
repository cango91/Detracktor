package com.example.shareuntracked.data

/**
 * Application configuration for URL cleaning behavior
 */
data class AppConfig(
    val removeAllParams: Boolean = true,
    val rules: List<CleaningRule> = emptyList()
)
