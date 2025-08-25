package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Performance instrumented tests for URL cleaning operations.
 * Tests performance with large parameter sets and complex rule matching scenarios.
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        urlCleanerService = UrlCleanerService(context)
        configManager = ConfigManager(context)
    }

    @Test
    fun testPerformanceWithManyParameters() {
        // Test performance with URLs containing many parameters
        val baseUrl = "https://example.com/page"
        val trackingParams = listOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
            "fbclid", "gclid", "msclkid", "twclid", "li_fat_id",
            "ref", "source", "campaign", "medium", "content",
            "tracking_id", "session_id", "visitor_id", "click_id", "ad_id"
        )
        val functionalParams = listOf(
            "id", "page", "sort", "filter", "limit", "offset",
            "lang", "locale", "theme", "format", "version",
            "category", "tag", "search", "query", "type"
        )

        // Create URL with many parameters (mix of tracking and functional)
        val allParams = mutableListOf<String>()
        trackingParams.forEach { param ->
            allParams.add("$param=value_$param")
        }
        functionalParams.forEach { param ->
            allParams.add("$param=value_$param")
        }
        
        val complexUrl = "$baseUrl?${allParams.joinToString("&")}"
        
        val startTime = System.currentTimeMillis()
        val cleanedUrl = urlCleanerService.cleanUrl(complexUrl)
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        
        assertNotNull("Cleaned URL should not be null", cleanedUrl)
        assertTrue("Processing should complete in reasonable time", processingTime < 500) // Less than 500ms
        
        // Verify tracking parameters are removed (only those in the rules)
        val removedParams = listOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
            "fbclid", "gclid", "msclkid", "_ga", "_gl"
        )
        removedParams.forEach { param ->
            assertFalse("Should remove $param", cleanedUrl.contains("$param="))
        }
        
        // Parameters not in rules should remain
        val keptParams = listOf("twclid", "li_fat_id", "ref", "source", "campaign", "medium", "content",
                               "tracking_id", "session_id", "visitor_id", "click_id", "ad_id")
        keptParams.forEach { param ->
            assertTrue("Should keep $param (not in removal rules)", cleanedUrl.contains("$param="))
        }
        
        // Verify functional parameters are kept
        functionalParams.forEach { param ->
            assertTrue("Should keep $param", cleanedUrl.contains("$param="))
        }
        
        println("Complex URL with ${allParams.size} parameters processed in ${processingTime}ms")
        println("Original length: ${complexUrl.length}, Cleaned length: ${cleanedUrl.length}")
    }

    @Test
    fun testPerformanceWithMultipleUrls() {
        // Test performance when processing multiple URLs in sequence
        val testUrls = listOf(
            "https://instagram.com/p/test/?igsh=session&utm_source=facebook&utm_medium=social",
            "https://twitter.com/user/status/123?utm_source=web&t=tracking&s=session&ref_src=twsrc",
            "https://youtube.com/watch?v=abc123&utm_source=google&utm_campaign=video&t=30s",
            "https://amazon.com/dp/B123?ref=sr_1_1&utm_source=google&tag=affiliate&psc=1",
            "https://github.com/user/repo/issues?utm_source=newsletter&utm_campaign=weekly",
            "https://reddit.com/r/test/comments/123/?utm_source=share&utm_medium=web2x",
            "https://linkedin.com/in/user/?utm_source=share&utm_medium=member_desktop",
            "https://pinterest.com/pin/123/?utm_source=android&utm_medium=share",
            "https://tiktok.com/@user/video/123?utm_source=copy&utm_medium=android",
            "https://snapchat.com/add/user?utm_source=web&utm_campaign=profile_share"
        )

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<String>()
        
        // Process all URLs
        testUrls.forEach { url ->
            results.add(urlCleanerService.cleanUrl(url))
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime.toDouble() / testUrls.size

        assertTrue("Should process all URLs", results.size == testUrls.size)
        assertTrue("Total processing should complete in reasonable time", totalTime < 2000) // Less than 2 seconds
        assertTrue("Average processing time should be reasonable", averageTime < 200) // Less than 200ms per URL

        println("Processed ${testUrls.size} URLs in ${totalTime}ms (average: ${averageTime}ms per URL)")
        
        // Verify all results are valid
        results.forEachIndexed { index, result ->
            assertNotNull("Result $index should not be null", result)
            assertTrue("Result $index should be valid URL", 
                      result.startsWith("http://") || result.startsWith("https://"))
        }
    }

    @Test
    fun testPerformanceWithRepeatedOperations() {
        // Test performance with repeated operations on the same URL
        val testUrl = "https://example.com/page?utm_source=google&utm_medium=cpc&utm_campaign=test&id=123&lang=en"
        val iterations = 1000

        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            urlCleanerService.cleanUrl(testUrl)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime.toDouble() / iterations

        assertTrue("Repeated operations should complete in reasonable time", totalTime < 5000) // Less than 5 seconds
        assertTrue("Average operation time should be very fast", averageTime < 5) // Less than 5ms per operation

        println("Performed $iterations operations in ${totalTime}ms (average: ${averageTime}ms per operation)")
    }

    @Test
    fun testPerformanceWithComplexRuleMatching() {
        // Test performance when multiple complex rules need to be evaluated
        val config = configManager.loadConfig()
        val compiledRules = configManager.getCompiledRules()
        
        // Test URL that might match multiple rules
        val complexUrl = "https://www.instagram.com/p/ABC123/?igsh=session123&utm_source=facebook&utm_medium=social&utm_campaign=test&fbclid=tracking&gclid=google&ref=share&source=web"
        
        val startTime = System.currentTimeMillis()
        
        // Perform rule matching multiple times
        repeat(100) {
            urlCleanerService.cleanUrl(complexUrl)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        assertTrue("Complex rule matching should complete in reasonable time", totalTime < 3000) // Less than 3 seconds

        println("Complex rule matching performed 100 times in ${totalTime}ms")
        println("Available rules: ${compiledRules.size}")
        println("Enabled rules: ${compiledRules.count { it.originalRule.enabled }}")
    }

    @Test
    fun testPerformanceWithLargeRuleSet() {
        // Test performance impact of having many rules
        val config = configManager.loadConfig()
        val stats = configManager.getConfigStats()
        
        val testUrls = listOf(
            "https://example1.com/page?utm_source=test&id=123",
            "https://example2.com/api?utm_medium=social&version=v1",
            "https://example3.com/search?fbclid=tracking&q=test",
            "https://example4.com/product?gclid=google&category=tech",
            "https://example5.com/article?utm_campaign=newsletter&author=john"
        )

        val startTime = System.currentTimeMillis()
        
        testUrls.forEach { url ->
            repeat(20) { // 20 iterations per URL
                urlCleanerService.cleanUrl(url)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val totalOperations = testUrls.size * 20

        assertTrue("Large rule set processing should complete in reasonable time", totalTime < 5000) // Less than 5 seconds

        println("Processed $totalOperations operations with large rule set in ${totalTime}ms")
        println("Rule set stats: $stats")
    }

    @Test
    fun testMemoryUsageWithLargeUrls() {
        // Test memory usage with very large URLs
        val baseUrl = "https://example.com/page"
        val largeValue = "x".repeat(10000) // 10KB parameter value
        val largeUrl = "$baseUrl?data=$largeValue&utm_source=test&utm_medium=social&id=123"

        // Get initial memory usage
        System.gc()
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        val startTime = System.currentTimeMillis()
        
        // Process large URL multiple times
        repeat(50) {
            urlCleanerService.cleanUrl(largeUrl)
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        // Get final memory usage
        System.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        assertTrue("Large URL processing should complete in reasonable time", processingTime < 3000) // Less than 3 seconds
        assertTrue("Memory usage should not increase excessively", memoryIncrease < 50 * 1024 * 1024) // Less than 50MB

        println("Processed large URLs (${largeUrl.length} chars) 50 times in ${processingTime}ms")
        println("Memory increase: ${memoryIncrease / 1024}KB")
    }

    @Test
    fun testConcurrentPerformance() {
        // Test performance under concurrent access (simulated)
        val testUrls = listOf(
            "https://example1.com/page?utm_source=test1&id=123",
            "https://example2.com/page?utm_source=test2&id=456",
            "https://example3.com/page?utm_source=test3&id=789",
            "https://example4.com/page?utm_source=test4&id=101",
            "https://example5.com/page?utm_source=test5&id=112"
        )

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<String>()

        // Simulate concurrent access by rapidly switching between different URLs
        repeat(200) { iteration ->
            val url = testUrls[iteration % testUrls.size]
            results.add(urlCleanerService.cleanUrl(url))
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        assertEquals("Should process all requests", 200, results.size)
        assertTrue("Concurrent-like processing should complete in reasonable time", totalTime < 3000) // Less than 3 seconds

        // Verify all results are valid
        results.forEach { result ->
            assertNotNull("Result should not be null", result)
            assertFalse("Should remove utm_source", result.contains("utm_source="))
        }

        println("Processed 200 concurrent-like requests in ${totalTime}ms")
    }

    @Test
    fun testConfigLoadingPerformance() {
        // Test performance of config loading and rule compilation
        val iterations = 10

        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            // Force config reload and rule recompilation
            configManager.recompileRules()
            configManager.getCompiledRules()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime.toDouble() / iterations

        assertTrue("Config loading should complete in reasonable time", totalTime < 5000) // Less than 5 seconds
        assertTrue("Average config loading should be fast", averageTime < 500) // Less than 500ms per load

        println("Config loading performed $iterations times in ${totalTime}ms (average: ${averageTime}ms)")
    }

    @Test
    fun testServiceStatsPerformance() {
        // Test performance of service statistics generation
        val iterations = 100

        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            urlCleanerService.getServiceStats()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        assertTrue("Service stats generation should be fast", totalTime < 1000) // Less than 1 second

        println("Service stats generated $iterations times in ${totalTime}ms")
    }

    @Test
    fun testUrlAnalysisPerformance() {
        // Test performance of URL analysis for debugging
        val testUrls = listOf(
            "https://instagram.com/p/test/?igsh=session&utm_source=facebook",
            "https://twitter.com/user/status/123?utm_source=web&t=tracking",
            "https://youtube.com/watch?v=abc123&utm_source=google&t=30s",
            "https://amazon.com/dp/B123?ref=sr_1_1&utm_source=google",
            "https://github.com/user/repo?utm_source=newsletter"
        )

        val startTime = System.currentTimeMillis()
        
        testUrls.forEach { url ->
            repeat(20) {
                urlCleanerService.testUrlCleaning(url)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val totalOperations = testUrls.size * 20

        assertTrue("URL analysis should complete in reasonable time", totalTime < 3000) // Less than 3 seconds

        println("URL analysis performed $totalOperations times in ${totalTime}ms")
    }
}
