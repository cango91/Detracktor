package com.gologlu.detracktor

import android.content.Context
import android.util.Log
import com.gologlu.detracktor.data.*
import com.gologlu.detracktor.utils.HostNormalizer
import com.gologlu.detracktor.utils.RuleCompiler
import com.google.gson.Gson
import java.io.File
import java.io.IOException

/**
 * Configuration manager with rule compilation and caching.
 * Simplified version without backwards compatibility complexity.
 */
class ConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ConfigManager"
        private const val CONFIG_FILE = "config.json"
        private const val BACKUP_DIR = "config_backups"
    }
    
    private val gson = Gson()
    private val configFile = File(context.filesDir, CONFIG_FILE)
    private val backupDir = File(context.filesDir, BACKUP_DIR)
    
    // Lazy initialization of utility classes
    val hostNormalizer by lazy { 
        HostNormalizer(loadConfig().hostNormalization) 
    }
    val ruleCompiler by lazy { 
        RuleCompiler(loadConfig().performance) 
    }
    
    // Cached compiled rules
    private var cachedCompiledRules: List<CompiledRule>? = null
    private var lastConfigHash: Int = 0
    
    init {
        // Ensure backup directory exists
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }
    
    /**
     * Load configuration.
     */
    fun loadConfig(): AppConfig {
        return try {
            if (configFile.exists()) {
                val json = configFile.readText()
                gson.fromJson(json, AppConfig::class.java)
            } else {
                Log.i(TAG, "No config found, creating default config")
                getDefaultConfig()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading config, returning default", e)
            getDefaultConfig()
        }
    }
    
    /**
     * Save configuration to file.
     */
    fun saveConfig(config: AppConfig) {
        try {
            val json = gson.toJson(config)
            configFile.writeText(json)
            
            // Invalidate cached compiled rules if config changed
            val newHash = config.hashCode()
            if (newHash != lastConfigHash) {
                invalidateCompiledRulesCache()
                lastConfigHash = newHash
            }
            
            Log.d(TAG, "Config saved successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save config", e)
        }
    }
    
    /**
     * Get compiled rules with caching support.
     */
    fun getCompiledRules(): List<CompiledRule> {
        val config = loadConfig()
        val configHash = config.hashCode()
        
        // Return cached rules if config hasn't changed
        if (cachedCompiledRules != null && configHash == lastConfigHash) {
            return cachedCompiledRules!!
        }
        
        // Compile rules and cache them
        val compiledRules = ruleCompiler.compileRules(config.rules)
        cachedCompiledRules = compiledRules
        lastConfigHash = configHash
        
        Log.d(TAG, "Compiled ${compiledRules.size} rules")
        return compiledRules
    }
    
    /**
     * Recompile rules and update cache.
     */
    fun recompileRules() {
        ruleCompiler.invalidateCache()
        invalidateCompiledRulesCache()
        getCompiledRules() // Trigger recompilation
    }
    
    /**
     * Get default configuration from assets.
     */
    fun getDefaultConfig(): AppConfig {
        return try {
            val json = context.assets.open("default_rules.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, AppConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Default rules not found, using hardcoded default", e)
            createHardcodedDefault()
        }
    }

    /**
     * Reset configuration to default format.
     */
    fun resetToDefault() {
        val defaultConfig = getDefaultConfig()
        saveConfig(defaultConfig)
        Log.i(TAG, "Config reset to default format")
    }
    
    /**
     * Create backup of configuration.
     */
    fun createBackup(configJson: String, filename: String) {
        try {
            val backupFile = File(backupDir, filename)
            backupFile.writeText(configJson)
            Log.d(TAG, "Config backup created: $filename")
        } catch (e: IOException) {
            Log.w(TAG, "Failed to create config backup", e)
        }
    }
    
    /**
     * Get configuration statistics.
     */
    fun getConfigStats(): Map<String, Any> {
        val config = loadConfig()
        val compiledRules = getCompiledRules()
        
        return mapOf(
            "version" to config.version,
            "totalRules" to config.rules.size,
            "enabledRules" to config.rules.count { it.enabled },
            "compiledRules" to compiledRules.size,
            "hostNormalization" to config.hostNormalization,
            "performance" to config.performance,
            "cacheStats" to ruleCompiler.getCacheStats()
        )
    }
    
    // Private helper methods
    
    private fun invalidateCompiledRulesCache() {
        cachedCompiledRules = null
        lastConfigHash = 0
    }
    
    private fun createHardcodedDefault(): AppConfig {
        return AppConfig(
            version = 2,
            removeAllParams = true,
            rules = listOf(
                CleaningRule(
                    hostPattern = "*",
                    params = listOf("utm_*", "fbclid", "gclid", "ref", "source"),
                    priority = RulePriority.GLOBAL_WILDCARD,
                    patternType = PatternType.WILDCARD,
                    enabled = true,
                    description = "Default global tracking parameter removal"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
    }
}
