package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * Enhanced status information with clipboard awareness
 */
data class AppStatus(
    val message: String,
    val clipboardState: ClipboardState,
    val canCleanUrl: Boolean
)
