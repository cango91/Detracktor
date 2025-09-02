package com.gologlu.detracktor.runtime.android.presentation.types

import com.gologlu.detracktor.application.types.*

/**
 * Form data structure for rule editing UI
 * Provides a more user-friendly representation of UrlRule for form editing
 */
data class RuleEditFormData(
    val hostPattern: String = "",
    val subdomainMode: SubdomainMode = SubdomainMode.EXACT,
    val removePatterns: List<String> = listOf(""),
    val schemes: List<String> = listOf("http", "https"),
    val warnOnCredentials: Boolean = false,
    val sensitiveParams: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Simplified subdomain mode for UI selection
 */
enum class SubdomainMode {
    /** Exact host match only */
    EXACT,
    /** Include all subdomains (*.example.com) */
    WILDCARD,
    /** Global wildcard (*) - matches any domain */
    ANY
}

/**
 * Rule validation result with detailed error and warning messages
 */
data class RuleValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Preview data for ConfigActivity live preview
 */
data class PreviewData(
    val originalUrl: String,
    val cleanedUrl: String,
    val matchedRules: List<RuleMatchSummary>,
    val warningData: WarningDisplayData,
    val hasChanges: Boolean
)

/**
 * Extension functions to convert between form data and domain types
 */
fun RuleEditFormData.toUrlRule(): UrlRule {
    val domains = when (subdomainMode) {
        SubdomainMode.ANY -> Domains.Any
        SubdomainMode.EXACT -> Domains.ListOf(listOf(hostPattern))
        SubdomainMode.WILDCARD -> Domains.ListOf(listOf(hostPattern))
    }
    
    val subdomains = when (subdomainMode) {
        SubdomainMode.ANY -> null
        SubdomainMode.EXACT -> Subdomains.None
        SubdomainMode.WILDCARD -> Subdomains.Any
    }
    
    val hostCond = HostCond(
        domains = domains,
        subdomains = subdomains
    )
    
    val removePatterns = removePatterns
        .filter { it.isNotBlank() }
        .map { Pattern(it) }
    
    val warningSettings = if (warnOnCredentials || sensitiveParams.isNotEmpty()) {
        WarningSettings(
            warnOnEmbeddedCredentials = if (warnOnCredentials) true else null,
            sensitiveParams = if (sensitiveParams.isNotEmpty()) sensitiveParams else null
        )
    } else null
    
    return UrlRule(
        when_ = WhenBlock(
            host = hostCond,
            schemes = if (schemes.isNotEmpty()) schemes else null
        ),
        then = ThenBlock(
            remove = removePatterns,
            warn = warningSettings
        ),
        metadata = if (metadata.isNotEmpty()) metadata.mapValues { it.value as Any? } else null
    )
}

/**
 * Convert UrlRule back to form data for editing
 */
fun UrlRule.toFormData(): RuleEditFormData {
    val hostPattern = when (val domains = when_.host.domains) {
        is Domains.Any -> "*"
        is Domains.ListOf -> domains.values.firstOrNull() ?: ""
    }
    
    val subdomainMode = when {
        when_.host.domains is Domains.Any -> SubdomainMode.ANY
        when_.host.subdomains is Subdomains.Any -> SubdomainMode.WILDCARD
        else -> SubdomainMode.EXACT
    }
    
    return RuleEditFormData(
        hostPattern = hostPattern,
        subdomainMode = subdomainMode,
        removePatterns = then.remove.map { it.pattern }.ifEmpty { listOf("") },
        schemes = when_.schemes ?: listOf("http", "https"),
        warnOnCredentials = then.warn?.warnOnEmbeddedCredentials == true,
        sensitiveParams = then.warn?.sensitiveParams ?: emptyList(),
        metadata = metadata?.mapValues { it.value.toString() } ?: emptyMap()
    )
}

/**
 * Get a human-readable description of the rule
 */
fun UrlRule.getDescription(): String {
    val hostDesc = when (val domains = when_.host.domains) {
        is Domains.Any -> "All domains"
        is Domains.ListOf -> {
            val host = domains.values.firstOrNull() ?: "unknown"
            when (when_.host.subdomains) {
                is Subdomains.Any -> "*.${host}"
                is Subdomains.None -> host
                is Subdomains.OneOf -> "${when_.host.subdomains.labels.joinToString("|")}.${host}"
                null -> host
            }
        }
    }
    
    val removeCount = then.remove.size
    val removeDesc = if (removeCount > 0) {
        "$removeCount parameter${if (removeCount != 1) "s" else ""}"
    } else {
        "no parameters"
    }
    
    return "Remove $removeDesc from $hostDesc"
}
