package com.gologlu.detracktor.domain.model

/**
 * Represents query parameters as an ordered list of query tokens that preserve exact wire format.
 * Unlike QueryMap, this preserves exact order, allows duplicate parameter names, and maintains
 * the original format including whether equals signs were present.
 * 
 * This implementation ensures lossless round-trip conversion: from(queryString).asString() == queryString
 */
@PlatformInline
data class QueryPairs private constructor(val tokens: List<QueryToken>) {
    companion object {
        // Precompiled regex patterns to avoid compilation overhead in hot path
        private val AMP_ONLY = "&".toRegex()
        private val AMP_OR_SEMI = "[&;]".toRegex()
        
        /**
         * Parses a query string into QueryPairs, preserving exact wire format for lossless round-trip.
         * 
         * @param queryRaw The raw query string to parse
         * @param acceptSemicolon If true, also accept semicolon (;) as a parameter delimiter in addition to ampersand (&)
         * 
         * Examples:
         * - "flag" -> QueryToken("flag", hasEquals=false, "")
         * - "flag=" -> QueryToken("flag", hasEquals=true, "")
         * - "key=value" -> QueryToken("key", hasEquals=true, "value")
         * - "=value" -> QueryToken("", hasEquals=true, "value") [preserved for lossless]
         * - "=" -> QueryToken("", hasEquals=true, "") [preserved for lossless]
         * - "a=1&flag&b=2" -> [QueryToken("a", true, "1"), QueryToken("flag", false, ""), QueryToken("b", true, "2")]
         * - "a=1&&b=2" -> [QueryToken("a", true, "1"), QueryToken("", false, ""), QueryToken("b", true, "2")] [empty segment preserved]
         * - "a=1;b=2" (with acceptSemicolon=true) -> [QueryToken("a", true, "1"), QueryToken("b", true, "2")]
         */
        fun from(queryRaw: String?, acceptSemicolon: Boolean = false): QueryPairs {
            if (queryRaw.isNullOrEmpty()) {
                return QueryPairs(emptyList())
            }
            
            // Use precompiled regex patterns to avoid compilation overhead in hot path
            val delimiterRegex = if (acceptSemicolon) AMP_OR_SEMI else AMP_ONLY
            
            val tokens = queryRaw.split(delimiterRegex, limit = 0)
                .map { param ->
                    val equalsIndex = param.indexOf('=')
                    if (equalsIndex == -1) {
                        // No equals sign: "flag" or empty segment ""
                        QueryToken(rawKey = param, hasEquals = false, rawValue = "")
                    } else {
                        // Has equals sign: "key=value", "key=", "=value", or "="
                        val rawKey = param.substring(0, equalsIndex)
                        val rawValue = param.substring(equalsIndex + 1)
                        
                        // Preserve ALL cases for lossless round-trip, including =value and lone =
                        QueryToken(rawKey = rawKey, hasEquals = true, rawValue = rawValue)
                    }
                }
            
            return QueryPairs(tokens)
        }
        
        fun empty(): QueryPairs = QueryPairs(emptyList())
        
        fun of(vararg pairs: Pair<String, String>): QueryPairs {
            val tokens = pairs.map { (key, value) ->
                QueryToken(rawKey = key, hasEquals = true, rawValue = value)
            }
            return QueryPairs(tokens)
        }
    }
    
    /**
     * Renders the query parameters back to their original string format.
     * This ensures lossless round-trip: from(x).asString() == x
     */
    fun asString(): String {
        return tokens.joinToString("&") { it.asString() }
    }
    
    /**
     * Get all values for a parameter name in order of appearance.
     * Matches against decoded keys but preserves original order.
     */
    fun getAll(name: String): List<String> {
        return tokens
            .filter { it.decodedKey.equals(name, ignoreCase = false) }
            .map { it.decodedValue }
    }
    
    /**
     * Get the first value for a parameter name, or null if not found.
     * Matches against decoded keys.
     */
    fun getFirst(name: String): String? {
        return tokens
            .firstOrNull { it.decodedKey.equals(name, ignoreCase = false) }
            ?.decodedValue
    }
    
    /**
     * Add a parameter at the end of the list.
     * The new parameter will have hasEquals=true.
     * Note: This method treats inputs as raw (already encoded) values.
     */
    fun add(name: String, value: String): QueryPairs {
        val newToken = QueryToken(rawKey = name, hasEquals = true, rawValue = value)
        return QueryPairs(tokens + newToken)
    }
    
    /**
     * Add a parameter with raw (already encoded) key and value.
     * Use this when you have pre-encoded values that should be stored as-is.
     * The new parameter will have hasEquals=true.
     */
    fun addRaw(rawKey: String, rawValue: String): QueryPairs {
        val newToken = QueryToken(rawKey = rawKey, hasEquals = true, rawValue = rawValue)
        return QueryPairs(tokens + newToken)
    }
    
