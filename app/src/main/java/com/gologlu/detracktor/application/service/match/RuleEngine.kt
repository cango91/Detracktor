package com.gologlu.detracktor.application.service.match

import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.WarningSettings
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
    private var baseWarnings: WarningSettings = WarningSettings()

    context(hostCanonicalizer: HostCanonicalizer)
    override fun load(settings: AppSettings) {
        compiled = MatchService.compile(settings)
        baseWarnings = settings.warnings
    }

    context(hostCanonicalizer: HostCanonicalizer)
    override fun evaluate(parts: UrlParts): Evaluation {
        val matches = MatchService.findMatches(parts, compiled)
        val tokenEffects = computeTokenEffects(parts.queryPairs, matches)
        val effectiveWarnings = mergeWarnings(baseWarnings, matches.mapNotNull { it.site.then.warn })
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

    private fun mergeWarnings(base: WarningSettings, overrides: List<WarningSettings>): WarningSettings {
        if (overrides.isEmpty()) return base
        // Field-wise merge with last-wins for overrides provided by later rules
        var warn = base
        for (ov in overrides) {
            warn = WarningSettings(
                warnOnEmbeddedCredentials = ov.warnOnEmbeddedCredentials,
                sensitiveParams = ov.sensitiveParams ?: warn.sensitiveParams,
                version = maxOf(warn.version, ov.version)
            )
        }
        return warn
    }
}


