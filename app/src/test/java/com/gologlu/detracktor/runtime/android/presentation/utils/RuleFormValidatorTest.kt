package com.gologlu.detracktor.runtime.android.presentation.utils

import com.gologlu.detracktor.runtime.android.presentation.types.RuleEditFormData
import com.gologlu.detracktor.runtime.android.presentation.types.SubdomainMode
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for RuleFormValidator
 * Tests form validation logic, error messages, and edge cases
 */
class RuleFormValidatorTest {

    private lateinit var validator: RuleFormValidator

    @Before
    fun setUp() {
        validator = RuleFormValidator()
    }

    // Complete form validation tests

    @Test
    fun `validateComplete - valid form with removal patterns`() {
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "utm_*,gclid",
            warnOnCredentials = false,
            sensitiveParamsInput = ""
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validateComplete - valid form with warnings only`() {
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "",
            warnOnCredentials = true,
            sensitiveParamsInput = "password,token"
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Warn-only rule"))
    }

    @Test
    fun `validateComplete - invalid form with no removal or warnings`() {
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "",
            warnOnCredentials = false,
            sensitiveParamsInput = ""
        )

        val result = validator.validateComplete(formData)

        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("at least one of removal patterns or warnings"))
    }

    @Test
    fun `validateComplete - multiple validation errors`() {
        val formData = RuleEditFormData(
            domainsInput = "",
            subdomainMode = SubdomainMode.SPECIFIC_LIST,
            subdomainsInput = "",
            removePatternsInput = "trailing\\",
            warnOnCredentials = false,
            sensitiveParamsInput = ""
        )

        val result = validator.validateComplete(formData)

        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 3) // Domain, subdomain, and pattern errors
        assertTrue(result.errors.any { it.contains("domain is required") })
        assertTrue(result.errors.any { it.contains("Specific subdomains are required") })
        assertTrue(result.errors.any { it.contains("Invalid removal pattern") })
    }

    // Domain validation tests

    @Test
    fun `validateDomains - empty input`() {
        val errors = validator.validateDomains("")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("At least one domain is required"))
    }

    @Test
    fun `validateDomains - blank input`() {
        val errors = validator.validateDomains("   ")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("At least one domain is required"))
    }

    @Test
    fun `validateDomains - valid single domain`() {
        val errors = validator.validateDomains("example.com")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateDomains - valid multiple domains`() {
        val errors = validator.validateDomains("example.com, google.com, facebook.com")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateDomains - wildcard domain`() {
        val errors = validator.validateDomains("*")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateDomains - valid subdomain`() {
        val errors = validator.validateDomains("sub.example.com")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateDomains - invalid domain format`() {
        val errors = validator.validateDomains("invalid..domain")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid domain: invalid..domain"))
    }

    @Test
    fun `validateDomains - mixed valid and invalid domains`() {
        val errors = validator.validateDomains("example.com, invalid..domain, google.com")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid domain: invalid..domain"))
    }

    @Test
    fun `validateDomains - domains with spaces`() {
        val errors = validator.validateDomains(" example.com , google.com ")

        assertTrue(errors.isEmpty())
    }

    // Subdomain validation tests

    @Test
    fun `validateSubdomainMode - NONE mode`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.NONE, "")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateSubdomainMode - ANY mode`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.ANY, "")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with valid subdomains`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www, api, cdn")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with empty input`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Specific subdomains are required"))
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with blank input`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "   ")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Specific subdomains are required"))
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with dots in subdomain`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www, api.v1, cdn")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Subdomain should not contain dots: api.v1"))
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with invalid subdomain name`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www, -invalid, cdn")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid subdomain name: -invalid"))
    }

    @Test
    fun `validateSubdomainMode - SPECIFIC_LIST with multiple errors`() {
        val errors = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www, api.v1, -invalid")

        assertEquals(2, errors.size)
        assertTrue(errors.any { it.contains("contain dots: api.v1") })
        assertTrue(errors.any { it.contains("Invalid subdomain name: -invalid") })
    }

    // Remove patterns validation tests

    @Test
    fun `validateRemovePatterns - empty input`() {
        val errors = validator.validateRemovePatterns("")

        assertTrue(errors.isEmpty()) // Empty is allowed
    }

    @Test
    fun `validateRemovePatterns - valid patterns`() {
        val errors = validator.validateRemovePatterns("utm_*, gclid, fbclid")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateRemovePatterns - single valid pattern`() {
        val errors = validator.validateRemovePatterns("utm_source")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateRemovePatterns - wildcard patterns`() {
        val errors = validator.validateRemovePatterns("utm_*, mc_*, _ga*")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateRemovePatterns - invalid glob pattern`() {
        val errors = validator.validateRemovePatterns("trailing\\")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid removal pattern at position 1"))
    }

    @Test
    fun `validateRemovePatterns - mixed valid and invalid patterns`() {
        val errors = validator.validateRemovePatterns("utm_*, trailing\\, gclid")

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid removal pattern at position 2"))
    }

    @Test
    fun `validateRemovePatterns - patterns with spaces`() {
        val errors = validator.validateRemovePatterns(" utm_source , gclid , fbclid ")

        assertTrue(errors.isEmpty())
    }

    // Scheme validation tests

    @Test
    fun `validateSchemes - valid schemes`() {
        val errors = validator.validateSchemes(listOf("http", "https"))

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateSchemes - empty list`() {
        val errors = validator.validateSchemes(emptyList())

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("At least one scheme must be specified"))
    }

    @Test
    fun `validateSchemes - unsupported scheme`() {
        val errors = validator.validateSchemes(listOf("http", "custom"))

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Unsupported scheme: custom"))
    }

    @Test
    fun `validateSchemes - blank scheme`() {
        val errors = validator.validateSchemes(listOf("http", ""))

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Empty scheme not allowed"))
    }

    @Test
    fun `validateSchemes - all valid schemes`() {
        val errors = validator.validateSchemes(listOf("http", "https", "ftp", "ftps"))

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateSchemes - multiple invalid schemes`() {
        val errors = validator.validateSchemes(listOf("http", "custom1", "custom2"))

        assertEquals(2, errors.size)
        assertTrue(errors.any { it.contains("Unsupported scheme: custom1") })
        assertTrue(errors.any { it.contains("Unsupported scheme: custom2") })
    }

    // Edge cases and integration tests

    @Test
    fun `validateComplete - complex valid form`() {
        val formData = RuleEditFormData(
            domainsInput = "example.com, google.com",
            subdomainMode = SubdomainMode.SPECIFIC_LIST,
            subdomainsInput = "www, api, cdn",
            removePatternsInput = "utm_*, gclid, fbclid",
            warnOnCredentials = true,
            sensitiveParamsInput = "password, token, secret"
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validateComplete - form with duplicate sensitive params warning`() {
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "utm_*",
            warnOnCredentials = false,
            sensitiveParamsInput = "password, token, password, secret"
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Duplicate sensitive parameters: password"))
    }

    @Test
    fun `validateComplete - comprehensive error scenario`() {
        val formData = RuleEditFormData(
            domainsInput = "invalid..domain, example.com",
            subdomainMode = SubdomainMode.SPECIFIC_LIST,
            subdomainsInput = "www, api.v1, -invalid",
            removePatternsInput = "trailing\\, utm_*",
            warnOnCredentials = false,
            sensitiveParamsInput = ""
        )

        val result = validator.validateComplete(formData)

        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 4)
        assertTrue(result.errors.any { it.contains("Invalid domain") })
        assertTrue(result.errors.any { it.contains("contain dots") })
        assertTrue(result.errors.any { it.contains("Invalid subdomain name") })
        assertTrue(result.errors.any { it.contains("Invalid removal pattern") })
    }

    // Companion object tests

    @Test
    fun `COMMON_TRACKING_PATTERNS - contains expected patterns`() {
        val patterns = RuleFormValidator.COMMON_TRACKING_PATTERNS

        assertTrue(patterns.contains("utm_*"))
        assertTrue(patterns.contains("gclid"))
        assertTrue(patterns.contains("fbclid"))
        assertTrue(patterns.contains("_ga"))
        assertTrue(patterns.size > 5)
    }

    @Test
    fun `COMMON_SENSITIVE_PARAMS - contains expected parameters`() {
        val params = RuleFormValidator.COMMON_SENSITIVE_PARAMS

        assertTrue(params.contains("password"))
        assertTrue(params.contains("token"))
        assertTrue(params.contains("secret"))
        assertTrue(params.contains("api_key"))
        assertTrue(params.size > 5)
    }

    // Helper method tests (testing private methods through public interface)

    @Test
    fun `domain validation - comprehensive domain format tests`() {
        // Valid domains
        assertTrue(validator.validateDomains("example.com").isEmpty())
        assertTrue(validator.validateDomains("sub.example.com").isEmpty())
        assertTrue(validator.validateDomains("a.b.c.example.com").isEmpty())
        assertTrue(validator.validateDomains("example-site.com").isEmpty())
        assertTrue(validator.validateDomains("123.com").isEmpty())
        assertTrue(validator.validateDomains("*").isEmpty())

        // Invalid domains
        assertFalse(validator.validateDomains("example..com").isEmpty())
        assertFalse(validator.validateDomains(".example.com").isEmpty())
        assertFalse(validator.validateDomains("example.com.").isEmpty())
        assertFalse(validator.validateDomains("-example.com").isEmpty())
        assertFalse(validator.validateDomains("example-.com").isEmpty())
    }

    @Test
    fun `subdomain validation - comprehensive subdomain format tests`() {
        // Valid subdomains
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www").isEmpty())
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "api").isEmpty())
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "cdn-1").isEmpty())
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "a1b2c3").isEmpty())

        // Invalid subdomains
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www.api").isEmpty())
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "-www").isEmpty())
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www-").isEmpty())
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "").isEmpty())
    }

    // Additional tests to cover missing lines and edge cases

    @Test
    fun `validateSubdomainMode - with Android context for localized messages`() {
        // This test covers the Android context branch that might be missing coverage
        // We can't easily mock Android context in unit tests, but we can test the null path
        val errors = validator.validateSubdomainMode(
            SubdomainMode.SPECIFIC_LIST, 
            "www.invalid, -bad", 
            null // This should trigger the null context branch
        )

        assertEquals(2, errors.size)
        assertTrue(errors.any { it.contains("Subdomain should not contain dots: www.invalid") })
        assertTrue(errors.any { it.contains("Invalid subdomain name: -bad") })
    }

    @Test
    fun `validateRemovePatterns - with blank patterns in list`() {
        // Test the case where some patterns in the list are blank after trimming
        val errors = validator.validateRemovePatterns("utm_*, , gclid, ")

        // Should not produce errors for blank patterns, only invalid ones
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validateRemovePatterns - exception handling in Globby validation`() {
        // Test patterns that might cause Globby to throw exceptions
        // Using a pattern with trailing backslash which causes Globby.requireValid to throw
        val errors = validator.validateRemovePatterns("valid_pattern, trailing\\")

        // Should catch the exception and convert to error message
        assertTrue(errors.size >= 1)
        assertTrue(errors.any { it.contains("Invalid removal pattern at position 2") })
    }

    @Test
    fun `validateComplete - third branch of hasRemove and hasWarn logic`() {
        // Test the "else if (hasRemove && !hasWarn)" branch which might be missing coverage
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "utm_*", // Has remove patterns
            warnOnCredentials = false,     // No warn on credentials
            sensitiveParamsInput = ""      // No sensitive params
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty()) // This branch should produce no warnings
    }

    @Test
    fun `validateSensitiveParams - empty params after parsing`() {
        // Test edge case where input is not blank but results in empty params after parsing
        val formData = RuleEditFormData(
            domainsInput = "example.com",
            subdomainMode = SubdomainMode.NONE,
            subdomainsInput = "",
            removePatternsInput = "utm_*",
            warnOnCredentials = false,
            sensitiveParamsInput = "   ,  ,  " // Only whitespace and commas
        )

        val result = validator.validateComplete(formData)

        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty()) // Should not produce duplicate warnings
    }

    @Test
    fun `parseCommaSeparatedList - edge cases`() {
        // Test the comma-separated parsing behavior through public methods
        // The implementation filters out blank entries, so empty strings won't be included
        
        // Test with only commas and whitespace - results in empty list after parsing, no error
        val errors1 = validator.validateDomains("  ,  ,  ")
        assertTrue(errors1.isEmpty()) // After parsing, empty entries are filtered out, no validation error

        // Test with mixed valid and empty entries - empty entries should be filtered out
        val errors2 = validator.validateDomains("example.com, , google.com, ")
        assertTrue(errors2.isEmpty()) // Should filter out empty entries and validate remaining
        
        // Test with trailing/leading commas - empty entries filtered out
        val errors3 = validator.validateDomains(",example.com,google.com,")
        assertTrue(errors3.isEmpty())
        
        // Test subdomain parsing with empty entries
        val errors4 = validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www, , api, ")
        assertTrue(errors4.isEmpty()) // Should filter out empty entries
        
        // Test removal patterns parsing with empty entries
        val errors5 = validator.validateRemovePatterns("utm_*, , gclid, ")
        assertTrue(errors5.isEmpty()) // Should filter out empty entries
    }

    @Test
    fun `isValidDomainPattern - edge cases for regex matching`() {
        // Test edge cases based on the actual regex implementation
        // Regex: ^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$
        
        // Single character domain (should be valid)
        assertTrue(validator.validateDomains("a").isEmpty())
        
        // Domain with hyphens (should be valid)
        assertTrue(validator.validateDomains("example-site.com").isEmpty())
        
        // Domain starting with hyphen (should be invalid)
        assertFalse(validator.validateDomains("-example.com").isEmpty())
        
        // Domain ending with hyphen (should be invalid)  
        assertFalse(validator.validateDomains("example-.com").isEmpty())
        
        // Domain with double dots (should be invalid)
        assertFalse(validator.validateDomains("example..com").isEmpty())
        
        // Domain starting with dot (should be invalid)
        assertFalse(validator.validateDomains(".example.com").isEmpty())
        
        // Domain ending with dot (should be invalid)
        assertFalse(validator.validateDomains("example.com.").isEmpty())
        
        // Wildcard (special case, should be valid)
        assertTrue(validator.validateDomains("*").isEmpty())
    }

    @Test
    fun `isValidSubdomainName - edge cases for regex matching`() {
        // Test edge cases based on the actual subdomain regex implementation
        // Regex: ^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$
        
        // Single character subdomain (should be valid)
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "a").isEmpty())
        
        // Subdomain with hyphens (should be valid)
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "api-v1").isEmpty())
        
        // Subdomain with numbers (should be valid)
        assertTrue(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "cdn1").isEmpty())
        
        // Subdomain starting with hyphen (should be invalid)
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "-invalid").isEmpty())
        
        // Subdomain ending with hyphen (should be invalid)
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "invalid-").isEmpty())
        
        // Subdomain with dots (should be invalid - checked separately)
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "www.api").isEmpty())
        
        // Empty subdomain (should be invalid)
        assertFalse(validator.validateSubdomainMode(SubdomainMode.SPECIFIC_LIST, "").isEmpty())
    }
}