    /**
     * Add a parameter with decoded key and value that will be percent-encoded for storage.
     * Use this when you have plain text values that need to be encoded.
     * The new parameter will have hasEquals=true.
     */
    fun addDecoded(name: String, value: String): QueryPairs {
        val encodedKey = UrlCodec.percentEncodeUtf8(name)
        val encodedValue = UrlCodec.percentEncodeUtf8(value)
        val newToken = QueryToken(rawKey = encodedKey, hasEquals = true, rawValue = encodedValue)
        return QueryPairs(tokens + newToken)
    }
    
    
    /**
     * Remove all parameters with the given name.
     * Matches against decoded keys.
     */
    fun remove(name: String): QueryPairs {
        return QueryPairs(tokens.filter { !it.decodedKey.equals(name, ignoreCase = false) })
    }
    
    /**
     * Replace the first occurrence of a parameter with a new value, preserving position.
     * If the parameter doesn't exist, adds it at the end.
     * Matches against decoded keys.
     */
    fun replaceFirst(name: String, value: String): QueryPairs {
        val firstIndex = tokens.indexOfFirst { it.decodedKey.equals(name, ignoreCase = false) }
        return if (firstIndex >= 0) {
            val newToken = QueryToken(rawKey = name, hasEquals = true, rawValue = value)
            val newTokens = tokens.toMutableList()
            newTokens[firstIndex] = newToken
            // Remove any additional occurrences
            val filteredTokens = newTokens.filterIndexed { index, token ->
                index == firstIndex || !token.decodedKey.equals(name, ignoreCase = false)
            }
            QueryPairs(filteredTokens)
        } else {
            add(name, value)
        }
    }
    
    /**
     * Replace the first occurrence of a parameter with a new decoded value, preserving position.
     * If the parameter doesn't exist, adds it at the end.
     * The key and value will be percent-encoded before storage.
     * Matches against decoded keys.
     */
    fun replaceFirstDecoded(name: String, value: String): QueryPairs {
        val encodedKey = UrlCodec.percentEncodeUtf8(name)
        val encodedValue = UrlCodec.percentEncodeUtf8(value)
        val firstIndex = tokens.indexOfFirst { it.decodedKey.equals(name, ignoreCase = false) }
        return if (firstIndex >= 0) {
            val newToken = QueryToken(rawKey = encodedKey, hasEquals = true, rawValue = encodedValue)
            val newTokens = tokens.toMutableList()
            newTokens[firstIndex] = newToken
            // Remove any additional occurrences
            val filteredTokens = newTokens.filterIndexed { index, token ->
                index == firstIndex || !token.decodedKey.equals(name, ignoreCase = false)
            }
            QueryPairs(filteredTokens)
        } else {
            addDecoded(name, value)
        }
    }
    
    
    /**
     * Remove parameters matching the given predicate on decoded keys.
     */
    fun removeWhere(predicate: (String) -> Boolean): QueryPairs {
        return QueryPairs(tokens.filter { !predicate(it.decodedKey) })
    }
    
    /**
     * Remove parameters where the decoded key matches the given predicate.
     * This is an alias for removeWhere for clarity.
     */
    fun removeWhereDecoded(predicate: (String) -> Boolean): QueryPairs {
        return removeWhere(predicate)
    }
    
    /**
     * Remove parameters where the decoded key matches any of the given predicates.
     * A parameter is removed if ANY of the predicates returns true for its decoded key.
     */
    fun removeAnyOf(predicates: List<(String) -> Boolean>): QueryPairs {
        return QueryPairs(tokens.filter { token ->
            val decodedKey = token.decodedKey
            predicates.none { predicate -> predicate(decodedKey) }
        })
    }
    
    /**
     * Keep only parameters where the decoded key matches the given predicate.
     * This is a thin wrapper over removeWhere with inverted logic for better readability.
     */
    fun filterKeys(predicate: (String) -> Boolean): QueryPairs {
        return QueryPairs(tokens.filter { predicate(it.decodedKey) })
    }
    
    /**
     * Convert to QueryMap, losing order information but gaining grouped access.
     * This now uses direct conversion instead of inefficient reparsing.
     */
    fun toQueryMap(): QueryMap {
        return QueryMap.from(this)
    }
    
    fun isEmpty(): Boolean = tokens.isEmpty()
    fun isNotEmpty(): Boolean = tokens.isNotEmpty()
    fun size(): Int = tokens.size
    
    // Removed getTokens() function - use the 'tokens' property directly
}
