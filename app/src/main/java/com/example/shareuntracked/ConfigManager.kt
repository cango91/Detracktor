package com.example.shareuntracked

import android.content.Context
import com.example.shareuntracked.data.AppConfig
import com.example.shareuntracked.data.CleaningRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

/**
 * Manages application configuration using JSON file storage
 */
class ConfigManager(private val context: Context) {
    
    private val gson = Gson()
    private val configFile = File(context.filesDir, "config.json")
    
    /**
     * Load configuration from JSON file, or return default if file doesn't exist
     */
    fun loadConfig(): AppConfig {
        return try {
            if (configFile.exists()) {
                val json = configFile.readText()
                gson.fromJson(json, AppConfig::class.java)
            } else {
                getDefaultConfig()
            }
        } catch (e: Exception) {
            // If there's any error reading the config, return default
            getDefaultConfig()
        }
    }
    
    /**
     * Save configuration to JSON file
     */
    fun saveConfig(config: AppConfig) {
        try {
            val json = gson.toJson(config)
            configFile.writeText(json)
        } catch (e: IOException) {
            // Handle save error - could log or show toast
        }
    }
    
    /**
     * Get default configuration from assets
     */
    fun getDefaultConfig(): AppConfig {
        return try {
            val json = context.assets.open("default_rules.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, AppConfig::class.java)
        } catch (e: Exception) {
            // Fallback to hardcoded default if assets file is missing
            AppConfig(
                removeAllParams = true,
                rules = emptyList()
            )
        }
    }
    
    /**
     * Get default cleaning rules from assets
     */
    fun getDefaultRules(): List<CleaningRule> {
        return getDefaultConfig().rules
    }
    
    /**
     * Reset configuration to default
     */
    fun resetToDefault() {
        val defaultConfig = getDefaultConfig()
        saveConfig(defaultConfig)
    }
}
