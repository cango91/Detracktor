package com.gologlu.detracktor.runtime.android.presentation.types

import com.gologlu.detracktor.application.types.*

/**
 * Form data structure for rule editing UI
 * Provides a more user-friendly representation of UrlRule for form editing
 */
data class RuleEditFormData(
    val domainsInput: String = "",
    val subdomainMode: SubdomainMode = SubdomainMode.NONE,
    val subdomainsInput: String = "",
    val removePatternsInput: String = "",
    val warnOnCredentials: Boolean = false,
    val sensitiveParamsInput: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val sensitiveMergeMode: SensitiveMergeModeUi = SensitiveMergeModeUi.UNION
)

/**
 * Subdomain mode for UI selection matching the actual rule schema
 */
enum class SubdomainMode {
    /** No subdomains allowed - exact domain match only */
    NONE,
    /** Any subdomains allowed (*.example.com) */
    ANY,
    /** Specific list of subdomains (comma-separated input) */
    SPECIFIC_LIST
}

enum class SensitiveMergeModeUi { UNION, REPLACE }

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
 * Helper functions for parsing comma-separated inputs
 */
fun parseCommaSeparatedList(input: String): List<String> {
    return input.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun formatCommaSeparatedList(items: List<String>): String {
    return items.joinToString(", ")
}

/**
 * Extension functions to convert between form data and domain types
 */
fun RuleEditFormData.toUrlRule(): UrlRule {
    val trimmedDomains = domainsInput.trim()
    val (domains, subdomains) = if (trimmedDomains == "*") {
        // Catch‑all: domains = Any and subdomains MUST be null per schema
        Domains.Any to null
    } else {
        val domainsList = parseCommaSeparatedList(trimmedDomains)
        val d = if (domainsList.isEmpty()) {
            Domains.ListOf(listOf(""))
        } else {
            Domains.ListOf(domainsList)
        }
        val s = when (subdomainMode) {
            SubdomainMode.NONE -> Subdomains.None
            SubdomainMode.ANY -> Subdomains.Any
            SubdomainMode.SPECIFIC_LIST -> {
                val subdomainsList = parseCommaSeparatedList(subdomainsInput)
                if (subdomainsList.isEmpty()) Subdomains.None else Subdomains.OneOf(subdomainsList)
            }
        }
        d to s
    }
    
    val hostCond = HostCond(
        domains = domains,
        subdomains = subdomains
    )
    
    val removePatterns = parseCommaSeparatedList(removePatternsInput)
        .map { Pattern(it) }
    
    val sensitiveParams = parseCommaSeparatedList(sensitiveParamsInput)
    
    val warningSettings = if (warnOnCredentials || sensitiveParams.isNotEmpty()) {
        WarningSettings(
            warnOnEmbeddedCredentials = if (warnOnCredentials) true else null,
            sensitiveParams = if (sensitiveParams.isNotEmpty()) sensitiveParams else emptyList(),
            sensitiveMerge = when (sensitiveMergeMode) {
                SensitiveMergeModeUi.UNION -> SensitiveMergeMode.UNION
                SensitiveMergeModeUi.REPLACE -> SensitiveMergeMode.REPLACE
            }
        )
    } else null
    
    return UrlRule(
        when_ = WhenBlock(
            host = hostCond,
            schemes = null // Remove scheme selection as per plan
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
    val domainsInput = when (val domains = when_.host.domains) {
        is Domains.Any -> "*"
        is Domains.ListOf -> formatCommaSeparatedList(domains.values)
    }
    
    val (subdomainMode, subdomainsInput) = when (val subdomains = when_.host.subdomains) {
        is Subdomains.None -> SubdomainMode.NONE to ""
        is Subdomains.Any -> SubdomainMode.ANY to ""
        is Subdomains.OneOf -> SubdomainMode.SPECIFIC_LIST to formatCommaSeparatedList(subdomains.labels)
        null -> SubdomainMode.NONE to ""
    }
    
    return RuleEditFormData(
        domainsInput = domainsInput,
        subdomainMode = subdomainMode,
        subdomainsInput = subdomainsInput,
        removePatternsInput = formatCommaSeparatedList(then.remove.map { it.pattern }),
        warnOnCredentials = then.warn?.warnOnEmbeddedCredentials == true,
        sensitiveParamsInput = formatCommaSeparatedList(then.warn?.sensitiveParams ?: emptyList()),
        metadata = metadata?.mapValues { it.value.toString() } ?: emptyMap(),
        sensitiveMergeMode = when (then.warn?.sensitiveMerge) {
            SensitiveMergeMode.REPLACE -> SensitiveMergeModeUi.REPLACE
            else -> SensitiveMergeModeUi.UNION
        }
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
