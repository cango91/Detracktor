package com.gologlu.detracktor.domain.model

import java.util.Locale

/**
 * Represents the parsed components of a URL.
 * This is used as an intermediate representation during URL parsing,
 * before validation creates a proper [Url] instance.
 *
 * @param scheme The URL scheme (e.g., "http", "https"), may be null if not present
 * @param host The host name or IP address, may be null if not present  
 * @param port The port number, may be null if not present or using default port
 * @param userInfo Optional user information (username:password format)
 * @param path Optional path component
 * @param queryPairs The parsed query parameters as QueryPairs (preserves exact order)
 * @param fragment Optional fragment identifier (after '#')
 * 
 * ## IPv6 Zone ID Handling
 * 
 * IPv6 addresses with zone IDs (e.g., `fe80::1%eth0`) require special handling:
 * 
 * - **Zone ID Encoding**: Zone IDs must be percent-encoded before being passed to UrlParts.
 *   The `%` character in zone IDs should be encoded as `%25` to avoid conflicts with 
 *   percent-encoding syntax.
 * 
 * - **Example**: `fe80::1%eth0` should be passed as `fe80::1%25eth0` in the host parameter.
 * 
 * - **Parser Responsibility**: URL parsers should handle zone ID encoding/decoding 
 *   automatically. UrlParts expects pre-encoded zone IDs and will bracket them 
 *   appropriately for URL string generation.
 * 
 * - **Bracketing**: IPv6 addresses (including those with encoded zone IDs) are 
 *   automatically bracketed in `toUrlString()` output: `[fe80::1%25eth0]`
 */
data class UrlParts(
    val scheme: String?,
    val host: String?,
    val port: Int?,
    val userInfo: String?,
    val path: String?,
    val queryPairs: QueryPairs,
    val fragment: String?
) {
    /**
     * Convenience accessor for query parameters as a QueryMap.
     * Provides grouped access to parameters while preserving the underlying order.
     */
    val queryMap: QueryMap get() = QueryMap.from(queryPairs)
    
    /**
     * Convenience accessor for raw query string.
     * Returns the query parameters as they would appear in a URL (without the '?' prefix).
     */
    val rawQuery: String get() = queryPairs.asString()
    
    /**
     * Reconstructs a complete URL string from these parts.
     * Handles proper formatting of userInfo, IPv6 addresses, ports, and all components.
     * 
     * IPv6 Host Bracketing:
     * - IPv6 addresses are automatically wrapped in brackets: [::1], [2001:db8::1]
     * - Already bracketed addresses are left unchanged
     * - IPv4 addresses and hostnames are not bracketed
     * 
     * Empty Path Handling:
     * - null or empty path results in no path component in the URL
     * - Non-empty paths are included as-is (including leading slash if present)
     * - Path normalization (adding leading slash) is handled by higher-level components
     * 
     * @return A properly formatted URL string
     */
    fun toUrlString(): String {
        val result = StringBuilder()
        
        // Add scheme
        scheme?.let { result.append("$it://") }
        
        // Add user info with @ prefix if present
        userInfo?.let { result.append("$it@") }
        
        // Add host with proper IPv6 bracketing
        host?.let { hostValue ->
            if (isIPv6Address(hostValue)) {
                result.append("[$hostValue]")
            } else {
                result.append(hostValue)
            }
        }
        
        // Add port if present (always render for lossless round-trip)
        port?.let { portValue ->
            result.append(":$portValue")
        }
        
        // Add path
        path?.let { result.append(it) }
        
        // Add query parameters
        if (queryPairs.isNotEmpty()) {
            result.append("?${queryPairs.asString()}")
        }
        
        // Add fragment
        fragment?.let { result.append("#$it") }
        
        return result.toString()
    }
    
    /**
     * Checks if a host string represents an IPv6 address that needs bracketing.
     * This is a simple heuristic check for the presence of colons.
     * 
     * IPv6 Detection Logic:
     * - Returns true if the host contains colons (IPv6 indicator) AND is not already properly bracketed
     * - Returns false for IPv4 addresses, hostnames, and properly bracketed IPv6 addresses
     * - Properly bracketed means starts with '[' AND ends with ']'
     * - This ensures we only add brackets when needed, avoiding double-bracketing for valid cases
     * - Malformed brackets (partial bracketing) will still get bracketed for safety
     */
    private fun isIPv6Address(host: String): Boolean {
        return host.contains(':') && !(host.startsWith('[') && host.endsWith(']'))
    }
    
    /**
     * Creates a copy of this UrlParts with new query parameters, preserving other components.
     * This is a common operation that keeps call sites terse.
     */
    fun copyWithQuery(queryPairs: QueryPairs): UrlParts {
        return copy(queryPairs = queryPairs)
    }
    
    /**
     * Returns the URL string representation for debugging and logging.
     * This makes debugging friendlier and reduces accidental raw prints.
     */
    override fun toString(): String = toUrlString()
}
