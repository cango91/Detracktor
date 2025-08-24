package com.gologlu.detracktor.data

/**
 * Analysis result for clipboard content showing what parameters would be removed
 */
data class ClipboardAnalysis(
    val originalUrl: String,
    val cleanedUrl: String,
    val isValidUrl: Boolean,
    val hasChanges: Boolean,
    val parametersToRemove: List<String>,
    val parametersToKeep: List<String>,
    val matchingRule: String?
)
