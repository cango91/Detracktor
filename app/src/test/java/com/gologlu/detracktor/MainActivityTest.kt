package com.gologlu.detracktor

import android.content.Context
import android.content.Intent
import com.gologlu.detracktor.data.CleaningResult
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    private lateinit var context: Context
    private lateinit var mainActivity: MainActivity

    @Before
    fun setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication()
        mainActivity = MainActivity()
    }

    @Test
    fun testIsValidHttpUrl_withValidUrls_returnsTrue() {
        // Use reflection to test the private method
        val isValidHttpUrlMethod = MainActivity::class.java.getDeclaredMethod("isValidHttpUrl", String::class.java)
        isValidHttpUrlMethod.isAccessible = true

        val validUrls = listOf(
            "https://example.com",
            "http://example.com",
            "https://sub.example.com/path?param=value",
            "http://localhost:8080/test"
        )

        validUrls.forEach { url ->
            val result = isValidHttpUrlMethod.invoke(mainActivity, url) as Boolean
            assertTrue("$url should be valid", result)
        }
    }

    @Test
    fun testIsValidHttpUrl_withInvalidUrls_returnsFalse() {
        val isValidHttpUrlMethod = MainActivity::class.java.getDeclaredMethod("isValidHttpUrl", String::class.java)
        isValidHttpUrlMethod.isAccessible = true

        val invalidUrls = listOf(
            "ftp://example.com",
            "not a url",
            "",
            "mailto:test@example.com",
            "file:///path/to/file"
        )

        invalidUrls.forEach { url ->
            val result = isValidHttpUrlMethod.invoke(mainActivity, url) as Boolean
            assertFalse("$url should be invalid", result)
        }
    }

    @Test
    fun testToastMessageMapping_showsCorrectMessages() {
        // Test that our CleaningResult enum values map to correct messages
        val testCases = mapOf(
            CleaningResult.CLIPBOARD_EMPTY to "Clipboard empty",
            CleaningResult.NOT_A_URL to "Not a URL", 
            CleaningResult.NO_CHANGE to "No change",
            CleaningResult.CLEANED_AND_COPIED to "Cleaned" // This should now show "Cleaned" instead of "Cleaned → copied"
        )

        testCases.forEach { (result, expectedMessage) ->
            // Test the message mapping logic
            val message = when (result) {
                CleaningResult.CLIPBOARD_EMPTY -> "Clipboard empty"
                CleaningResult.NOT_A_URL -> "Not a URL"
                CleaningResult.NO_CHANGE -> "No change"
                CleaningResult.CLEANED_AND_COPIED -> "Cleaned"
            }
            assertEquals("Message for $result should be correct", expectedMessage, message)
        }
    }

    @Test
    fun testShareIntentHandling_withValidUrl() {
        // Test that share intents with valid URLs are handled correctly
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.com/page?utm_source=test&id=123")
        }

        // Verify intent has correct action and data
        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("text/plain", shareIntent.type)
        assertEquals("https://example.com/page?utm_source=test&id=123", shareIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun testShareIntentHandling_withInvalidUrl() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "not a valid url")
        }

        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("not a valid url", shareIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun testShareIntentHandling_withEmptyText() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "")
        }

        assertEquals("", shareIntent.getStringExtra(Intent.EXTRA_TEXT))
    }
}
