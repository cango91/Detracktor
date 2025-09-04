package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * Data class for instructional content with expansion state
 */
data class InstructionalContent(
    val title: String,
    val steps: List<String>,
    val isExpanded: Boolean = false
)
