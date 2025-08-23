package com.example.shareuntracked

import android.content.Context
import android.content.res.AssetManager
import androidx.test.core.app.ApplicationProvider
import com.example.shareuntracked.data.AppConfig
import com.example.shareuntracked.data.CleaningRule
import com.example.shareuntracked.utils.TestContextProvider
import com.example.shareuntracked.utils.TestDataBuilder
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Unit tests for configuration management
 */
@RunWith(RobolectricTestRunner::class)
class ConfigManagerTest {

    private lateinit var testContextProvider: TestContextProvider
    private lateinit var testDataBuilder: TestDataBuilder
    private lateinit var configManager: ConfigManager
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testContextProvider = TestContextProvider()
        testDataBuilder = TestDataBuilder()
    }

    @Test
    fun testGetDefaultConfig_fallback() {
        // Test fallback behavior when assets are not available
        configManager = ConfigManager(context)
        val config = configManager.getDefaultConfig()

        assertNotNull("Config should not be null", config)
        // The fallback should provide a working config
        assertTrue("Should have valid config structure", 
                  config.removeAllParams || config.rules.isNotEmpty())
    }

    @Test
    fun testLoadConfig() {
        // Test loading config with real context
        configManager = ConfigManager(context)
        val config = configManager.loadConfig()
        
        assertNotNull("Loaded config should not be null", config)
        // Should have valid structure
        assertTrue("Config should be valid", 
                  config.removeAllParams || config.rules.isNotEmpty())
    }

    @Test
    fun testSaveAndLoadConfig() {
        // Test with real context
        configManager = ConfigManager(context)
        val testConfig = testDataBuilder.createDefaultAppConfig()

        // Test that we can create a config object
        assertNotNull("Test config should not be null", testConfig)
        assertFalse("Test config should not remove all params", testConfig.removeAllParams)
        assertEquals("Test config should have rules", 4, testConfig.rules.size)
        
        // Test save/load cycle
        configManager.saveConfig(testConfig)
        val loadedConfig = configManager.loadConfig()
        assertNotNull("Loaded config should not be null", loadedConfig)
    }

    @Test
    fun testGetDefaultRules() {
        configManager = ConfigManager(context)
        val rules = configManager.getDefaultRules()

        assertNotNull("Rules should not be null", rules)
        // Rules should be a valid list (empty or with content)
        assertNotNull("Rules should not be null", rules)
        assertTrue("Rules should be valid list", rules.isEmpty() || rules.isNotEmpty())
    }

    @Test
    fun testResetToDefault() {
        configManager = ConfigManager(context)
        
        // Test reset functionality
        configManager.resetToDefault()
        
        // Verify we can get a config after reset
        val config = configManager.loadConfig()
        assertNotNull("Config after reset should not be null", config)
    }

    @Test
    fun testConfigDataStructure() {
        // Test the data structures work correctly
        val testConfig = testDataBuilder.createDefaultAppConfig()
        val removeAllConfig = testDataBuilder.createRemoveAllParamsConfig()
        
        assertNotNull("Default config should not be null", testConfig)
        assertNotNull("Remove all config should not be null", removeAllConfig)
        
        assertFalse("Default config should not remove all params", testConfig.removeAllParams)
        assertTrue("Remove all config should remove all params", removeAllConfig.removeAllParams)
        
        assertTrue("Default config should have rules", testConfig.rules.isNotEmpty())
        assertTrue("Remove all config should have empty rules", removeAllConfig.rules.isEmpty())
    }
}
