package com.gologlu.detracktor.application.service.match

import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.application.types.SensitiveMergeMode
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.QueryToken
import com.gologlu.detracktor.domain.model.UrlParts

/**
 * Describes how a specific query token is affected by matching rules.
 */
data class TokenEffect(
    val tokenIndex: Int,
    val name: String,
    val willBeRemoved: Boolean,
    val matchedRuleIndexes: List<Int>,
    val matchedPatternsByRule: Map<Int, List<String>>
)

/**
 * Result of evaluating the engine for a URL: rule matches, token-level effects, and warnings.
 */
data class Evaluation(
    val matches: List<CompiledSiteRule>,
    val tokenEffects: List<TokenEffect>,
    val effectiveWarnings: WarningSettings
)

/**
 * Rule engine that compiles settings, evaluates URLs, and provides data for transformation and annotation.
 */
interface RuleEngine {
    /** Compile settings and cache structures for fast evaluation. */
    context(hostCanonicalizer: HostCanonicalizer)
    fun load(settings: AppSettings)

    /** Evaluate a URL against cached rules to get matches, token effects, and warnings. */
    context(hostCanonicalizer: HostCanonicalizer)
    fun evaluate(parts: UrlParts): Evaluation

    /** Apply removals to the query pairs using current compiled rules. */
    context(hostCanonicalizer: HostCanonicalizer)
    fun applyRemovals(parts: UrlParts): UrlParts
}

class DefaultRuleEngine : RuleEngine {
    private var compiled: CompiledRuleset = CompiledRuleset(emptyList())

    context(hostCanonicalizer: HostCanonicalizer)
    override fun load(settings: AppSettings) {
        compiled = MatchService.compile(settings)
    }

    context(hostCanonicalizer: HostCanonicalizer)
    override fun evaluate(parts: UrlParts): Evaluation {
        val matches = MatchService.findMatches(parts, compiled)
        val tokenEffects = computeTokenEffects(parts.queryPairs, matches)
        val effectiveWarnings = computeEffectiveWarnings(matches)
        return Evaluation(matches = matches, tokenEffects = tokenEffects, effectiveWarnings = effectiveWarnings)
    }

    context(hostCanonicalizer: HostCanonicalizer)
    override fun applyRemovals(parts: UrlParts): UrlParts {
        val matches = MatchService.findMatches(parts, compiled)
        if (matches.isEmpty()) return parts
        val predicates = matches.flatMap { it.removePredicates }
        val filtered = parts.queryPairs.removeAnyOf(predicates)
        return parts.copyWithQuery(filtered)
    }

    private fun computeTokenEffects(query: QueryPairs, matches: List<CompiledSiteRule>): List<TokenEffect> {
        if (matches.isEmpty()) return emptyList()
        val effects = ArrayList<TokenEffect>(query.size())
        val predicatesByRule: List<Triple<Int, (String) -> Boolean, String>> = matches.flatMap { rule ->
            rule.removePredicates.mapIndexed { idx, pred -> Triple(rule.index, pred, rule.removePatterns[idx]) }
        }
        val tokens: List<QueryToken> = query.getTokens()
        for ((idx, token) in tokens.withIndex()) {
            val name = token.decodedKey
            val matchedDetails = predicatesByRule.filter { (_, pred, _) -> pred(name) }
            val matchedIndexes = matchedDetails.map { (i, _, _) -> i }
            val patternsByRule: Map<Int, List<String>> = matchedDetails
                .groupBy({ (i, _, _) -> i }, valueTransform = { (_, _, pat) -> pat })
            effects.add(
                TokenEffect(
                    tokenIndex = idx,
                    name = name,
                    willBeRemoved = matchedIndexes.isNotEmpty(),
                    matchedRuleIndexes = matchedIndexes,
                    matchedPatternsByRule = patternsByRule
                )
            )
        }
        return effects
    }

    private fun computeEffectiveWarnings(matches: List<CompiledSiteRule>): WarningSettings {
        if (matches.isEmpty()) return WarningSettings()
        // 1) Catch-all rules (Domains.Any) are unioned together
        val catchAllWarns = matches
            .filter { it.site.when_.host.domains is Domains.Any }
            .mapNotNull { it.site.then.warn }
        val catchAll = unionWarnings(catchAllWarns)

        // 2) Specific rules (non-Any) override catch-all with last-wins
        val specifics = matches
            .filter { it.site.when_.host.domains !is Domains.Any }
            .mapNotNull { it.site.then.warn }

        var result = catchAll
        for (ov in specifics) {
            val nextSensitive = when (ov.sensitiveMerge ?: SensitiveMergeMode.REPLACE) {
                SensitiveMergeMode.REPLACE -> ov.sensitiveParams
                SensitiveMergeMode.UNION -> {
                    if (ov.sensitiveParams == null) result.sensitiveParams
                    else ((result.sensitiveParams ?: emptyList()) + ov.sensitiveParams).distinct()
                }
            }
            result = WarningSettings(
                warnOnEmbeddedCredentials = ov.warnOnEmbeddedCredentials ?: result.warnOnEmbeddedCredentials,
                sensitiveParams = nextSensitive,
                sensitiveMerge = ov.sensitiveMerge ?: result.sensitiveMerge,
                version = maxOf(result.version, ov.version)
            )
        }
        return result
    }

    private fun unionWarnings(list: List<WarningSettings>): WarningSettings {
        if (list.isEmpty()) return WarningSettings()
        var warnOnCreds: Boolean? = null
        val sens = LinkedHashSet<String>()
        var version = 1U
        for (w in list) {
            warnOnCreds = when {
                warnOnCreds == null -> w.warnOnEmbeddedCredentials
                w.warnOnEmbeddedCredentials == null -> warnOnCreds
                else -> warnOnCreds || w.warnOnEmbeddedCredentials
            }
            w.sensitiveParams?.let { sens.addAll(it) }
            version = maxOf(version, w.version)
        }
        return WarningSettings(
            warnOnEmbeddedCredentials = warnOnCreds,
            sensitiveParams = if (sens.isEmpty()) null else sens.toList(),
            version = version
        )
    }
}


