package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * UI-specific settings for the Android runtime layer.
 * These settings control presentation behavior and user preferences
 * that are separate from the application-layer settings.
 */
data class UiSettings(
    val urlPreviewMode: UrlPreviewMode = UrlPreviewMode.INLINE_BLUR,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val afterCleaningAction: AfterCleaningAction = AfterCleaningAction.ASK,
    val suppressShareWarnings: Boolean = false,
    val version: UInt = VERSION
) {
    companion object {
        const val VERSION: UInt = 2U
    }
}

/**
 * URL preview display modes
 */
enum class UrlPreviewMode {
    /** Unified inline blur-based display with parameters integrated into URL text and masking support */
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

/**
 * Dialog types to distinguish between manual cleaning and share-intent dialogs
 */
enum class DialogType {
    /** Dialog shown after manual URL cleaning */
    MANUAL_CLEANING,
    /** Dialog shown for share-intent warnings */
    SHARE_INTENT_WARNING
}

/**
 * Dialog state data class for managing dialog visibility and content
 */
data class DialogState(
    val isVisible: Boolean = false,
    val type: DialogType = DialogType.MANUAL_CLEANING,
    val cleanedUrl: String? = null,
    val warningData: WarningDisplayData? = null
)
