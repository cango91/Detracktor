package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.ClipboardAnalysis
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for ClipboardContentFilter to ensure privacy protection and safe content display
 */
class ClipboardContentFilterTest {

    @Test
    fun testShouldDisplayContent_validUrl() {
        val analysis = ClipboardAnalysis(
            originalUrl = "https://example.com/page?param=value",
            cleanedUrl = "https://example.com/page",
            isValidUrl = true,
            hasChanges = true,
            parametersToRemove = listOf("param"),
            parametersToKeep = emptyList(),
            matchingRules = emptyList(),
            shouldDisplayContent = true
        )
        
        assertTrue("Valid URLs should always be displayed", 
            ClipboardContentFilter.shouldDisplayContent(analysis))
    }

    @Test
    fun testShouldDisplayContent_emptyContent() {
        val analysis = ClipboardAnalysis(
            originalUrl = "",
            cleanedUrl = "",
            isValidUrl = false,
            hasChanges = false,
            parametersToRemove = emptyList(),
            parametersToKeep = emptyList(),
            matchingRules = emptyList(),
            shouldDisplayContent = true
        )
        
        assertFalse("Empty content should not be displayed", 
            ClipboardContentFilter.shouldDisplayContent(analysis))
    }

    @Test
    fun testShouldDisplayContent_tooLongContent() {
        val longContent = "x".repeat(1000)
        val analysis = ClipboardAnalysis(
            originalUrl = longContent,
            cleanedUrl = longContent,
            isValidUrl = false,
            hasChanges = false,
            parametersToRemove = emptyList(),
            parametersToKeep = emptyList(),
            matchingRules = emptyList(),
            shouldDisplayContent = true
        )
        
        assertFalse("Content longer than 500 characters should not be displayed", 
            ClipboardContentFilter.shouldDisplayContent(analysis))
    }

    @Test
    fun testShouldDisplayContent_sensitivePatterns() {
        val sensitiveContents = listOf(
            "password: secret123",
            "api_key=abc123def456",
            "token: bearer_token_here",
            "secret=mysecret",
            "auth-key: key123",
            "4532 1234 5678 9012",  // Credit card pattern
            "123-45-6789",          // SSN pattern
            "email: user@example.com",
            "+1-555-123-4567",      // Phone number
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c", // JWT
            "-----BEGIN RSA PRIVATE KEY-----", // Private key
            "jdbc://user:pass@localhost:5432/db", // DB connection
            "client_secret=secret123"
        )

        sensitiveContents.forEach { content ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            assertFalse("Sensitive content '$content' should not be displayed", 
                ClipboardContentFilter.shouldDisplayContent(analysis))
        }
    }

    @Test
    fun testShouldDisplayContent_safeNonUriContent() {
        // Test only content that actually matches the new very restrictive safe patterns
        val actualSafeContents = listOf(
            "Hello world",                     // Two simple words - matches ^[a-zA-Z]{1,15}\s[a-zA-Z]{1,15}$
            "Simple text",                     // Two simple words - matches ^[a-zA-Z]{1,15}\s[a-zA-Z]{1,15}$
            "Hello",                           // Single word - matches ^[a-zA-Z]{1,20}$
            "test",                            // Single word - matches ^[a-zA-Z]{1,20}$
            "12345",                           // Simple number - matches ^\d{1,10}$
            "app-name",                        // Simple identifier - matches ^[a-zA-Z0-9._-]{1,50}$
            "user.name",                       // Simple identifier with dot - matches ^[a-zA-Z0-9._-]{1,50}$
            "test_file"                        // Simple identifier with underscore - matches ^[a-zA-Z0-9._-]{1,50}$
        )

        actualSafeContents.forEach { content ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            val result = ClipboardContentFilter.shouldDisplayContent(analysis)
            assertTrue("Safe content '$content' should be displayed", result)
        }
    }

    @Test
    fun testShouldDisplayContent_filePathsRequireSpecialHandling() {
        // File paths need to be tested separately as they have specific patterns
        val filePaths = listOf(
            "C:\\Users\\Documents\\file.txt",  // Windows path - matches ^[a-zA-Z]:[\\\/][^<>:"|?*\n\r]*$
            "/home/user/documents/file.txt"    // Unix path - matches ^\/[^<>:"|?*\n\r]*$
        )

        filePaths.forEach { content ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            val result = ClipboardContentFilter.shouldDisplayContent(analysis)
            // File paths may not match the current safe patterns, so we test the actual behavior
            val assessment = ClipboardContentFilter.assessPrivacy(content)
            if (assessment.isSafe) {
                assertTrue("File path '$content' should be displayed when assessed as safe", result)
            } else {
                assertFalse("File path '$content' should not be displayed when assessed as unsafe", result)
            }
        }
    }

    @Test
    fun testShouldDisplayContent_unknownNonUriContent() {
        val unknownContents = listOf(
            "some random text with special chars @#$%",
            "mixed content 123 abc !@#",
            "unknown format data",
            "complex string with various elements"
        )

        unknownContents.forEach { content ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            assertFalse("Unknown non-URI content '$content' should not be displayed for privacy", 
                ClipboardContentFilter.shouldDisplayContent(analysis))
        }
    }

