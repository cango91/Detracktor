package com.gologlu.detracktor.application.service.match

import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.application.types.SensitiveMergeMode
import com.gologlu.detracktor.application.types.Subdomains
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.application.types.WhenBlock
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.UrlParts
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.Assert.*

class RuleEngineTest {

    private val mockHostCanonicalizer = mockk<HostCanonicalizer>()
    private val ruleEngine = DefaultRuleEngine()

    private fun setupHostCanonicalizer() {
        every { mockHostCanonicalizer.toAscii(any()) } answers { 
            val input = firstArg<String?>()
            input?.lowercase()?.trimEnd('.')
        }
    }

    private fun createTestRule(
        domains: Domains = Domains.ListOf(listOf("example.com")),
        subdomains: Subdomains? = null,
        schemes: List<String>? = null,
        removePatterns: List<String> = listOf("utm_*"),
        warn: WarningSettings? = null
    ): UrlRule {
        return UrlRule(
            when_ = WhenBlock(
                host = HostCond(domains = domains, subdomains = subdomains),
                schemes = schemes
            ),
            then = ThenBlock(
                remove = removePatterns.map { Pattern(it) },
                warn = warn
            ),
            version = 1U
        )
    }

    private fun createTestUrlParts(
        scheme: String = "https",
        host: String = "example.com",
        path: String = "/test",
        queryString: String = "utm_source=test&param=value"
    ): UrlParts {
        return UrlParts(
            scheme = scheme,
            host = host,
            port = null,
            userInfo = null,
            path = path,
            queryPairs = QueryPairs.from(queryString),
            fragment = null
        )
    }

    @Test
    fun `load should compile settings successfully`() {
        setupHostCanonicalizer()
        val settings = AppSettings(sites = listOf(createTestRule()), version = 1U)

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
        }

