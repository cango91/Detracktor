package com.gologlu.detracktor.application.service.integration

import com.gologlu.detracktor.application.service.globby.Globby
import com.gologlu.detracktor.runtime.android.service.net.HostCanonicalizer
import com.gologlu.detracktor.runtime.android.service.net.UrlParserImpl
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WhenBlock
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.Subdomains
import com.gologlu.detracktor.domain.error.isSuccess
import com.gologlu.detracktor.domain.error.getOrThrow
import org.junit.Test
import org.junit.Assert.*

class ServiceIntegrationTest {

    private val urlParser = UrlParserImpl()

    @Test
    fun `integration_should_parse_url_and_canonicalize_host_for_rules_matching`() {
        // Test the flow: URL parsing -> Host canonicalization -> Rules matching
        val testUrl = "https://WWW.YOUTUBE.COM/watch?v=dQw4w9WgXcQ&utm_source=google&gclid=abc123"
        
        // Step 1: Parse URL
        val parseResult = urlParser.parse(testUrl)
        assertTrue("URL should parse successfully", parseResult.isSuccess)
        val urlParts = parseResult.getOrThrow()
        
        // Step 2: Canonicalize host (should preserve subdomains, only do IDN normalization)
        val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
        assertNotNull("Host should canonicalize successfully", canonicalHost)
        assertEquals("www.youtube.com", canonicalHost)
        
        // Step 3: Test rules matching logic
        val rules = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("youtube.com","youtu.be")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.OneOf(listOf("www","m",""))
                        ),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("utm_*"), Pattern("gclid")), warn = null),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        val site = rules.sites[0]
        
        // Verify the rule would match
        assertTrue("Domains should match", site.when_.host.domains is Domains.ListOf)
        val domainsList = site.when_.host.domains as Domains.ListOf
        assertTrue("Should contain youtube.com", domainsList.values.contains("youtube.com"))
        
        // Step 4: Test pattern matching against query parameters
        val queryParams = listOf("v", "utm_source", "gclid")
        val removePatterns = site.then.remove
        
        val paramsToRemove = queryParams.filter { param ->
            removePatterns.any { pattern -> Globby.matches(pattern.pattern, param) }
        }
        
        assertEquals("Should identify utm_source and gclid for removal", 
                    listOf("utm_source", "gclid"), paramsToRemove)
    }

    @Test
    fun `integration_should_handle_international_domain_with_globby_patterns`() {
        // Test IDN domain with Unicode parameter patterns
        val testUrl = "https://例え.テスト/search?追跡_source=test&normal_param=value"
        
        // Parse URL
        val parseResult = urlParser.parse(testUrl)
        assertTrue("IDN URL should parse successfully", parseResult.isSuccess)
        val urlParts = parseResult.getOrThrow()
        
        // Canonicalize host (should convert to punycode)
        val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
        assertNotNull("IDN host should canonicalize", canonicalHost)
        assertTrue("Should be punycode", canonicalHost!!.startsWith("xn--"))
        
        // Test Unicode pattern matching
        val unicodePattern = "追跡_*"
        assertTrue("Should match Unicode parameter", Globby.matches(unicodePattern, "追跡_source"))
        assertFalse("Should not match different parameter", Globby.matches(unicodePattern, "normal_param"))
    }

    @Test
    fun `integration_should_handle_complex_subdomain_matching`() {
        // Test complex subdomain scenarios
        val testCases = listOf(
            "https://www.example.com/path" to "www",
            "https://m.example.com/path" to "m", 
            "https://mobile.example.com/path" to "mobile",
            "https://example.com/path" to "",
            "https://api.v2.example.com/path" to "api.v2"
        )
        
        for ((url, expectedSubdomain) in testCases) {
            val parseResult = urlParser.parse(url)
            assertTrue("URL should parse: $url", parseResult.isSuccess)
            val urlParts = parseResult.getOrThrow()
            
            val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
            assertNotNull("Host should canonicalize: $url", canonicalHost)
            
            // Extract subdomain logic (simplified)
            val hostParts = canonicalHost!!.split(".")
            val actualSubdomain = if (hostParts.size > 2) {
                hostParts.dropLast(2).joinToString(".")
            } else {
                ""
            }
            
            assertEquals("Subdomain should match for $url", expectedSubdomain, actualSubdomain)
        }
    }

    @Test
    fun `integration_should_validate_and_match_globby_patterns_from_rules`() {
        // Test that patterns from rules are valid and work with Globby
        val rules = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(domains = com.gologlu.detracktor.application.types.Domains.Any, subdomains = null),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("utm_*"), Pattern("gclid"), Pattern("fbclid"), Pattern("?"), Pattern("*_id"), Pattern("tracking_*")), warn = null),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        val patterns = rules.sites[0].then.remove.map { it.pattern }
        
        // Validate all patterns
        for (pattern in patterns) {
            try {
                Globby.requireValid(pattern, "test.pattern")
            } catch (e: Exception) {
                fail("Pattern should be valid: $pattern - ${e.message}")
            }
        }
        
        // Test pattern matching against real parameter names
        val testParams = listOf(
            "utm_source", "utm_campaign", "utm_medium", // Should match utm_*
            "gclid", "fbclid", // Should match exact
            "a", "x", "1", // Should match ?
            "user_id", "session_id", "tracking_id", // Should match *_id
            "tracking_pixel", "tracking_code" // Should match tracking_*
        )
        
        val matchedParams = testParams.filter { param ->
            patterns.any { pattern -> Globby.matches(pattern, param) }
        }
        
        assertEquals("All test params should match some pattern", testParams.size, matchedParams.size)
    }

    @Test
    fun `integration_should_handle_edge_case_url_with_all_services`() {
        // Complex URL with multiple edge cases
        val complexUrl = "https://測試.例え:8443/path%20with%20spaces?utm_source=test&param%20name=value&gclid=abc123&normal=ok#section"
        
        // Parse URL
        val parseResult = urlParser.parse(complexUrl)
        assertTrue("Complex URL should parse", parseResult.isSuccess)
        val urlParts = parseResult.getOrThrow()
        
        // Canonicalize host
        val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
        assertNotNull("Complex host should canonicalize", canonicalHost)
        
        // Test port handling
        assertEquals("Port should be preserved", 8443, urlParts.port)
        
        // Test encoded path
        assertNotNull("Encoded path should be parsed", urlParts.path)
        
        // Test query parameters with encoding
        assertNotNull("Query should be parsed", urlParts.rawQuery)
        assertTrue("Should contain encoded parameters", urlParts.rawQuery.contains("param%20name"))
        
        // Test fragment
        assertEquals("Fragment should be parsed", "section", urlParts.fragment)
        
        // Test pattern matching on decoded parameter names
        val patterns = listOf("utm_*", "gclid", "normal")
        val testParamNames = listOf("utm_source", "param name", "gclid", "normal") // Note: decoded names
        
        val matchingParams = testParamNames.filter { param ->
            patterns.any { pattern -> Globby.matches(pattern, param) }
        }
        
        assertEquals("Should match utm_source, gclid, and normal", 
                    listOf("utm_source", "gclid", "normal"), matchingParams)
    }

    @Test
    fun `integration_should_handle_rules_with_scheme_restrictions`() {
        // Test scheme-specific rules
        val rules = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("example.com")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                        ),
                        schemes = listOf("https")
                    ),
                    then = ThenBlock(remove = listOf(Pattern("secure_*")), warn = null),
                    metadata = null
                ),
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("example.com")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                        ),
                        schemes = listOf("http")
                    ),
                    then = ThenBlock(remove = listOf(Pattern("insecure_*")), warn = null),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        
        val httpsUrl = "https://www.example.com/path?secure_token=abc&insecure_param=def"
        val httpUrl = "http://www.example.com/path?secure_token=abc&insecure_param=def"
        
        // Parse both URLs
        val httpsResult = urlParser.parse(httpsUrl)
        val httpResult = urlParser.parse(httpUrl)
        
        assertTrue("HTTPS URL should parse", httpsResult.isSuccess)
        assertTrue("HTTP URL should parse", httpResult.isSuccess)
        
        val httpsScheme = httpsResult.getOrThrow().scheme
        val httpScheme = httpResult.getOrThrow().scheme
        
        // Test scheme matching
        val httpsRule = rules.sites.find { it.when_.schemes?.contains(httpsScheme) == true }
        val httpRule = rules.sites.find { it.when_.schemes?.contains(httpScheme) == true }
        
        assertNotNull("Should find HTTPS rule", httpsRule)
        assertNotNull("Should find HTTP rule", httpRule)
        
        // Test pattern matching for each scheme
        assertTrue("HTTPS rule should match secure_token", 
                  Globby.matches(httpsRule!!.then.remove[0].pattern, "secure_token"))
        assertFalse("HTTPS rule should not match insecure_param", 
                   Globby.matches(httpsRule.then.remove[0].pattern, "insecure_param"))
        
        assertTrue("HTTP rule should match insecure_param", 
                  Globby.matches(httpRule!!.then.remove[0].pattern, "insecure_param"))
        assertFalse("HTTP rule should not match secure_token", 
                   Globby.matches(httpRule.then.remove[0].pattern, "secure_token"))
    }

    @Test
    fun `integration_should_handle_error_propagation_across_services`() {
        // Test error handling when services fail
        
        // Test invalid Globby pattern
        try {
            Globby.requireValid("invalid\\", "test.field")
            fail("Should have thrown exception for invalid pattern")
        } catch (e: Exception) {
            assertTrue("Should be validation exception", e.message!!.contains("Trailing backslash"))
        }
        
        // Test invalid host canonicalization
        val invalidHost = HostCanonicalizer.toAscii("invalid..domain")
        assertNull("Invalid domain should return null", invalidHost)
        
        // JSON parsing test removed (JSON parsing is not in app-layer)
        
        // Test URL parsing with extremely malformed input
        val malformedResult = urlParser.parse("not-a-url-at-all")
        // UrlParserImpl should handle this gracefully (android.net.Uri is lenient)
        assertNotNull("Should handle malformed URL gracefully", malformedResult)
    }

    @Test
    fun `integration_should_handle_performance_with_complex_patterns`() {
        // Test performance with complex backtracking patterns
        val complexPatterns = listOf(
            "*a*b*c*d*e*f*g*h*i*j*",
            "utm_*_*_*_source",
            "*tracking*analytics*pixel*"
        )
        
        val testStrings = listOf(
            "abcdefghij",
            "utm_campaign_source_google_source", 
            "complex_tracking_analytics_pixel_data",
            "no_match_here"
        )
        
        // Validate patterns first
        for (pattern in complexPatterns) {
            Globby.requireValid(pattern, "performance.test")
        }
        
        // Test matching performance (should complete without timeout)
        val startTime = System.currentTimeMillis()
        
        for (pattern in complexPatterns) {
            for (testString in testStrings) {
                val matches = Globby.matches(pattern, testString)
                // Just ensure it completes - actual matching correctness tested elsewhere
                assertNotNull("Matching should complete", matches)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue("Pattern matching should complete quickly (took ${duration}ms)", duration < 1000)
    }

    @Test
    fun `integration_should_handle_real_world_youtube_example`() {
        // Test with real YouTube URL and rules
        val youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=google&utm_medium=search&gclid=abc123&fbclid=def456&t=42s"
        
        // Parse URL
        val parseResult = urlParser.parse(youtubeUrl)
        assertTrue("YouTube URL should parse", parseResult.isSuccess)
        val urlParts = parseResult.getOrThrow()
        
        // Canonicalize host (should preserve subdomains, only do IDN normalization)
        val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
        assertEquals("www.youtube.com", canonicalHost)
        
        // Build rules in code
        val rules = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("youtube.com","youtu.be")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.OneOf(listOf("www","m",""))
                        ),
                        schemes = listOf("http","https")
                    ),
                    then = ThenBlock(remove = listOf(Pattern("utm_*"), Pattern("gclid"), Pattern("fbclid")), warn = null),
                    metadata = mapOf("description" to "Standard trackers for YouTube")
                )
            ),
            version = AppSettings.VERSION
        )
        val site = rules.sites[0]
        
        // Verify rule matching
        assertTrue("Should match youtube.com domain", 
                  (site.when_.host.domains as Domains.ListOf).values.contains("youtube.com"))
        assertTrue("Should match www subdomain", 
                  (site.when_.host.subdomains as Subdomains.OneOf).labels.contains("www"))
        
        // Test parameter removal patterns
        val allParams = listOf("v", "utm_source", "utm_medium", "gclid", "fbclid", "t")
        val removePatterns = site.then.remove
        
        val paramsToKeep = allParams.filter { param ->
            removePatterns.none { pattern -> Globby.matches(pattern.pattern, param) }
        }
        
        val paramsToRemove = allParams.filter { param ->
            removePatterns.any { pattern -> Globby.matches(pattern.pattern, param) }
        }
        
        assertEquals("Should keep v and t parameters", listOf("v", "t"), paramsToKeep)
        assertEquals("Should remove tracking parameters", 
                    listOf("utm_source", "utm_medium", "gclid", "fbclid"), paramsToRemove)
    }
}
