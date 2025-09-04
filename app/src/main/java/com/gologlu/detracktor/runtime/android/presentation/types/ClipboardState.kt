package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * Represents the different states of clipboard content for UX feedback
 */
enum class ClipboardState {
    EMPTY,           // No content in clipboard
    NON_TEXT,        // Clipboard has non-text content
    TEXT_NOT_URL,    // Clipboard has text but not a valid URL
    VALID_URL        // Clipboard contains a valid URL
}
