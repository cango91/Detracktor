package com.gologlu.detracktor.domain.model

/**
 * Represents a single query parameter token that preserves the exact wire format.
 * 
 * This class maintains the raw key and value as they appeared in the original query string,
 * including whether an equals sign was present. This allows for lossless round-trip
 * conversion: from(queryString).asString() == queryString.
 * 
 * Examples:
 * - "flag" -> QueryToken("flag", hasEquals=false, "")
 * - "flag=" -> QueryToken("flag", hasEquals=true, "")
 * - "key=value" -> QueryToken("key", hasEquals=true, "value")
 * - "key%20name=value%20data" -> QueryToken("key%20name", hasEquals=true, "value%20data")
 */
data class QueryToken(
    /**
     * The raw key as it appeared in the query string (URL-encoded).
     */
    val rawKey: String,
    
    /**
     * Whether an equals sign was present in the original query string.
     * This distinguishes between "flag" (hasEquals=false) and "flag=" (hasEquals=true).
     */
    val hasEquals: Boolean,
    
    /**
     * The raw value as it appeared in the query string (URL-encoded).
     * Empty string if no value was present.
     */
    val rawValue: String
) {
    /**
     * Cached decoded key for performance. Uses lazy initialization with NONE thread safety
     * since QueryToken instances are typically immutable and accessed from single threads.
     */
    val decodedKey: String by lazy(LazyThreadSafetyMode.NONE) { decodedKeyOrRaw() }
    
    /**
     * Cached decoded value for performance. Uses lazy initialization with NONE thread safety
     * since QueryToken instances are typically immutable and accessed from single threads.
     */
    val decodedValue: String by lazy(LazyThreadSafetyMode.NONE) { decodedValueOrRaw() }
    
    /**
     * Returns the decoded key, or the raw key if decoding fails.
     * This is used for matching operations while preserving the raw key for rendering.
     * 
     * Uses proper percent-decoding that only handles %XX sequences, unlike URLDecoder
     * which also converts + to space (application/x-www-form-urlencoded behavior).
     */
    fun decodedKeyOrRaw(): String {
        return try {
            UrlCodec.percentDecodeUtf8(rawKey)
        } catch (e: Exception) {
            rawKey
        }
    }
    
    /**
     * Returns the decoded value, or the raw value if decoding fails.
     * 
     * Uses proper percent-decoding that only handles %XX sequences, unlike URLDecoder
     * which also converts + to space (application/x-www-form-urlencoded behavior).
     */
    fun decodedValueOrRaw(): String {
        return try {
            UrlCodec.percentDecodeUtf8(rawValue)
        } catch (e: Exception) {
            rawValue
        }
    }
    
    
    /**
     * Renders this token back to its original query string format.
     */
    fun asString(): String {
        return if (hasEquals) {
            "$rawKey=$rawValue"
        } else {
            rawKey
        }
    }
}
