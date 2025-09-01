package com.gologlu.detracktor.application.service

import com.gologlu.detracktor.application.error.AppError
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.error.AppValidationError
import com.gologlu.detracktor.application.error.AppValidationException
import com.gologlu.detracktor.application.error.getOrThrow
import com.gologlu.detracktor.application.repo.SettingsRepository
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.WarningSettings

/**
 * Application-level settings service.
 *
 * Responsible for orchestrating settings across sources (defaults + user overrides),
 * validating version compatibility and providing a single API to the app.
 */
interface SettingsService {
    /** Combined effective settings (defaults merged with user overrides). */
    suspend fun loadEffective(): AppResult<AppSettings>

    /** Return only the stored user override, or null if none stored. */
    suspend fun loadUser(): AppResult<AppSettings?>

    /** Persist a new user override after validation. */
    suspend fun saveUser(settings: AppSettings): AppResult<Unit>

    /** Clear any user override and revert to defaults. */
    suspend fun resetUser(): AppResult<Unit>
}

/** Default implementation that delegates persistence to a repository. */
class DefaultSettingsService(
    private val repository: SettingsRepository
) : SettingsService {

    override suspend fun loadEffective(): AppResult<AppSettings> {
        val defaultsRes = repository.readDefaultSettings()
        return when (defaultsRes) {
            is AppResult.Failure -> defaultsRes
            is AppResult.Success -> {
                val defaults = defaultsRes.value
                when (val userRes = repository.readUserSettings()) {
                    is AppResult.Failure -> userRes
                    is AppResult.Success -> AppResult.success(merge(defaults, userRes.value))
                }
            }
        }
    }

    override suspend fun loadUser(): AppResult<AppSettings?> = repository.readUserSettings()

    override suspend fun saveUser(settings: AppSettings): AppResult<Unit> {
        validate(settings).getOrThrow()
        return repository.writeUserSettings(settings)
    }

    override suspend fun resetUser(): AppResult<Unit> = repository.clearUserSettings()

    private fun validate(settings: AppSettings): AppResult<Unit> {
        // Basic semantic checks beyond data class init.
        // Example: ensure at least one site rule exists.
        if (settings.sites.isEmpty()) {
            return AppResult.failure(AppValidationError("settings.sites must not be empty", "settings.sites"))
        }
        // Version sanity is already enforced in types via init blocks.
        return AppResult.success(Unit)
    }

    private fun merge(defaults: AppSettings, user: AppSettings?): AppSettings {
        if (user == null) return defaults

        // Merge strategy: user overrides take precedence; defaults fill gaps.
        // For now, treat rules as replace-all when user override exists.
        // WarningSettings merged field-by-field.
        val mergedWarnings = mergeWarnings(defaults.warnings, user.warnings)
        return AppSettings(
            sites = if (user.sites.isNotEmpty()) user.sites else defaults.sites,
            warnings = mergedWarnings,
            version = maxOf(defaults.version, user.version)
        )
    }

    private fun mergeWarnings(defaults: WarningSettings, user: WarningSettings): WarningSettings {
        return WarningSettings(
            warnOnEmbeddedCredentials = user.warnOnEmbeddedCredentials,
            sensitiveParams = user.sensitiveParams ?: defaults.sensitiveParams,
            version = maxOf(defaults.version, user.version)
        )
    }
}

