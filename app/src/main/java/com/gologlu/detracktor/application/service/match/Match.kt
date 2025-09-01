package com.gologlu.detracktor.application.service.match

import com.gologlu.detracktor.application.error.AppValidationError
import com.gologlu.detracktor.application.error.AppValidationException
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.Subdomains
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.WhenBlock
import com.gologlu.detracktor.application.service.globby.Globby
import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.domain.model.UrlParts
import java.util.Locale

/** Compiled representation of a single site rule for fast matching. */
data class CompiledSiteRule(
    val index: Int,
    val site: UrlRule,
    val hostMatcher: HostMatcher,
    val schemeSet: Set<String>,
    val removePatterns: List<String>,
    val removePredicates: List<(String) -> Boolean>
)

/** Compiled ruleset preserving original order (first match wins). */
data class CompiledRuleset(
    val sites: List<CompiledSiteRule>
)

/** Service to compile rules and evaluate matches. */
object MatchService {
    context(hostCanonicalizer: HostCanonicalizer)
    fun compile(config: AppSettings): CompiledRuleset {
        val compiled = config.sites.mapIndexed { siteIndex, site ->
            val schemeSet = compileSchemes(siteIndex, site.when_)
            val hostMatcher = HostMatcher.compile(siteIndex, site.when_.host)
            val removePatterns = compileRemovePatterns(siteIndex, site.then)
            val removePredicates = removePatterns.map { pat ->
                { name: String -> com.gologlu.detracktor.application.service.globby.Globby.matches(pat, name) }
            }
            CompiledSiteRule(index = siteIndex, site = site, hostMatcher = hostMatcher, schemeSet = schemeSet, removePatterns = removePatterns, removePredicates = removePredicates)
        }
        return CompiledRuleset(compiled)
    }

    context(hostCanonicalizer: HostCanonicalizer)
    fun findMatches(parts: UrlParts, ruleset: CompiledRuleset): List<CompiledSiteRule> {
        val scheme = parts.scheme?.lowercase(Locale.ROOT)
        val host = parts.host
        return ruleset.sites.asSequence()
            .filter { rule -> scheme == null || scheme in rule.schemeSet }
            .filter { rule -> rule.hostMatcher.matches(host) }
            .toList()
    }

    private fun compileSchemes(siteIndex: Int, whenBlock: WhenBlock): Set<String> {
        val schemes = whenBlock.schemes?.map { it.lowercase(Locale.ROOT) } ?: listOf("http", "https")
        if (schemes.isEmpty()) {
            throw AppValidationException(AppValidationError("schemes cannot be empty", "sites[$siteIndex].when.schemes"))
        }
        return schemes.toSet()
    }

    private fun compileRemovePatterns(siteIndex: Int, thenBlock: ThenBlock): List<String> {
        if (thenBlock.remove.isEmpty()) {
            throw AppValidationException(AppValidationError("remove list cannot be empty", "sites[$siteIndex].then.remove"))
        }
        thenBlock.remove.forEachIndexed { idx, p ->
            Globby.requireValid(p.pattern, "sites[$siteIndex].then.remove[$idx]")
        }
        return thenBlock.remove.map { it.pattern }
    }
}

/** Structural, label-aware host matcher derived from WhenBlock.host. */
class HostMatcher private constructor(
    private val domainsSpec: DomainsSpec,
    private val subdomainsSpec: SubdomainsSpec?
) {
    companion object {
        context(hostCanonicalizer: HostCanonicalizer)
        fun compile(siteIndex: Int, host: HostCond): HostMatcher {
            val domainsSpec = when (val d = host.domains) {
                is Domains.Any -> DomainsSpec.Any
                is Domains.ListOf -> DomainsSpec.ListOf(
                    d.values.mapIndexed { domainIndex, s ->
                        val canon = canonicalizeConfiguredDomain(s)
                            ?: throw AppValidationException(
                                AppValidationError(
                                    message = "invalid domain",
                                    fieldPath = "sites[$siteIndex].when.host.domains[$domainIndex]"
                                )
                            )
                        splitLabels(canon)
                    }
                )
            }

            val subSpec: SubdomainsSpec? = when (val s = host.subdomains) {
                null -> null // allowed only when domains == "*" per schema; we do not re-validate here
                is Subdomains.Any -> SubdomainsSpec.Any
                is Subdomains.None -> SubdomainsSpec.None
                is Subdomains.OneOf -> {
                    // Accept "" in the list to mean "no subdomain is also allowed".
                    val includesNone = s.labels.any { it.isEmpty() }
                    val labelSet = s.labels.filter { it.isNotEmpty() }.map { it.lowercase(Locale.ROOT) }.toSet()
                    SubdomainsSpec.OneOf(labelSet, includesNone)
                }
            }
            return HostMatcher(domainsSpec = domainsSpec, subdomainsSpec = subSpec)
        }

        context(hostCanonicalizer: HostCanonicalizer)
        private fun canonicalizeConfiguredDomain(domain: String): String? {
            val ascii = hostCanonicalizer.toAscii(domain) ?: return null
            return ascii.trimEnd('.')
        }

        private fun splitLabels(hostAscii: String): List<String> {
            if (hostAscii.isEmpty()) return emptyList()
            return hostAscii.split('.')
        }
    }

    context(hostCanonicalizer: HostCanonicalizer)
    fun matches(rawHost: String?): Boolean {
        val ascii = hostCanonicalizer.toAscii(rawHost) ?: return false
        val host = ascii.trimEnd('.')
        if (host.isEmpty()) return false
        val hostLabels = splitLabels(host)

        return when (val d = domainsSpec) {
            DomainsSpec.Any -> true
            is DomainsSpec.ListOf -> {
                d.domainLabelsList.any { domainLabels ->
                    if (hostLabels.size < domainLabels.size) return@any false
                    // Match suffix labels exactly
                    val offset = hostLabels.size - domainLabels.size
                    for (i in domainLabels.indices) {
                        if (hostLabels[offset + i] != domainLabels[i]) return@any false
                    }
                    // Subdomain labels are the leftover leading labels
                    val subCount = offset
                    when (val s = subdomainsSpec) {
                        null -> true // schema guarantees this only for Domains.Any; but be permissive
                        SubdomainsSpec.Any -> true
                        SubdomainsSpec.None -> subCount == 0
                        is SubdomainsSpec.OneOf -> {
                            if (subCount == 0) {
                                s.includesNone
                            } else if (subCount == 1) {
                                val label = hostLabels[0]
                                label in s.labels
                            } else {
                                false
                            }
                        }
                    }
                }
            }
        }
    }

    private sealed interface DomainsSpec {
        object Any : DomainsSpec
        data class ListOf(val domainLabelsList: List<List<String>>) : DomainsSpec
    }

    private sealed interface SubdomainsSpec {
        object Any : SubdomainsSpec
        object None : SubdomainsSpec
        data class OneOf(val labels: Set<String>, val includesNone: Boolean) : SubdomainsSpec
    }
    // Note: splitLabels is defined in the companion object above
}