        // No exception should be thrown
    }

    @Test
    fun `evaluate should return empty matches for non-matching URL`() {
        setupHostCanonicalizer()
        val rule = createTestRule(domains = Domains.ListOf(listOf("example.com")))
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(host = "different.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertTrue("Should have no matches", evaluation.matches.isEmpty())
            assertTrue("Should have no token effects", evaluation.tokenEffects.isEmpty())
            assertEquals("Should have default warnings", WarningSettings(), evaluation.effectiveWarnings)
        }
    }

    @Test
    fun `evaluate should return matches for matching URL`() {
        setupHostCanonicalizer()
        val rule = createTestRule(domains = Domains.ListOf(listOf("example.com")))
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have one match", 1, evaluation.matches.size)
            assertEquals("Should match the rule", rule, evaluation.matches[0].site)
            assertFalse("Should have token effects", evaluation.tokenEffects.isEmpty())
        }
    }

    @Test
    fun `evaluate should compute token effects correctly`() {
        setupHostCanonicalizer()
        val rule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = listOf("utm_*", "gclid")
        )
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(queryString = "utm_source=test&gclid=123&param=value")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have three token effects", 3, evaluation.tokenEffects.size)
            
            val utmEffect = evaluation.tokenEffects.find { it.name == "utm_source" }
            assertNotNull("Should have utm_source effect", utmEffect)
            assertTrue("utm_source should be removed", utmEffect!!.willBeRemoved)
            assertEquals("Should match rule 0", listOf(0), utmEffect.matchedRuleIndexes)
            
            val gclidEffect = evaluation.tokenEffects.find { it.name == "gclid" }
            assertNotNull("Should have gclid effect", gclidEffect)
            assertTrue("gclid should be removed", gclidEffect!!.willBeRemoved)
            
            val paramEffect = evaluation.tokenEffects.find { it.name == "param" }
            assertNotNull("Should have param effect", paramEffect)
            assertFalse("param should not be removed", paramEffect!!.willBeRemoved)
        }
    }

    @Test
    fun `evaluate should handle multiple matching rules`() {
        setupHostCanonicalizer()
        val rule1 = createTestRule(
            domains = Domains.Any,
            removePatterns = listOf("utm_*")
        )
        val rule2 = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = listOf("gclid")
        )
        val settings = AppSettings(sites = listOf(rule1, rule2), version = 1U)
        val urlParts = createTestUrlParts(host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have two matches", 2, evaluation.matches.size)
            assertEquals("First match should be rule1", rule1, evaluation.matches[0].site)
            assertEquals("Second match should be rule2", rule2, evaluation.matches[1].site)
        }
    }

    @Test
    fun `evaluate should handle scheme filtering`() {
        setupHostCanonicalizer()
        val rule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            schemes = listOf("https")
        )
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        
        val httpsUrl = createTestUrlParts(scheme = "https", host = "example.com")
        val httpUrl = createTestUrlParts(scheme = "http", host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            
            val httpsEvaluation = ruleEngine.evaluate(httpsUrl)
            assertEquals("HTTPS should match", 1, httpsEvaluation.matches.size)
            
            val httpEvaluation = ruleEngine.evaluate(httpUrl)
            assertEquals("HTTP should not match", 0, httpEvaluation.matches.size)
        }
    }

    @Test
    fun `evaluate should handle subdomain matching`() {
        setupHostCanonicalizer()
        val rule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            subdomains = Subdomains.OneOf(listOf("www", "api"))
        )
        val settings = AppSettings(sites = listOf(rule), version = 1U)

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            
            val wwwUrl = createTestUrlParts(host = "www.example.com")
            val apiUrl = createTestUrlParts(host = "api.example.com")
            val noSubUrl = createTestUrlParts(host = "example.com")
            val otherSubUrl = createTestUrlParts(host = "other.example.com")

            val wwwEvaluation = ruleEngine.evaluate(wwwUrl)
            assertEquals("www subdomain should match", 1, wwwEvaluation.matches.size)
            
            val apiEvaluation = ruleEngine.evaluate(apiUrl)
            assertEquals("api subdomain should match", 1, apiEvaluation.matches.size)
            
            val noSubEvaluation = ruleEngine.evaluate(noSubUrl)
            assertEquals("no subdomain should not match", 0, noSubEvaluation.matches.size)
            
            val otherSubEvaluation = ruleEngine.evaluate(otherSubUrl)
            assertEquals("other subdomain should not match", 0, otherSubEvaluation.matches.size)
        }
    }

    @Test
    fun `evaluate should handle Any domains`() {
        setupHostCanonicalizer()
        val rule = createTestRule(domains = Domains.Any)
        val settings = AppSettings(sites = listOf(rule), version = 1U)

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            
            val url1 = createTestUrlParts(host = "example.com")
            val url2 = createTestUrlParts(host = "different.org")
            val url3 = createTestUrlParts(host = "subdomain.test.net")

            val eval1 = ruleEngine.evaluate(url1)
            assertEquals("Any domain should match example.com", 1, eval1.matches.size)
            
            val eval2 = ruleEngine.evaluate(url2)
            assertEquals("Any domain should match different.org", 1, eval2.matches.size)
            
            val eval3 = ruleEngine.evaluate(url3)
            assertEquals("Any domain should match subdomain.test.net", 1, eval3.matches.size)
        }
    }

    @Test
    fun `evaluate should compute effective warnings for catch-all rules`() {
        setupHostCanonicalizer()
        val catchAllRule = createTestRule(
            domains = Domains.Any,
            warn = WarningSettings(
                warnOnEmbeddedCredentials = true,
                sensitiveParams = listOf("token", "key")
            )
        )
        val settings = AppSettings(sites = listOf(catchAllRule), version = 1U)
        val urlParts = createTestUrlParts()

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            val warnings = evaluation.effectiveWarnings
            assertEquals("Should warn on embedded credentials", true, warnings.warnOnEmbeddedCredentials)
            assertEquals("Should have sensitive params", listOf("token", "key"), warnings.sensitiveParams)
        }
    }

    @Test
    fun `evaluate should compute effective warnings with specific rule override`() {
        setupHostCanonicalizer()
        val catchAllRule = createTestRule(
            domains = Domains.Any,
            warn = WarningSettings(
                warnOnEmbeddedCredentials = true,
                sensitiveParams = listOf("token")
            )
        )
        val specificRule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            warn = WarningSettings(
                warnOnEmbeddedCredentials = false,
                sensitiveParams = listOf("key"),
                sensitiveMerge = SensitiveMergeMode.REPLACE
            )
        )
        val settings = AppSettings(sites = listOf(catchAllRule, specificRule), version = 1U)
        val urlParts = createTestUrlParts(host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            val warnings = evaluation.effectiveWarnings
            assertEquals("Specific rule should override credentials warning", false, warnings.warnOnEmbeddedCredentials)
            assertEquals("Specific rule should replace sensitive params", listOf("key"), warnings.sensitiveParams)
        }
    }

    @Test
    fun `evaluate should compute effective warnings with union merge mode`() {
        setupHostCanonicalizer()
        val catchAllRule = createTestRule(
            domains = Domains.Any,
            warn = WarningSettings(
                warnOnEmbeddedCredentials = true,
                sensitiveParams = listOf("token")
            )
        )
        val specificRule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            warn = WarningSettings(
                sensitiveParams = listOf("key"),
                sensitiveMerge = SensitiveMergeMode.UNION
            )
        )
        val settings = AppSettings(sites = listOf(catchAllRule, specificRule), version = 1U)
        val urlParts = createTestUrlParts(host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            val warnings = evaluation.effectiveWarnings
            assertEquals("Should preserve credentials warning", true, warnings.warnOnEmbeddedCredentials)
            assertEquals("Should union sensitive params", listOf("token", "key"), warnings.sensitiveParams)
        }
    }

    @Test
    fun `applyRemovals should remove matching query parameters`() {
        setupHostCanonicalizer()
        val rule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = listOf("utm_*", "gclid")
        )
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(queryString = "utm_source=test&gclid=123&param=value&utm_medium=email")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val result = ruleEngine.applyRemovals(urlParts)

            val remainingParams = result.queryPairs.getTokens().map { it.decodedKey }
            assertFalse("utm_source should be removed", remainingParams.contains("utm_source"))
            assertFalse("utm_medium should be removed", remainingParams.contains("utm_medium"))
            assertFalse("gclid should be removed", remainingParams.contains("gclid"))
            assertTrue("param should remain", remainingParams.contains("param"))
        }
    }

    @Test
    fun `applyRemovals should return original parts when no matches`() {
        setupHostCanonicalizer()
        val rule = createTestRule(domains = Domains.ListOf(listOf("example.com")))
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(host = "different.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val result = ruleEngine.applyRemovals(urlParts)

            assertEquals("Should return original parts", urlParts, result)
        }
    }

    @Test
    fun `applyRemovals should handle multiple matching rules`() {
        setupHostCanonicalizer()
        val rule1 = createTestRule(
            domains = Domains.Any,
            removePatterns = listOf("utm_*")
        )
        val rule2 = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = listOf("gclid", "fbclid")
        )
        val settings = AppSettings(sites = listOf(rule1, rule2), version = 1U)
        val urlParts = createTestUrlParts(queryString = "utm_source=test&gclid=123&fbclid=456&param=value")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val result = ruleEngine.applyRemovals(urlParts)

            val remainingParams = result.queryPairs.getTokens().map { it.decodedKey }
            assertFalse("utm_source should be removed", remainingParams.contains("utm_source"))
            assertFalse("gclid should be removed", remainingParams.contains("gclid"))
            assertFalse("fbclid should be removed", remainingParams.contains("fbclid"))
            assertTrue("param should remain", remainingParams.contains("param"))
        }
    }

    @Test
    fun `engine should handle empty query parameters`() {
        setupHostCanonicalizer()
        val rule = createTestRule(domains = Domains.ListOf(listOf("example.com")))
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(queryString = "")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have one match", 1, evaluation.matches.size)
            assertTrue("Should have no token effects for empty query", evaluation.tokenEffects.isEmpty())
        }
    }

    @Test
    fun `engine should handle warn-only rules`() {
        setupHostCanonicalizer()
        val warnOnlyRule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = emptyList(),
            warn = WarningSettings(warnOnEmbeddedCredentials = true)
        )
        val settings = AppSettings(sites = listOf(warnOnlyRule), version = 1U)
        val urlParts = createTestUrlParts()

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have one match", 1, evaluation.matches.size)
            assertTrue("Should have no removals", evaluation.matches[0].removePredicates.isEmpty())
            assertEquals("Should have warning settings", true, evaluation.effectiveWarnings.warnOnEmbeddedCredentials)
        }
    }

    @Test
    fun `engine should handle complex pattern matching`() {
        setupHostCanonicalizer()
        val rule = createTestRule(
            domains = Domains.ListOf(listOf("example.com")),
            removePatterns = listOf("utm_*", "fb*", "?clid")
        )
        val settings = AppSettings(sites = listOf(rule), version = 1U)
        val urlParts = createTestUrlParts(queryString = "utm_source=test&fbclid=123&gclid=456&param=value")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            val tokenEffects = evaluation.tokenEffects
            val utmEffect = tokenEffects.find { it.name == "utm_source" }
            val fbEffect = tokenEffects.find { it.name == "fbclid" }
            val gclidEffect = tokenEffects.find { it.name == "gclid" }
            val paramEffect = tokenEffects.find { it.name == "param" }

            assertTrue("utm_source should be removed", utmEffect?.willBeRemoved == true)
            assertTrue("fbclid should be removed", fbEffect?.willBeRemoved == true)
            assertTrue("gclid should be removed", gclidEffect?.willBeRemoved == true)
            assertFalse("param should not be removed", paramEffect?.willBeRemoved == true)
        }
    }

    @Test
    fun `engine should preserve rule order in matches`() {
        setupHostCanonicalizer()
        val rule1 = createTestRule(domains = Domains.Any, removePatterns = listOf("utm_*"))
        val rule2 = createTestRule(domains = Domains.ListOf(listOf("example.com")), removePatterns = listOf("gclid"))
        val rule3 = createTestRule(domains = Domains.Any, removePatterns = listOf("fbclid"))
        
        val settings = AppSettings(sites = listOf(rule1, rule2, rule3), version = 1U)
        val urlParts = createTestUrlParts(host = "example.com")

        with(mockHostCanonicalizer) {
            ruleEngine.load(settings)
            val evaluation = ruleEngine.evaluate(urlParts)

            assertEquals("Should have three matches", 3, evaluation.matches.size)
            assertEquals("First match should be rule1", 0, evaluation.matches[0].index)
            assertEquals("Second match should be rule2", 1, evaluation.matches[1].index)
            assertEquals("Third match should be rule3", 2, evaluation.matches[2].index)
        }
    }
}
