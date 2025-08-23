package com.example.shareuntracked.utils

import android.content.ClipData
import android.content.Intent
import com.example.shareuntracked.data.AppConfig
import com.example.shareuntracked.data.CleaningRule

/**
 * Test utility class for creating consistent test data objects
 * used across multiple test classes.
 */
class TestDataBuilder {
    
    /**
     * Creates a default AppConfig for testing with common settings
     */
    fun createDefaultAppConfig(): AppConfig {
        return AppConfig(
            removeAllParams = false,
            rules = createTestCleaningRules()
        )
    }
    
    /**
     * Creates an AppConfig that removes all parameters
     */
    fun createRemoveAllParamsConfig(): AppConfig {
        return AppConfig(
            removeAllParams = true,
            rules = emptyList()
        )
    }
    
    /**
     * Creates a list of test cleaning rules for common scenarios
     */
    fun createTestCleaningRules(): List<CleaningRule> {
        return listOf(
            CleaningRule("*.twitter.com", listOf("t", "si")),
            CleaningRule("*.youtube.com", listOf("utm_source", "utm_medium", "utm_campaign")),
            CleaningRule("*.amazon.com", listOf("ref", "tag")),
            CleaningRule("example.com", listOf("tracking_id"))
        )
    }
    
    /**
     * Creates a single test cleaning rule
     */
    fun createSingleTestRule(hostPattern: String = "*.test.com", params: List<String> = listOf("param1", "param2")): CleaningRule {
        return CleaningRule(hostPattern, params)
    }
    
    /**
     * Creates a mock Intent with URL text for testing
     */
    fun createMockIntent(url: String): Intent {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_TEXT, url)
        return intent
    }
    
    /**
     * Creates a ClipData object with the provided text
     */
    fun createClipData(text: String): ClipData {
        return ClipData.newPlainText("test", text)
    }
    
    /**
     * Creates JSON string representation of default config for asset testing
     */
    fun createDefaultConfigJson(): String {
        return """
            {
                "removeAllParams": false,
                "rules": [
                    {
                        "hostPattern": "*.twitter.com",
                        "params": ["t", "si"]
                    },
                    {
                        "hostPattern": "*.youtube.com",
                        "params": ["utm_source", "utm_medium", "utm_campaign"]
                    }
                ]
            }
        """.trimIndent()
    }
    
    /**
     * Creates JSON string for remove-all-params config
     */
    fun createRemoveAllParamsConfigJson(): String {
        return """
            {
                "removeAllParams": true,
                "rules": []
            }
        """.trimIndent()
    }
    
    /**
     * Creates JSON string with invalid format for error testing
     */
    fun createInvalidConfigJson(): String {
        return "{ invalid json content"
    }
    
    /**
     * Creates a list of test URLs for various scenarios
     */
    fun createTestUrls(): Map<String, String> {
        return mapOf(
            "twitter_with_tracking" to "https://twitter.com/user/status/123?t=abc&si=def&other=keep",
            "youtube_with_utm" to "https://youtube.com/watch?v=123&utm_source=test&utm_medium=social",
            "amazon_with_ref" to "https://amazon.com/product/123?ref=tracking&tag=affiliate",
            "clean_url" to "https://example.com/page",
            "url_with_params" to "https://example.com/page?param1=value1&param2=value2",
            "invalid_url" to "not-a-url",
            "non_http_url" to "ftp://example.com/file"
        )
    }
    
    /**
     * Creates expected cleaned URLs for test validation
     */
    fun createExpectedCleanedUrls(): Map<String, String> {
        return mapOf(
            "twitter_cleaned" to "https://twitter.com/user/status/123?other=keep",
            "youtube_cleaned" to "https://youtube.com/watch?v=123",
            "amazon_cleaned" to "https://amazon.com/product/123",
            "remove_all_params" to "https://example.com/page"
        )
    }
}
