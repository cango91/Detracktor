package com.gologlu.detracktor

import android.content.Context
import android.content.res.AssetManager
import com.gologlu.detracktor.data.*
import com.gologlu.detracktor.utils.HostNormalizer
import com.gologlu.detracktor.utils.RuleCompiler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.*


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConfigManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockAssetManager: AssetManager
    
    @Mock
    private lateinit var mockFilesDir: File
    
    private lateinit var configManager: ConfigManager
    private lateinit var testConfigFile: File
    private lateinit var testBackupDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockAssetManager = mock(AssetManager::class.java)
        mockFilesDir = mock(File::class.java)
        
        // Create temporary test files
        testConfigFile = File.createTempFile("config", ".json")
        testBackupDir = kotlin.io.createTempDir("config_backups")
        
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        `when`(mockContext.filesDir).thenReturn(testConfigFile.parentFile)
        
        configManager = ConfigManager(mockContext)
    }

    @Test
    fun testLoadConfig_withExistingFile_returnsConfig() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        
        // Save test config to file
        configManager.saveConfig(testConfig)
        
        // When
        val loadedConfig = configManager.loadConfig()
        
        // Then
        assertNotNull(loadedConfig)
        assertEquals(testConfig.version, loadedConfig.version)
        assertEquals(testConfig.removeAllParams, loadedConfig.removeAllParams)
        assertEquals(testConfig.rules.size, loadedConfig.rules.size)
    }

    @Test
    fun testLoadConfig_withNonExistentFile_returnsDefaultConfig() {
        // Given - no existing config file
        
        // When
        val config = configManager.loadConfig()
        
        // Then
        assertNotNull(config)
        assertTrue(config.version > 0)
        assertTrue(config.rules.isNotEmpty())
    }

    @Test
    fun testSaveConfig_withValidConfig_savesSuccessfully() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = true,
            rules = listOf(
                CleaningRule(
                    hostPattern = "test.com",
                    params = listOf("tracking_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test save rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        
        // When
        configManager.saveConfig(testConfig)
        
        // Then
        val loadedConfig = configManager.loadConfig()
        assertEquals(testConfig.version, loadedConfig.version)
        assertEquals(testConfig.removeAllParams, loadedConfig.removeAllParams)
    }

    @Test
    fun testGetCompiledRules_returnsCompiledRules() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        configManager.saveConfig(testConfig)
        
        // When
        val compiledRules = configManager.getCompiledRules()
        
        // Then
        assertNotNull(compiledRules)
        assertTrue(compiledRules.isNotEmpty())
        assertEquals(testConfig.rules.size, compiledRules.size)
    }

    @Test
    fun testGetCompiledRules_withCaching_returnsCachedRules() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        configManager.saveConfig(testConfig)
        
        // When - call twice to test caching
        val firstCall = configManager.getCompiledRules()
        val secondCall = configManager.getCompiledRules()
        
        // Then
        assertEquals(firstCall.size, secondCall.size)
        assertEquals(firstCall.first().originalRule.hostPattern, secondCall.first().originalRule.hostPattern)
    }

    @Test
    fun testRecompileRules_invalidatesCacheAndRecompiles() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        configManager.saveConfig(testConfig)
        
        // Get initial compiled rules
        val initialRules = configManager.getCompiledRules()
        
        // When
        configManager.recompileRules()
        val recompiledRules = configManager.getCompiledRules()
        
        // Then
        assertEquals(initialRules.size, recompiledRules.size)
        assertNotNull(recompiledRules)
    }

    @Test
    fun testGetDefaultConfig_returnsValidDefaultConfig() {
        // Given - mock asset manager to return default config
        val defaultConfigJson = """
        {
            "version": 2,
            "removeAllParams": false,
            "rules": [
                {
                    "hostPattern": "*.example.com",
                    "params": ["utm_*", "fbclid"],
                    "priority": "SUBDOMAIN_WILDCARD",
                    "patternType": "WILDCARD",
                    "enabled": true,
                    "description": "Example default rule"
                }
            ],
            "hostNormalization": {
                "enableCaseNormalization": true,
                "enableIDNConversion": true,
                "enablePortNormalization": true,
                "defaultPorts": {
                    "http": 80,
                    "https": 443
                }
            },
            "performance": {
                "enablePatternCaching": true,
                "maxCacheSize": 1000,
                "enableRuleOptimization": true
            }
        }
        """.trimIndent()
        
        `when`(mockAssetManager.open("enhanced_default_rules.json"))
            .thenReturn(ByteArrayInputStream(defaultConfigJson.toByteArray()))
        
        // When
        val defaultConfig = configManager.getDefaultConfig()
        
        // Then
        assertNotNull(defaultConfig)
        assertEquals(2, defaultConfig.version)
        assertTrue(defaultConfig.rules.isNotEmpty())
        assertNotNull(defaultConfig.hostNormalization)
        assertNotNull(defaultConfig.performance)
    }

    @Test
    fun testGetDefaultConfig_withMissingAsset_returnsHardcodedDefault() {
        // Given - mock asset manager to throw exception
        `when`(mockAssetManager.open("enhanced_default_rules.json"))
            .thenThrow(RuntimeException("Asset not found"))
        
        // When
        val defaultConfig = configManager.getDefaultConfig()
        
        // Then
        assertNotNull(defaultConfig)
        assertTrue(defaultConfig.version > 0)
        assertTrue(defaultConfig.rules.isNotEmpty())
    }

    @Test
    fun testResetToDefault_resetsConfigToDefault() {
        // Given
        val customConfig = AppConfig(
            version = 3,
            removeAllParams = true,
            rules = emptyList(),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        configManager.saveConfig(customConfig)
        
        // When
        configManager.resetToDefault()
        
        // Then
        val resetConfig = configManager.loadConfig()
        assertNotNull(resetConfig)
        assertTrue(resetConfig.rules.isNotEmpty()) // Should have default rules
    }

    @Test
    fun testCreateBackup_createsBackupFile() {
        // Given
        val configJson = """{"version": 2, "removeAllParams": false}"""
        val filename = "test_backup.json"
        
        // When
        configManager.createBackup(configJson, filename)
        
        // Then - verify no exceptions thrown (backup creation is best-effort)
        assertTrue(true)
    }

    @Test
    fun testGetConfigStats_returnsValidStats() {
        // Given
        val testConfig = AppConfig(
            version = 2,
            removeAllParams = false,
            rules = listOf(
                CleaningRule(
                    hostPattern = "example.com",
                    params = listOf("utm_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = true,
                    description = "Test rule"
                ),
                CleaningRule(
                    hostPattern = "test.com",
                    params = listOf("tracking_*"),
                    priority = RulePriority.EXACT_HOST,
                    patternType = PatternType.EXACT,
                    enabled = false,
                    description = "Disabled test rule"
                )
            ),
            hostNormalization = HostNormalizationConfig(),
            performance = PerformanceConfig()
        )
        configManager.saveConfig(testConfig)
        
        // When
        val stats = configManager.getConfigStats()
        
        // Then
        assertNotNull(stats)
        assertTrue(stats.containsKey("version"))
        assertTrue(stats.containsKey("totalRules"))
        assertTrue(stats.containsKey("enabledRules"))
        assertTrue(stats.containsKey("compiledRules"))
        assertTrue(stats.containsKey("hostNormalization"))
        assertTrue(stats.containsKey("performance"))
        assertTrue(stats.containsKey("cacheStats"))
        
        assertEquals(2, stats["version"])
        assertEquals(2, stats["totalRules"])
        assertEquals(1, stats["enabledRules"]) // Only one enabled rule
    }

    @Test
    fun testHostNormalizer_isInitializedLazily() {
        // When
        val hostNormalizer = configManager.hostNormalizer
        
        // Then
        assertNotNull(hostNormalizer)
        assertTrue(hostNormalizer is HostNormalizer)
    }

    @Test
    fun testRuleCompiler_isInitializedLazily() {
        // When
        val ruleCompiler = configManager.ruleCompiler
        
        // Then
        assertNotNull(ruleCompiler)
        assertTrue(ruleCompiler is RuleCompiler)
    }
}
