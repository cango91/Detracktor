package com.example.shareuntracked.data

/**
 * Represents a URL cleaning rule for specific hosts and parameters
 */
data class CleaningRule(
    val hostPattern: String,  // e.g., "*.twitter.com", "example.com"
    val params: List<String>  // e.g., ["utm_*", "fbclid", "t", "si"]
)
