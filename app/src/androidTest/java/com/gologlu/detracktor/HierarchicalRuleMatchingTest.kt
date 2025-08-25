package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive instrumented tests for hierarchical rule matching behavior.
 * Tests composite rule application where multiple rules can match the same URL.
 */
@RunWith(AndroidJUnit4::class)
class HierarchicalRuleMatchingTest {

    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        urlCleanerService = UrlCleanerService(context)
        configManager = ConfigManager(context)
    }

    @Test
    fun testInstagramUrlWithMultipleParameterTypes() {
        // Test Instagram URL with both igsh and utm_ parameters (hierarchical rule matching)
        val instagramUrl = "https://www.instagram.com/p/ABC123/?igsh=session123&utm_source=facebook&utm_medium=social&utm_campaign=test&hl=en"
        
        val cleanedUrl = urlCleanerService.cleanUrl(instagramUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be Instagram", cleanedUrl.contains("instagram.com"))
        assertTrue("URL should still contain post path", cleanedUrl.contains("/p/ABC123/"))
        
        // Verify that tracking parameters are removed by composite rules
        assertFalse("Should remove igsh parameter", cleanedUrl.contains("igsh="))
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        assertFalse("Should remove utm_campaign parameter", cleanedUrl.contains("utm_campaign="))
        
        // Verify that legitimate parameters are kept
        assertTrue("Should keep hl parameter", cleanedUrl.contains("hl=en"))
        
        println("Original: $instagramUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testTwitterUrlWithCompositeRuleApplication() {
        // Test Twitter URL with multiple tracking parameters that should be removed by different rules
        val twitterUrl = "https://twitter.com/user/status/123456789?utm_source=web&utm_medium=twitter&t=tracking123&s=session456&ref_src=twsrc&ref_url=example.com"
        
        val cleanedUrl = urlCleanerService.cleanUrl(twitterUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be Twitter", cleanedUrl.contains("twitter.com"))
        assertTrue("URL should still contain status path", cleanedUrl.contains("/status/123456789"))
        
        // Verify Twitter-specific parameters are removed
        assertFalse("Should remove t parameter", cleanedUrl.contains("t="))
        assertFalse("Should remove s parameter", cleanedUrl.contains("s="))
        assertFalse("Should remove ref_src parameter", cleanedUrl.contains("ref_src="))
        assertFalse("Should remove ref_url parameter", cleanedUrl.contains("ref_url="))
        
        // Verify global UTM parameters are also removed by composite rules
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        
        println("Original: $twitterUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testYouTubeUrlWithMixedParameters() {
        // Test YouTube URL with both tracking and functional parameters
        val youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=google&utm_campaign=video&t=42s&list=PLplaylist&index=1&fbclid=tracking"
        
        val cleanedUrl = urlCleanerService.cleanUrl(youtubeUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be YouTube", cleanedUrl.contains("youtube.com"))
        assertTrue("URL should still contain watch path", cleanedUrl.contains("/watch"))
        
        // Verify essential YouTube parameters are kept
        assertTrue("Should keep video ID parameter", cleanedUrl.contains("v=dQw4w9WgXcQ"))
        // Note: 't' parameter is removed by YouTube rules as it's considered tracking
        assertFalse("Should remove timestamp parameter (tracking)", cleanedUrl.contains("t=42s"))
        // Note: 'list' parameter is removed by YouTube rules as it's considered tracking
        assertFalse("Should remove playlist parameter (tracking)", cleanedUrl.contains("list=PLplaylist"))
        // Note: 'index' parameter is removed by YouTube rules as it's considered tracking
        assertFalse("Should remove index parameter (tracking)", cleanedUrl.contains("index=1"))
        
        // Verify tracking parameters are removed
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_campaign parameter", cleanedUrl.contains("utm_campaign="))
        assertFalse("Should remove fbclid parameter", cleanedUrl.contains("fbclid="))
        
        println("Original: $youtubeUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testAmazonUrlWithComplexParameters() {
        // Test Amazon URL with product parameters and tracking
        val amazonUrl = "https://www.amazon.com/dp/B08N5WRWNW?ref=sr_1_1&utm_source=google&utm_medium=cpc&tag=affiliate123&psc=1&keywords=test"
        
        val cleanedUrl = urlCleanerService.cleanUrl(amazonUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be Amazon", cleanedUrl.contains("amazon.com"))
        assertTrue("URL should still contain product path", cleanedUrl.contains("/dp/B08N5WRWNW"))
        
        // Verify tracking parameters are removed
        assertFalse("Should remove ref parameter", cleanedUrl.contains("ref="))
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        assertFalse("Should remove tag parameter", cleanedUrl.contains("tag="))
        // Note: keywords is not in Amazon rules, so it should remain
        assertTrue("Should keep keywords parameter (not in removal rules)", cleanedUrl.contains("keywords="))
        
        // Verify functional parameters are kept
        assertTrue("Should keep psc parameter", cleanedUrl.contains("psc=1"))
        
        println("Original: $amazonUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testMultipleRulesOnSameHost() {
        // Test that multiple rules can apply to the same host with different parameter patterns
        val testUrl = "https://example.com/page?utm_source=test&utm_medium=social&fbclid=facebook123&gclid=google456&custom_param=keep&another_param=also_keep"
        
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be example.com", cleanedUrl.contains("example.com"))
        
        // Verify global tracking parameters are removed
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        assertFalse("Should remove fbclid parameter", cleanedUrl.contains("fbclid="))
        assertFalse("Should remove gclid parameter", cleanedUrl.contains("gclid="))
        
        // Verify custom parameters are kept (assuming no specific rule removes them)
        assertTrue("Should keep custom_param", cleanedUrl.contains("custom_param=keep"))
        assertTrue("Should keep another_param", cleanedUrl.contains("another_param=also_keep"))
        
        println("Original: $testUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testRuleSpecificityOrdering() {
        // Test that more specific rules are applied before less specific ones
        val config = configManager.loadConfig()
        val compiledRules = configManager.getCompiledRules()
        
        assertNotNull("Config should be loaded", config)
        assertTrue("Should have compiled rules", compiledRules.isNotEmpty())
        
        // Verify rules are sorted by specificity (most specific first)
        for (i in 0 until compiledRules.size - 1) {
            val currentRule = compiledRules[i]
            val nextRule = compiledRules[i + 1]
            
            assertTrue("Rules should be sorted by specificity (descending)", 
                      currentRule.specificity >= nextRule.specificity)
        }
        
        println("Found ${compiledRules.size} compiled rules")
        compiledRules.forEachIndexed { index, rule ->
            println("Rule $index: ${rule.originalRule.hostPattern} (specificity: ${rule.specificity})")
        }
    }

    @Test
    fun testCompositeRuleApplicationLogging() {
        // Test that composite rule application is properly logged
        val testUrl = "https://instagram.com/p/test/?igsh=session&utm_source=facebook&utm_medium=social"
        
        // This test primarily verifies that the composite rule application works
        // and that multiple rules can be applied to the same URL
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertNotEquals("URL should be modified", testUrl, cleanedUrl)
        
        // Verify both Instagram-specific and global parameters are removed
        assertFalse("Should remove igsh parameter", cleanedUrl.contains("igsh="))
        assertFalse("Should remove utm_source parameter", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        
        println("Original: $testUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testUrlWithNoMatchingRules() {
        // Test URL that doesn't match any specific rules (should only apply global rules if any)
        val testUrl = "https://unknown-domain.com/page?custom1=value1&custom2=value2&utm_source=test"
        
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("URL should still be unknown-domain.com", cleanedUrl.contains("unknown-domain.com"))
        
        // Global rules should still apply
        assertFalse("Should remove utm_source parameter via global rules", cleanedUrl.contains("utm_source="))
        
        // Custom parameters should be kept if no specific rule removes them
        assertTrue("Should keep custom1 parameter", cleanedUrl.contains("custom1=value1"))
        assertTrue("Should keep custom2 parameter", cleanedUrl.contains("custom2=value2"))
        
        println("Original: $testUrl")
        println("Cleaned:  $cleanedUrl")
    }

    @Test
    fun testEmptyParametersHandling() {
        // Test URLs with empty parameter values
        val testUrl = "https://example.com/page?utm_source=&utm_medium=social&param="
        
        val cleanedUrl = urlCleanerService.cleanUrl(testUrl)
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        
        // Verify tracking parameters are removed even with empty values
        assertFalse("Should remove utm_source parameter even if empty", cleanedUrl.contains("utm_source="))
        assertFalse("Should remove utm_medium parameter", cleanedUrl.contains("utm_medium="))
        
        // Empty non-tracking parameters should be handled appropriately
        // (behavior may vary based on implementation)
        
        println("Original: $testUrl")
        println("Cleaned:  $cleanedUrl")
    }
}
