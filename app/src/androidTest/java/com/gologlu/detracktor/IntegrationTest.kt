package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.gologlu.detracktor.data.CleaningResult
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule

/**
 * Integration tests for the Detracktor app
 * These tests run on actual Android devices/emulators and test component interactions
 */
@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.gologlu.detracktor", appContext.packageName)
    }

    @Test
    fun testConfigManagerIntegration() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configManager = ConfigManager(appContext)
        
        // Test that we can load default config
        val config = configManager.getDefaultConfig()
        assertNotNull("Config should not be null", config)
        
        // Test that we can get compiled rules
        val rules = configManager.getCompiledRules()
        assertNotNull("Rules should not be null", rules)
        
        // Test config persistence
        val originalConfig = configManager.loadConfig()
        configManager.saveConfig(originalConfig)
        val loadedConfig = configManager.loadConfig()
        assertEquals("Loaded config should match saved config", 
                    originalConfig.removeAllParams, loadedConfig.removeAllParams)
    }

    @Test
    fun testUrlCleanerServiceIntegration() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val urlCleanerService = UrlCleanerService(appContext)
        
        // Test basic URL cleaning with real context
        val testUrl = "https://example.com/page?utm_source=test&param=value"
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("Cleaned URL should be valid", cleanedUrl.startsWith("https://"))
        
        // Test various URL scenarios
        testUrlCleaningScenarios(urlCleanerService)
    }

    @Test
    fun testMainActivityLaunch() {
        // Test that MainActivity can be launched without crashing
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertEquals("Should be MainActivity", MainActivity::class.java, activity.javaClass)
        }
    }

    @Test
    fun testEndToEndUrlCleaning() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configManager = ConfigManager(appContext)
        val urlCleanerService = UrlCleanerService(appContext)
        
        // Test complete workflow: load config -> clean URL -> verify result
        val config = configManager.loadConfig()
        assertNotNull("Config should be loaded", config)
        
        // Test with tracking parameters that should be removed by composite rules
        // Twitter-specific: ["s", "t", "ref_src", "ref_url"] + Global: ["utm_source", "utm_medium", ...]
        val dirtyUrl = "https://twitter.com/user/status/123?utm_source=test&utm_medium=social&t=tracking&s=session"
        val cleanedUrl = urlCleanerService.cleanUrl(dirtyUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be valid", cleanedUrl.startsWith("https://twitter.com"))
        // Check that at least some parameters were removed (the URL should be different)
        assertTrue("Should remove tracking parameters", dirtyUrl != cleanedUrl)
        // Verify Twitter-specific parameters are removed: ["s", "t", "ref_src", "ref_url"]
        assertFalse("Should remove t parameter", cleanedUrl.contains("t="))
        assertFalse("Should remove s parameter", cleanedUrl.contains("s="))
        // Verify global UTM parameters are also removed by composite rules
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source"))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium"))
    }

    @Test
    fun testConfigManagerFileOperations() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configManager = ConfigManager(appContext)
        
        // Test reset to default
        configManager.resetToDefault()
        val defaultConfig = configManager.loadConfig()
        assertNotNull("Default config should be loaded", defaultConfig)
        
        // Test that compiled rules are available
        val compiledRules = configManager.getCompiledRules()
        assertNotNull("Compiled rules should be available", compiledRules)
    }

    @Test
    fun testUrlCleanerServiceWithRealClipboard() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val urlCleanerService = UrlCleanerService(appContext)
        
        // Test clipboard operations (this will use the real clipboard)
        val result = urlCleanerService.cleanClipboardUrl()
        
        // Result should be one of the valid enum values (including NOT_A_URL)
        assertTrue("Result should be valid CleaningResult", 
                  result in listOf(CleaningResult.CLIPBOARD_EMPTY, 
                                 CleaningResult.NOT_A_URL,
                                 CleaningResult.NO_CHANGE, 
                                 CleaningResult.CLEANED_AND_COPIED))
    }

    private fun testUrlCleaningScenarios(urlCleanerService: UrlCleanerService) {
        val testCases = mapOf(
            "https://example.com/page" to "https://example.com/page", // No change
            "https://example.com/page?param=value" to "https://example.com/page", // Should clean based on config
            "not-a-url" to "not-a-url", // Invalid URL unchanged
            "ftp://example.com/file" to "ftp://example.com/file" // Non-HTTP unchanged
        )
        
        testCases.forEach { (input, expected) ->
            val result = urlCleanerService.cleanUrl(input)
            when (input) {
                "not-a-url", "ftp://example.com/file" -> {
                    assertEquals("Invalid/non-HTTP URLs should be unchanged", expected, result)
                }
                else -> {
                    assertNotNull("Result should not be null for: $input", result)
                    assertTrue("Result should be valid URL for: $input", 
                              result.startsWith("http://") || result.startsWith("https://"))
                }
            }
        }
    }
}
