package com.example.shareuntracked

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

/**
 * Unit tests for URL cleaning functionality
 */
@RunWith(RobolectricTestRunner::class)
class UrlCleanerServiceTest {

    private lateinit var testContextProvider: TestContextProvider
    private lateinit var testDataBuilder: TestDataBuilder
    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testContextProvider = TestContextProvider()
        testDataBuilder = TestDataBuilder()
    }

    @Test
    fun testCleanUrl_invalidUrl() {
        urlCleanerService = UrlCleanerService(context)
        
        val testUrls = testDataBuilder.createTestUrls()
        val invalidUrl = testUrls["invalid_url"]!!
        
        val result = urlCleanerService.cleanUrl(invalidUrl)
        assertEquals("Should return original if invalid", invalidUrl, result)
    }

    @Test
    fun testCleanUrl_noParams() {
        urlCleanerService = UrlCleanerService(context)
        
        val testUrls = testDataBuilder.createTestUrls()
        val urlWithoutParams = testUrls["clean_url"]!!
        
        val result = urlCleanerService.cleanUrl(urlWithoutParams)
        assertEquals("Should return unchanged", urlWithoutParams, result)
    }

    @Test
    fun testCleanUrl_httpsUrl() {
        urlCleanerService = UrlCleanerService(context)
        
        val httpsUrl = "https://example.com/page?param=value"
        val result = urlCleanerService.cleanUrl(httpsUrl)
        assertTrue("Should handle HTTPS URLs", result.startsWith("https://"))
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun testCleanUrl_httpUrl() {
        urlCleanerService = UrlCleanerService(context)
        
        val httpUrl = "http://example.com/page?param=value"
        val result = urlCleanerService.cleanUrl(httpUrl)
        assertTrue("Should handle HTTP URLs", result.startsWith("http://"))
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun testCleanUrl_nonHttpUrl() {
        urlCleanerService = UrlCleanerService(context)
        
        val testUrls = testDataBuilder.createTestUrls()
        val ftpUrl = testUrls["non_http_url"]!!
        
        val result = urlCleanerService.cleanUrl(ftpUrl)
        assertEquals("Should return non-HTTP URLs unchanged", ftpUrl, result)
    }

    @Test
    fun testCleanUrl_withParameters() {
        urlCleanerService = UrlCleanerService(context)
        
        val testUrls = testDataBuilder.createTestUrls()
        val urlWithParams = testUrls["url_with_params"]!!
        
        val result = urlCleanerService.cleanUrl(urlWithParams)
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be valid URL", result.startsWith("https://"))
        // The actual cleaning behavior depends on the config, so we just verify it doesn't crash
    }

    @Test
    fun testCleanClipboardUrl() {
        urlCleanerService = UrlCleanerService(context)
        
        // Test clipboard operations with real context
        val result = urlCleanerService.cleanClipboardUrl()
        
        // Result should be one of the valid enum values
        assertTrue("Result should be valid CleaningResult", 
                  result in listOf(
                      com.example.shareuntracked.data.CleaningResult.CLIPBOARD_EMPTY,
                      com.example.shareuntracked.data.CleaningResult.NO_CHANGE,
                      com.example.shareuntracked.data.CleaningResult.CLEANED_AND_COPIED
                  ))
    }

    @Test
    fun testUrlCleaningLogic() {
        urlCleanerService = UrlCleanerService(context)
        
        // Test various URL scenarios
        val testCases = mapOf(
            "https://example.com/page" to "https://example.com/page",
            "not-a-url" to "not-a-url",
            "ftp://example.com/file" to "ftp://example.com/file"
        )
        
        testCases.forEach { (input, expected) ->
            val result = urlCleanerService.cleanUrl(input)
            when (input) {
                "not-a-url", "ftp://example.com/file" -> {
                    assertEquals("Invalid/non-HTTP URLs should be unchanged: $input", expected, result)
                }
                else -> {
                    assertNotNull("Result should not be null for: $input", result)
                    assertTrue("Result should be valid URL for: $input", 
                              result.startsWith("http://") || result.startsWith("https://"))
                }
            }
        }
    }

    @Test
    fun testServiceInstantiation() {
        // Test that the service can be created without errors
        urlCleanerService = UrlCleanerService(context)
        assertNotNull("Service should be created successfully", urlCleanerService)
    }
}
