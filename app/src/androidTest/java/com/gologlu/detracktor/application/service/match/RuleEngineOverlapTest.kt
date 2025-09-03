package com.gologlu.detracktor.application.service.match

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.application.types.*
import com.gologlu.detracktor.domain.model.MaybeUrl
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.runtime.android.service.net.UrlParserImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuleEngineOverlapTest {

    private val hostCanonicalizer: HostCanonicalizer = HostCanonicalizer { raw ->
        // simple lowercase + dot trim for tests; runtime uses full impl
        raw?.lowercase()?.trimEnd('.')
    }

    private fun parse(url: String): UrlParts {
        val res = UrlParserImpl().parse(url as MaybeUrl)
        return when (res) {
            is com.gologlu.detracktor.domain.error.DomainResult.Success -> res.value
            is com.gologlu.detracktor.domain.error.DomainResult.Failure -> error("Invalid test URL: $url")
        }
    }

    private fun settings(vararg sites: UrlRule): AppSettings = AppSettings(sites = sites.toList())

    private fun siteCatchAll(remove: List<String> = emptyList(), warn: WarningSettings? = null): UrlRule =
        UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.Any)),
            then = ThenBlock(remove = remove.map { Pattern(it) }, warn = warn)
        )

    private fun siteFor(domain: String, remove: List<String> = emptyList(), warn: WarningSettings? = null): UrlRule =
        UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf(domain)))),
            then = ThenBlock(remove = remove.map { Pattern(it) }, warn = warn)
        )

    @Test
    fun removal_is_union_across_overlapping_rules() {
        val s = settings(
            siteFor("example.com", remove = listOf("a_*")),
            siteFor("example.com", remove = listOf("b_*"))
        )
        val engine = DefaultRuleEngine()
        with(hostCanonicalizer) { engine.load(s) }

        val parts = parse("https://example.com/path?a_x=1&a_y=2&b_x=3&c=4")
        val eval = with(hostCanonicalizer) { engine.evaluate(parts) }
        // a_* and b_* should be marked for removal
        val removeNames = eval.tokenEffects.filter { it.willBeRemoved }.map { it.name }.toSet()
        assertTrue(removeNames.containsAll(listOf("a_x", "a_y", "b_x")))

        val cleaned = with(hostCanonicalizer) { engine.applyRemovals(parts) }
        val cleanedQuery = cleaned.rawQuery
        assertEquals("c=4", cleanedQuery)
    }

    @Test
    fun warnings_catchall_union_then_specific_overrides_fields_last_wins() {
        val catchAllWarn = WarningSettings(
            warnOnEmbeddedCredentials = true,
            sensitiveParams = listOf("token", "pwd")
        )
        val specificWarn = WarningSettings(
            // only override sensitive params to empty; leave creds warning as inherited
            sensitiveParams = emptyList()
        )

        val s = settings(
            siteCatchAll(warn = catchAllWarn),
            siteFor("example.com", warn = specificWarn)
        )
        val engine = DefaultRuleEngine()
        with(hostCanonicalizer) { engine.load(s) }

        // For example.com: creds warning true (inherited), sens = [] (override)
        val partsSpecific = parse("https://user:pass@example.com/?token=1&pwd=2&x=3")
        val evalSpecific = with(hostCanonicalizer) { engine.evaluate(partsSpecific) }
        assertEquals(true, evalSpecific.effectiveWarnings.warnOnEmbeddedCredentials)
        assertEquals(emptyList<String>(), evalSpecific.effectiveWarnings.sensitiveParams)

        // For other host: creds true, sens from catch-all
        val partsOther = parse("https://other.com/?token=1&pwd=2&x=3")
        val evalOther = with(hostCanonicalizer) { engine.evaluate(partsOther) }
        assertEquals(true, evalOther.effectiveWarnings.warnOnEmbeddedCredentials)
        assertEquals(listOf("token", "pwd"), evalOther.effectiveWarnings.sensitiveParams)
    }

    @Test
    fun warnings_specific_only_applies_without_catchall() {
        val specificWarn = WarningSettings(
            warnOnEmbeddedCredentials = false,
            sensitiveParams = listOf("secret")
        )
        val s = settings(
            siteFor("example.com", warn = specificWarn)
        )
        val engine = DefaultRuleEngine()
        with(hostCanonicalizer) { engine.load(s) }

        val eval = with(hostCanonicalizer) { engine.evaluate(parse("https://example.com/?secret=1")) }
        assertEquals(false, eval.effectiveWarnings.warnOnEmbeddedCredentials)
        assertEquals(listOf("secret"), eval.effectiveWarnings.sensitiveParams)
    }
}


