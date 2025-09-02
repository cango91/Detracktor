package com.gologlu.detracktor.runtime.android.service

import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode
import com.gologlu.detracktor.runtime.android.repo.UiSettingsRepository

/**
 * Service for managing UI-specific settings.
 * Provides business logic layer over the UI settings repository.
 */
class UiSettingsService(private val repository: UiSettingsRepository) {
    
    /**
     * Get current UI settings
     */
    fun getCurrentSettings(): UiSettings {
        return repository.loadUiSettings()
    }
    
    /**
     * Update all UI settings at once
     */
    fun updateSettings(settings: UiSettings) {
        repository.saveUiSettings(settings)
    }
    
    /**
     * Update only the theme mode
     */
    fun updateTheme(theme: ThemeMode) {
        val current = getCurrentSettings()
        updateSettings(current.copy(themeMode = theme))
    }
    
    /**
     * Update only the URL preview mode
     */
    fun updatePreviewMode(mode: UrlPreviewMode) {
        val current = getCurrentSettings()
        updateSettings(current.copy(urlPreviewMode = mode))
    }
    
    /**
     * Update only the after-cleaning action
     */
    fun updateAfterCleaningAction(action: AfterCleaningAction) {
        val current = getCurrentSettings()
        updateSettings(current.copy(afterCleaningAction = action))
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        updateSettings(UiSettings())
    }
}
