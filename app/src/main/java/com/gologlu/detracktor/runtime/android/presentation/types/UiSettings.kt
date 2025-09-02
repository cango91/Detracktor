package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * UI-specific settings for the Android runtime layer.
 * These settings control presentation behavior and user preferences
 * that are separate from the application-layer settings.
 */
data class UiSettings(
    val urlPreviewMode: UrlPreviewMode = UrlPreviewMode.CHIPS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val afterCleaningAction: AfterCleaningAction = AfterCleaningAction.ASK,
    val version: UInt = VERSION
) {
    companion object {
        const val VERSION: UInt = 1U
    }
}

/**
 * URL preview display modes
 */
enum class UrlPreviewMode {
    /** Current chip-based display with parameters shown as chips below URL */
    CHIPS,
    /** New inline blur-based display with parameters integrated into URL text */
    INLINE_BLUR
}

/**
 * Theme mode preferences
 */
enum class ThemeMode {
    /** Light theme */
    LIGHT,
    /** Dark theme */
    DARK,
    /** Follow system theme */
    SYSTEM
}

/**
 * After-cleaning action preferences for the post-cleaning dialog
 */
enum class AfterCleaningAction {
    /** Always share cleaned URL without asking */
    ALWAYS_SHARE,
    /** Always copy cleaned URL to clipboard without asking */
    ALWAYS_COPY,
    /** Ask user what to do with cleaned URL (current behavior) */
    ASK
}
