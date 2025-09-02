package com.gologlu.detracktor.application.types

data class AppSettings(
    val sites: List<UrlRule>, // schema: rules.schema.json
    val version: UInt = VERSION
){
    init { require(version <= VERSION && version >= 1U) { "version must be <= $VERSION and >= 1" } }
    companion object {
        const val VERSION: UInt = 1U
    }
}

data class WarningSettings(
    val warnOnEmbeddedCredentials: Boolean? = null,
    val sensitiveParams: List<String>? = null,
    val version: UInt = VERSION
){
    init { require(version <= VERSION && version >= 1U) { "version must be <= $VERSION and >= 1" } }
    companion object {
        const val VERSION: UInt = 1U
    }
}


// Root
data class UrlRule(
    val when_: WhenBlock,   // JSON key "when" (`when` reserved keyword in Kotlin)
    val then: ThenBlock,
    val metadata: Map<String, Any?>? = null,
    val version: UInt = VERSION
){
    init { require(version <= VERSION && version >= 1U) { "version must be <= $VERSION and >= 1" } }
    companion object {
        const val VERSION: UInt = 1U;
    }
}

data class WhenBlock(
    val host: HostCond,
    val schemes: List<String>? = null // defaults to ["http","https"]
)

// ---- Host conditions per schema ----

/** domains: "*" OR array of strings */
sealed interface Domains {
    object Any : Domains
    data class ListOf(val values: List<String>) : Domains
}

/**
 * subdomains:
 * - "*" (any)
 * - ""  (none)
 * - array of labels (empty array means none)
 */
sealed interface Subdomains {
    object Any : Subdomains            // "*"
    object None : Subdomains           // "" or []
    data class OneOf(val labels: List<String>) : Subdomains // exactly one label must match
}

data class HostCond(
    val domains: Domains,
    val subdomains: Subdomains? = null // must be present iff domains is array (schema enforces this)
)

// ---- Actions ----
data class ThenBlock(
    val remove: List<Pattern>,  // param-name glob patterns (decoded names), e.g., "utm_*", "gclid"
    val warn: WarningSettings?  // warning behavior defined per rule; use Domains.Any for catch-all
)