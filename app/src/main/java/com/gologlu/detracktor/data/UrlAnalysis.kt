package com.gologlu.detracktor.data

/**
 * Simplified analysis result replacing the complex ClipboardAnalysis system.
 * Contains all information needed for URL display and cleaning.
 */
data class UrlAnalysis(
    val originalUrl: String,
    val segments: List<AnnotatedUrlSegment>,
    val hasEmbeddedCredentials: Boolean,
    val matchingRules: List<String>,
    val cleanedUrl: String
)