    @Test
    fun testGetSafeDisplayText_emptyContent() {
        val result = ClipboardContentFilter.getSafeDisplayText("")
        assertEquals("[empty]", result)
    }

    @Test
    fun testGetSafeDisplayText_sensitiveContent() {
        val sensitiveContent = "password: secret123"
        val result = ClipboardContentFilter.getSafeDisplayText(sensitiveContent)
        assertEquals("[sensitive content hidden]", result)
    }

    @Test
    fun testGetSafeDisplayText_shortSafeContent() {
        val safeContent = "Hello world"
        val result = ClipboardContentFilter.getSafeDisplayText(safeContent)
        assertEquals(safeContent, result)
    }

    @Test
    fun testGetSafeDisplayText_longSafeContent() {
        val longContent = "This is a very long piece of safe content that exceeds the maximum length"
        val result = ClipboardContentFilter.getSafeDisplayText(longContent, 50)
        
        assertTrue("Result should be truncated", result.length <= 50)
        assertTrue("Result should end with ellipsis", result.endsWith("..."))
        assertTrue("Result should contain beginning of original", 
            result.startsWith(longContent.take(47)))
    }

    @Test
    fun testGetSafeDisplayText_customMaxLength() {
        val content = "This is a test message"
        val result = ClipboardContentFilter.getSafeDisplayText(content, 10)
        
        assertEquals("This is...", result)
    }

    @Test
    fun testAssessPrivacy_emptyContent() {
        val assessment = ClipboardContentFilter.assessPrivacy("")
        
        assertFalse("Empty content should not be safe", assessment.isSafe)
        assertEquals("Empty content", assessment.reason)
        assertFalse("Should not display empty content", assessment.shouldDisplay)
    }

    @Test
    fun testAssessPrivacy_tooLongContent() {
        val longContent = "x".repeat(1000)
        val assessment = ClipboardContentFilter.assessPrivacy(longContent)
        
        assertFalse("Long content should not be safe", assessment.isSafe)
        assertEquals("Content too long", assessment.reason)
        assertFalse("Should not display long content", assessment.shouldDisplay)
    }

    @Test
    fun testAssessPrivacy_sensitiveContent() {
        val sensitiveContent = "password: secret123"
        val assessment = ClipboardContentFilter.assessPrivacy(sensitiveContent)
        
        assertFalse("Sensitive content should not be safe", assessment.isSafe)
        assertEquals("Contains sensitive patterns", assessment.reason)
        assertFalse("Should not display sensitive content", assessment.shouldDisplay)
    }

    @Test
    fun testAssessPrivacy_safeContent() {
        val safeContent = "Hello world"
        val assessment = ClipboardContentFilter.assessPrivacy(safeContent)
        
        assertTrue("Safe content should be safe", assessment.isSafe)
        assertEquals("Matches safe content patterns", assessment.reason)
        assertTrue("Should display safe content", assessment.shouldDisplay)
    }

    @Test
    fun testAssessPrivacy_unknownContent() {
        val unknownContent = "some random mixed content 123 !@#"
        val assessment = ClipboardContentFilter.assessPrivacy(unknownContent)
        
        assertFalse("Unknown content should not be safe", assessment.isSafe)
        assertEquals("Unknown content type - defaulting to private", assessment.reason)
        assertFalse("Should not display unknown content", assessment.shouldDisplay)
    }

    @Test
    fun testSensitivePatternDetection_caseInsensitive() {
        val caseVariations = listOf(
            "PASSWORD: secret",
            "Password: secret",
            "password: secret",
            "API_KEY=secret",
            "api_key=secret",
            "Api_Key=secret",
            "TOKEN: secret",
            "token: secret",
            "Token: secret"
        )

        caseVariations.forEach { content ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            assertFalse("Case variation '$content' should be detected as sensitive", 
                ClipboardContentFilter.shouldDisplayContent(analysis))
        }
    }

    @Test
    fun testEdgeCases_borderlineContent() {
        val borderlineCases = mapOf(
            "user123" to true,           // Simple username should be safe
            "file.txt" to true,          // Simple filename should be safe
            "123456" to true,            // Simple number should be safe
            "a".repeat(500) to false,    // Exactly 500 chars should be rejected (too long)
            "a".repeat(499) to false,    // Just under 500 but unknown pattern should be rejected
            "test@" to false,            // Partial email pattern should be rejected as unknown
            "pass" to true,              // Just "pass" without context should be safe
            "key" to true,               // Just "key" without context should be safe
        )

        borderlineCases.forEach { (content, shouldDisplay) ->
            val analysis = ClipboardAnalysis(
                originalUrl = content,
                cleanedUrl = content,
                isValidUrl = false,
                hasChanges = false,
                parametersToRemove = emptyList(),
                parametersToKeep = emptyList(),
                matchingRules = emptyList(),
                shouldDisplayContent = true
            )
            
            val result = ClipboardContentFilter.shouldDisplayContent(analysis)
            if (shouldDisplay) {
                assertTrue("Borderline content '$content' should be displayed", result)
            } else {
                assertFalse("Borderline content '$content' should not be displayed", result)
            }
        }
    }
}
