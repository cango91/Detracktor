package com.gologlu.detracktor

import android.content.Context
import com.gologlu.detracktor.data.AppConfig
import com.gologlu.detracktor.data.CleaningRule
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.util.regex.PatternSyntaxException

/**
 * Simplified configuration manager for the new rule system.
 * Handles loading, saving, and validation of cleaning rules.
 */
class ConfigManager(private val context: Context) {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private val configFile = File(context.filesDir, "config.json")
    private val defaultConfigFile = "default_rules.json"
    
    companion object {
        private const val CURRENT_CONFIG_VERSION = 1
    }
    
    /**
     * Load configuration from file or create default if none exists.
     */
    fun loadConfig(): AppConfig {
        return try {
            if (configFile.exists()) {
                val configJson = configFile.readText()
                gson.fromJson(configJson, AppConfig::class.java)
            } else {
                loadDefaultConfig()
            }
        } catch (e: Exception) {
            // Fallback to default config if loading fails
            loadDefaultConfig()
        }
    }
    
    /**
     * Save configuration to file.
     */
    fun saveConfig(config: AppConfig): Boolean {
        return try {
            val configJson = gson.toJson(config)
            configFile.writeText(configJson)
            true
        } catch (e: IOException) {
            false
        }
    }
    
    /**
     * Load default configuration from assets.
     */
    private fun loadDefaultConfig(): AppConfig {
        return try {
            val defaultConfigJson = context.assets.open(defaultConfigFile).bufferedReader().use { it.readText() }
            gson.fromJson(defaultConfigJson, AppConfig::class.java)
        } catch (e: Exception) {
            // Ultimate fallback: minimal working config
            AppConfig(
                version = CURRENT_CONFIG_VERSION,
                rules = createFallbackRules(),
                removeAllParams = false
            )
        }
    }
    
    /**
     * Validate a cleaning rule.
     * Returns null if valid, error message if invalid.
     */
    fun validateRule(rule: CleaningRule): String? {
        // Validate rule ID
        if (rule.id.isBlank()) {
            return "Rule ID cannot be empty"
        }
        
        // Validate host pattern
        if (rule.hostPattern.isBlank()) {
            return "Host pattern cannot be empty"
        }
        
        try {
            java.util.regex.Pattern.compile(rule.hostPattern)
        } catch (e: PatternSyntaxException) {
            return "Invalid host pattern regex: ${e.message}"
        }
        
        // Validate parameter patterns
        rule.parameterPatterns.forEachIndexed { index, pattern ->
            if (pattern.isBlank()) {
                return "Parameter pattern ${index + 1} cannot be empty"
            }
            try {
                java.util.regex.Pattern.compile(pattern)
            } catch (e: PatternSyntaxException) {
                return "Invalid parameter pattern ${index + 1} regex: ${e.message}"
            }
        }
        
        // Validate priority
        if (rule.priority < 0) {
            return "Priority must be non-negative"
        }
        
        return null // Valid
    }
    
    /**
     * Add or update a rule in the configuration.
     */
    fun addOrUpdateRule(rule: CleaningRule): Boolean {
        val validationError = validateRule(rule)
        if (validationError != null) {
            return false
        }
        
        val config = loadConfig()
        val updatedRules = config.rules.toMutableList()
        
        // Remove existing rule with same ID
        updatedRules.removeAll { it.id == rule.id }
        
        // Add new rule
        updatedRules.add(rule)
        
        // Sort by priority
        updatedRules.sortBy { it.priority }
        
        val updatedConfig = config.copy(rules = updatedRules)
        return saveConfig(updatedConfig)
    }
    
    /**
     * Remove a rule from the configuration.
     */
    fun removeRule(ruleId: String): Boolean {
        val config = loadConfig()
        val updatedRules = config.rules.filter { it.id != ruleId }
        val updatedConfig = config.copy(rules = updatedRules)
        return saveConfig(updatedConfig)
    }
    
    /**
     * Toggle rule enabled state.
     */
    fun toggleRule(ruleId: String): Boolean {
        val config = loadConfig()
        val updatedRules = config.rules.map { rule ->
            if (rule.id == ruleId) {
                rule.copy(enabled = !rule.enabled)
            } else {
                rule
            }
        }
        val updatedConfig = config.copy(rules = updatedRules)
        return saveConfig(updatedConfig)
    }
    
    /**
     * Reset configuration to defaults.
     */
    fun resetToDefaults(): Boolean {
        return try {
            if (configFile.exists()) {
                configFile.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create minimal fallback rules if all else fails.
     */
    private fun createFallbackRules(): List<CleaningRule> {
        return listOf(
            CleaningRule(
                id = "google-tracking",
                hostPattern = ".*\\.google\\..*",
                parameterPatterns = listOf("utm_.*", "gclid", "fbclid"),
                priority = 1,
                enabled = true,
                description = "Remove Google tracking parameters"
            ),
            CleaningRule(
                id = "facebook-tracking",
                hostPattern = ".*\\.facebook\\..*",
                parameterPatterns = listOf("fbclid", "utm_.*"),
                priority = 2,
                enabled = true,
                description = "Remove Facebook tracking parameters"
            )
        )
    }
}
