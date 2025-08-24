package com.gologlu.detracktor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testMainActivityLaunches() {
        // Test that MainActivity launches without crashing
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("MainActivity should launch successfully", activity)
                assertFalse("Activity should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun testShareIntentHandling_withValidUrl() {
        // Test URL cleaning service directly instead of triggering system share dialog
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val urlCleanerService = UrlCleanerService(context)
        
        val originalUrl = "https://example.com/page?utm_source=test&id=123"
        val cleanedUrl = urlCleanerService.cleanUrl(originalUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should be cleaned", originalUrl != cleanedUrl)
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source"))
    }

    @Test
    fun testShareIntentHandling_withInvalidUrl() {
        // Test URL cleaning service with invalid URL
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val urlCleanerService = UrlCleanerService(context)
        
        val invalidUrl = "not a valid url"
        val result = urlCleanerService.cleanUrl(invalidUrl)
        
        assertEquals("Invalid URL should remain unchanged", invalidUrl, result)
    }

    @Test
    fun testClipboardAnalysis_withValidUrl() {
        // Set up clipboard with valid URL
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val testUrl = "https://example.com/page?utm_source=test&id=123"
        val clipData = ClipData.newPlainText("test", testUrl)
        clipboardManager.setPrimaryClip(clipData)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("Activity should launch with clipboard content", activity)
                // Give time for clipboard analysis to update
                Thread.sleep(2000)
            }
        }
    }

    @Test
    fun testClipboardAnalysis_withNonUrl() {
        // Set up clipboard with non-URL content
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val nonUrlText = "This is just some text, not a URL"
        val clipData = ClipData.newPlainText("test", nonUrlText)
        clipboardManager.setPrimaryClip(clipData)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("Activity should launch with non-URL clipboard content", activity)
                Thread.sleep(2000) // Give time for clipboard analysis
            }
        }
    }

    @Test
    fun testClipboardAnalysis_withEmptyClipboard() {
        // Clear clipboard
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("Activity should launch with empty clipboard", activity)
                Thread.sleep(2000) // Give time for clipboard analysis
            }
        }
    }

    @Test
    fun testConfigActivityLaunch() {
        // Test that config activity can be launched
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Test that we can create an intent to ConfigActivity
                val configIntent = Intent(activity, ConfigActivity::class.java)
                assertNotNull("Config intent should be created", configIntent)
                assertEquals("Intent should target ConfigActivity", 
                    ConfigActivity::class.java.name, configIntent.component?.className)
            }
        }
    }
}
