package com.gologlu.detracktor.application.repo

import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.types.AppSettings

/**
 * Repository port for application settings.
 *
 * Implementations live in the runtime layer and handle persistence details
 * (e.g., files, SharedPreferences, databases, network).
 */
interface SettingsRepository {
    /**
     * Read the current user-defined settings override, if any.
     *
     * @return null when no user override exists.
     */
    suspend fun readUserSettings(): AppResult<AppSettings?>

    /**
     * Persist the user-defined settings override, replacing any previous value.
     */
    suspend fun writeUserSettings(settings: AppSettings): AppResult<Unit>

    /**
     * Remove any stored user-defined settings override.
     */
    suspend fun clearUserSettings(): AppResult<Unit>

    /**
     * Read the built-in default settings bundled with the app/runtime.
     */
    suspend fun readDefaultSettings(): AppResult<AppSettings>
}


