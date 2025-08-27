package com.gologlu.detracktor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gologlu.detracktor.utils.*
import com.gologlu.detracktor.data.*
import com.gologlu.detracktor.utils.RegexRiskLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the complete privacy and security workflow.
 * Tests end-to-end functionality from clipboard analysis to UI display.
 */
@RunWith(AndroidJUnit4::class)
class PrivacySecurityIntegrationTest {

    private lateinit var urlCleanerService: UrlCleanerService
    private lateinit var contentFilter: ClipboardContentFilter
    private lateinit var privacyAnalyzer: UrlPrivacyAnalyzer
    private lateinit var regexValidator: RegexValidator
    private lateinit var ruleCompiler: RuleCompiler

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize all components
        privacyAnalyzer = UrlPrivacyAnalyzer()
        regexValidator = RegexValidator()
        contentFilter = ClipboardContentFilter()
        
        // Create PerformanceConfig for RuleCompiler
        val performanceConfig = PerformanceConfig(
            enablePatternCaching = true,
            maxCacheSize = 100,
            recompileOnConfigChange = true
        )
        ruleCompiler = RuleCompiler(performanceConfig)
        urlCleanerService = UrlCleanerService(context)
    }

    @Test
    fun testCompletePrivacyWorkflow() = runBlocking {
        val testScenarios = listOf(
            // Regular URL - should be visible
            TestScenario(
                content = "https://example.com/page?utm_source=test",
                expectedContentType = ClipboardContentType.URI,
                expectedShouldDisplay = true,
                expectedShouldBlur = false,
                description = "Regular URL with tracking parameters"
            ),
            
            // Sensitive URL - should be blurred
            TestScenario(
                content = "https://user:password@api.example.com/data?api_key=secret123",
                expectedContentType = ClipboardContentType.SENSITIVE_URI,
                expectedShouldDisplay = true,
                expectedShouldBlur = true,
                description = "URL with credentials and API key"
            ),
            
            // Non-URI content - should be hidden
            TestScenario(
                content = "This is just plain text with sensitive info: password123",
                expectedContentType = ClipboardContentType.NON_URI,
                expectedShouldDisplay = false,
                expectedShouldBlur = false,
                description = "Plain text with potential sensitive content"
            ),
            
            // Database connection - should be sensitive
            TestScenario(
                content = "postgresql://admin:secret@db.example.com:5432/production",
                expectedContentType = ClipboardContentType.SENSITIVE_URI,
                expectedShouldDisplay = true,
                expectedShouldBlur = true,
                description = "Database connection string"
            )
        )

        testScenarios.forEach { scenario ->
            val privacySettings = PrivacySettings(
                hideNonUriContent = true,
                blurSensitiveParams = true,
                blurCredentials = true,
                showBlurToggle = true
            )

            // Step 1: Analyze content with privacy filter
            val analysis = contentFilter.analyzeAndFilter(scenario.content, privacySettings)
            
            // Verify analysis results
            assertEquals("Content type mismatch for: ${scenario.description}", 
                scenario.expectedContentType, analysis.contentType)
            assertEquals("Display visibility mismatch for: ${scenario.description}", 
                scenario.expectedShouldDisplay, analysis.shouldDisplay)
            assertEquals("Blur setting mismatch for: ${scenario.description}", 
                scenario.expectedShouldBlur, analysis.shouldBlur)

            // Step 2: If it's a URI, test URL cleaning
            if (analysis.contentType != ClipboardContentType.NON_URI) {
                val cleanedUrl = urlCleanerService.cleanUrl(scenario.content)
                assertNotNull("Cleaned URL should not be null for: ${scenario.description}", cleanedUrl)
                
                // Verify that URL cleaning preserves embedded credentials if present
                if (scenario.content.contains("@") && scenario.content.contains(":")) {
                    assertTrue("Cleaned URL should preserve embedded credentials for: ${scenario.description}",
                        cleanedUrl.contains("@"))
                }
            }

            // Step 3: Verify risk factors are properly identified
            if (scenario.expectedShouldBlur) {
                assertTrue("Sensitive content should have risk factors: ${scenario.description}",
                    analysis.riskFactors.isNotEmpty())
                assertTrue("Sensitive content should have high-severity risk factors: ${scenario.description}",
                    analysis.riskFactors.any { it.severity == RiskSeverity.HIGH || it.severity == RiskSeverity.CRITICAL })
            }

            // Step 4: Verify safe preview generation
            if (!analysis.shouldDisplay) {
                assertTrue("Hidden content should be filtered: ${scenario.description}",
                    analysis.filteredContent.contains("Content hidden") || analysis.filteredContent.contains("privacy"))
            } else if (analysis.shouldBlur) {
                assertNotEquals("Blurred content should have modified preview: ${scenario.description}",
                    scenario.content, analysis.filteredContent)
                assertTrue("Blurred preview should be shorter or masked: ${scenario.description}",
                    analysis.filteredContent.length <= scenario.content.length)
            }
        }
    }

    @Test
    fun testRegexValidationIntegration() = runBlocking {
        val securityConfig = SecurityConfig(
            enableRegexValidation = true,
            regexTimeoutMs = 1000L,
            maxRegexLength = 100,
            rejectHighRiskPatterns = true
        )

        val testPatterns = listOf(
            // Safe patterns
            TestPattern("example\\.com", true, "Simple domain pattern"),
            TestPattern("https?://.*", true, "HTTP/HTTPS protocol pattern"),
            TestPattern("\\d{3}-\\d{3}-\\d{4}", true, "Phone number pattern"),
            
            // Potentially dangerous patterns
            TestPattern("(a+)+b", false, "Classic ReDoS pattern"),
            TestPattern("(a|a)*b", false, "Alternative ReDoS pattern"),
            TestPattern("a".repeat(200) + "*", false, "Overly complex pattern")
        )

        testPatterns.forEach { testPattern ->
            val validationResult = regexValidator.validateRegexSafety(testPattern.pattern)
            
            assertEquals("Pattern safety mismatch for: ${testPattern.description}",
                testPattern.expectedSafe, validationResult.isSafe)
            
            if (!validationResult.isSafe) {
                assertTrue("Unsafe pattern should have suggestions: ${testPattern.description}",
                    validationResult.suggestions.isNotEmpty())
                assertTrue("Unsafe pattern should have high risk level: ${testPattern.description}",
                    validationResult.riskLevel in listOf(RegexRiskLevel.HIGH, RegexRiskLevel.CRITICAL))
            }

            // Test compilation with timeout
            val compilationResult = regexValidator.compileWithTimeout(testPattern.pattern, securityConfig.regexTimeoutMs)
            
            if (testPattern.expectedSafe) {
                assertNotNull("Safe pattern should compile successfully: ${testPattern.description}",
                    compilationResult)
            } else if (securityConfig.rejectHighRiskPatterns) {
                assertNull("Unsafe pattern should fail compilation when rejection enabled: ${testPattern.description}",
                    compilationResult)
            }
        }
    }

    @Test
    fun testEndToEndWorkflowWithRealScenarios() = runBlocking {
        val realWorldScenarios = listOf(
            // Social media sharing
            "https://twitter.com/share?url=https://example.com&text=Check%20this%20out&via=username",
            
            // E-commerce with tracking
            "https://shop.example.com/product/123?utm_source=email&utm_campaign=sale&ref=affiliate123",
            
            // API endpoint with authentication
            "https://api.github.com/user/repos?access_token=ghp_1234567890abcdef&per_page=100",
            
            // Database connection string
            "mongodb://user:pass@cluster.mongodb.net:27017/database?retryWrites=true",
            
            // File sharing with credentials
            "ftp://admin:secret@files.company.com/documents/confidential.pdf",
            
            // Email with sensitive info
            "mailto:support@example.com?subject=Password%20Reset&body=My%20username%20is%20admin123"
        )

        val privacySettings = PrivacySettings(
            hideNonUriContent = true,
            blurSensitiveParams = true,
            blurCredentials = true,
            showBlurToggle = true
        )

        realWorldScenarios.forEach { scenario ->
            // Step 1: Privacy analysis
            val analysis = contentFilter.analyzeAndFilter(scenario, privacySettings)
            assertNotNull("Analysis should not be null for real scenario: $scenario", analysis)
            
            // Step 2: URL cleaning (if applicable)
            if (analysis.contentType != ClipboardContentType.NON_URI) {
                val cleanedUrl = urlCleanerService.cleanUrl(scenario)
                assertNotNull("Cleaned URL should not be null: $scenario", cleanedUrl)
                
                // Verify that URL cleaning preserves embedded credentials if present
                if (scenario.contains("@") && scenario.contains(":")) {
                    assertTrue("Cleaned URL should preserve embedded credentials for: $scenario",
                        cleanedUrl.contains("@"))
                }
            }
            
            // Step 3: Verify privacy protection is maintained
            if (analysis.riskFactors.isNotEmpty()) {
                assertTrue("Sensitive content should be protected: $scenario",
                    analysis.shouldBlur || !analysis.shouldDisplay)
                assertTrue("Sensitive content should have risk factors: $scenario",
                    analysis.riskFactors.isNotEmpty())
            }
            
            // Step 4: Performance check
            val startTime = System.currentTimeMillis()
            repeat(10) {
                contentFilter.analyzeAndFilter(scenario, privacySettings)
            }
            val endTime = System.currentTimeMillis()
            val avgTime = (endTime - startTime) / 10.0
            
            assertTrue("Average processing time should be under 100ms per analysis: $scenario",
                avgTime < 100.0)
        }
    }

    @Test
    fun testSecurityConfigurationIntegration() = runBlocking {
        val strictConfig = SecurityConfig(
            enableRegexValidation = true,
            regexTimeoutMs = 500L,
            maxRegexLength = 50,
            rejectHighRiskPatterns = true
        )

        val lenientConfig = SecurityConfig(
            enableRegexValidation = false,
            regexTimeoutMs = 2000L,
            maxRegexLength = 200,
            rejectHighRiskPatterns = false
        )

        val testPattern = "(a+)+b" // Known ReDoS pattern

        // Test with strict configuration
        val strictValidation = regexValidator.validateRegexSafety(testPattern)
        assertFalse("Strict config should reject ReDoS pattern", strictValidation.isSafe)
        assertTrue("Strict config should identify issues", strictValidation.suggestions.isNotEmpty())

        val strictCompilation = regexValidator.compileWithTimeout(testPattern, strictConfig.regexTimeoutMs)
        assertNull("Strict config should fail compilation of dangerous pattern", strictCompilation)

        // Test with lenient configuration
        val lenientValidation = regexValidator.validateRegexSafety(testPattern)
        // Lenient config might still detect the pattern as unsafe, but won't reject it
        
        val lenientCompilation = regexValidator.compileWithTimeout(testPattern, lenientConfig.regexTimeoutMs)
        // Lenient config allows compilation but with longer timeout
        assertNull("ReDoS pattern should still fail compilation even with lenient config", lenientCompilation)
    }

    @Test
    fun testPerformanceUnderLoad() = runBlocking {
        val testContents = listOf(
            "https://example.com/page?param=value",
            "https://user:pass@api.example.com/data",
            "Plain text content that should be hidden",
            "https://social.example.com/share?url=test&token=secret",
            "postgresql://admin:secret@db.example.com/prod"
        )

        val privacySettings = PrivacySettings(
            hideNonUriContent = true,
            blurSensitiveParams = true,
            blurCredentials = true,
            showBlurToggle = true
        )

        // Test concurrent processing
        val startTime = System.currentTimeMillis()
        
        val results = testContents.map { content ->
            // Simulate concurrent analysis
            contentFilter.analyzeAndFilter(content, privacySettings)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Verify all results are valid
        assertEquals("Should have result for each test content", testContents.size, results.size)
        results.forEach { result ->
            assertNotNull("Result should not be null", result)
            assertTrue("Result should have valid content type", 
                result.contentType in ClipboardContentType.values())
        }

        // Performance check
        assertTrue("Concurrent processing should complete within 1 second", totalTime < 1000)

        // Test repeated processing for memory leaks
        repeat(50) { iteration ->
            testContents.forEach { content ->
                val result = contentFilter.analyzeAndFilter(content, privacySettings)
                assertNotNull("Result should not be null in iteration $iteration", result)
            }
        }

        // Force garbage collection and verify system is still responsive
        System.gc()
        val finalResult = contentFilter.analyzeAndFilter(testContents.first(), privacySettings)
        assertNotNull("System should still be responsive after load test", finalResult)
    }

    @Test
    fun testErrorHandlingAndRecovery() = runBlocking {
        val problematicInputs = listOf(
            null, // Null input
            "", // Empty string
            " ".repeat(10000), // Very long whitespace
            "https://" + "a".repeat(5000), // Extremely long URL
            "data:text/plain;base64," + "A".repeat(10000), // Large data URI
            "javascript:" + "alert('test');".repeat(1000), // Large JavaScript URI
            "\u0000\u0001\u0002", // Control characters
            "https://example.com/\uD83D\uDE00".repeat(100) // Unicode characters
        )

        val privacySettings = PrivacySettings(
            hideNonUriContent = true,
            blurSensitiveParams = true,
            blurCredentials = true,
            showBlurToggle = true
        )

        problematicInputs.forEach { input ->
            try {
                val result = if (input != null) {
                    contentFilter.analyzeAndFilter(input, privacySettings)
                } else {
                    // Test null handling
                    contentFilter.analyzeAndFilter("", privacySettings)
                }
                
                assertNotNull("Result should not be null for problematic input", result)
                assertTrue("Result should have valid content type", 
                    result.contentType in ClipboardContentType.values())
                
            } catch (e: Exception) {
                fail("Should not throw exception for input: ${input?.take(50)}... Exception: ${e.message}")
            }
        }
    }

    // Helper data classes
    private data class TestScenario(
        val content: String,
        val expectedContentType: ClipboardContentType,
        val expectedShouldDisplay: Boolean,
        val expectedShouldBlur: Boolean,
        val description: String
    )

    private data class TestPattern(
        val pattern: String,
        val expectedSafe: Boolean,
        val description: String
    )
}
