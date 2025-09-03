package com.gologlu.detracktor.runtime.android.repo

import android.content.Context
import android.content.SharedPreferences
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode

/**
 * Repository for UI-specific settings persistence using SharedPreferences.
 * Separate from application-layer settings which use JSON files.
 */
class UiSettingsRepository(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Load current UI settings, applying migrations if needed
     */
    fun loadUiSettings(): UiSettings {
        migrateIfNeeded()
        
        return UiSettings(
            urlPreviewMode = UrlPreviewMode.valueOf(
                prefs.getString(KEY_URL_PREVIEW_MODE, UrlPreviewMode.INLINE_BLUR.name) 
                    ?: UrlPreviewMode.INLINE_BLUR.name
            ),
            themeMode = ThemeMode.valueOf(
                prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) 
                    ?: ThemeMode.SYSTEM.name
            ),
            afterCleaningAction = AfterCleaningAction.valueOf(
                prefs.getString(KEY_AFTER_CLEANING_ACTION, AfterCleaningAction.ASK.name) 
                    ?: AfterCleaningAction.ASK.name
            ),
            suppressShareWarnings = prefs.getBoolean(KEY_SUPPRESS_SHARE_WARNINGS, false),
            version = prefs.getInt(KEY_VERSION, UiSettings.VERSION.toInt()).toUInt()
        )
    }
    
    /**
     * Save UI settings to SharedPreferences
     */
    fun saveUiSettings(settings: UiSettings) {
        prefs.edit()
            .putString(KEY_URL_PREVIEW_MODE, settings.urlPreviewMode.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_AFTER_CLEANING_ACTION, settings.afterCleaningAction.name)
            .putBoolean(KEY_SUPPRESS_SHARE_WARNINGS, settings.suppressShareWarnings)
            .putInt(KEY_VERSION, settings.version.toInt())
            .apply()
    }
    
    /**
     * Check if migration is needed and perform it
     */
    private fun migrateIfNeeded() {
        val currentVersion = prefs.getInt(KEY_VERSION, 0).toUInt()
        if (currentVersion < UiSettings.VERSION) {
            // Future migrations can be added here
            // For now, just update version
            prefs.edit()
                .putInt(KEY_VERSION, UiSettings.VERSION.toInt())
                .apply()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "ui_settings"
        private const val KEY_URL_PREVIEW_MODE = "url_preview_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AFTER_CLEANING_ACTION = "after_cleaning_action"
        private const val KEY_SUPPRESS_SHARE_WARNINGS = "suppress_share_warnings"
        private const val KEY_VERSION = "version"
    }
}
