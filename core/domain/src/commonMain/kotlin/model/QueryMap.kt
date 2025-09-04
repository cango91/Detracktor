package com.gologlu.detracktor.domain.model


private typealias ParamName = String
private typealias ParamValue = String

/**
 * Represents URL query parameters as a map where parameter names can have multiple values.
 * This provides convenient access to query parameters grouped by name.
 * This is now a convenience wrapper around [QueryPairs] to ensure exact order preservation.
 * For direct access to the ordered representation, use [QueryPairs] instead.
 * 
 * ## Case Policy
 * 
 * Parameter name matching in this domain layer implementation uses **strict case-sensitive matching**.
 * This means "Key", "key", and "KEY" are treated as completely different parameters.
 * 
 * This design choice ensures:
 * - Lossless round-trip conversion: from(queryString).asString() == queryString
 * - Predictable behavior regardless of locale settings
 * - Compatibility with case-sensitive systems and protocols
 * 
 * If case-insensitive parameter handling is needed for specific use cases (e.g., HTML form processing),
 * it should be implemented at the application layer using locale-safe operations with `Locale.ROOT`.
 * 
 * Example:
 * ```kotlin
 * val queryMap = QueryMap.from("Key=value1&key=value2&KEY=value3")
 * queryMap.get("Key")   // returns ["value1"]
 * queryMap.get("key")   // returns ["value2"] 
 * queryMap.get("KEY")   // returns ["value3"]
 * ```
 */
@PlatformInline
data class QueryMap private constructor(private val queryPairs: QueryPairs) {
    companion object {
        /**
         * Parses a raw query string into a QueryMap.
         * Parameters with the same name are grouped together as a list of values.
         * Order is preserved through the underlying QueryPairs representation.
         *
         * @param queryRaw The raw query string (without the '?' prefix), may be null or empty
         * @return A QueryMap containing the parsed parameters
         */
        fun from(queryRaw: String?): QueryMap {
            return QueryMap(QueryPairs.from(queryRaw))
        }
        
        /**
         * Creates a QueryMap from existing QueryPairs.
         *
         * @param queryPairs The QueryPairs to wrap
         * @return A QueryMap providing grouped access to the parameters
         */
        fun from(queryPairs: QueryPairs): QueryMap {
            return QueryMap(queryPairs)
        }
    }
    /**
     * Converts this QueryMap back to a query string format.
     * Preserves exact order and duplicates from the underlying QueryPairs.
     *
     * @return The query string representation (without '?' prefix)
     */
    fun asString(): String = queryPairs.asString()

    /**
     * Gets all values for a parameter name in order of appearance.
     *
     * @param key The parameter name to look up
     * @return A list of values for the parameter, or empty list if not found
     */
    fun get(key: ParamName): List<ParamValue> = queryPairs.getAll(key)
    
    /**
     * Sets a parameter to a single value, replacing any existing values.
     *
     * @param key The parameter name
     * @param value The parameter value
     * @return A new QueryMap with the parameter set
     */
    fun set(key: ParamName, value: ParamValue): QueryMap {
        return QueryMap(queryPairs.remove(key).add(key, value))
    }
    
    /**
     * Sets a parameter to multiple values, replacing any existing values.
     *
     * @param key The parameter name
     * @param values The list of parameter values
     * @return A new QueryMap with the parameter set
     */
    fun set(key: ParamName, values: List<ParamValue>): QueryMap {
        var result = queryPairs.remove(key)
        values.forEach { value ->
            result = result.add(key, value)
        }
        return QueryMap(result)
    }
    
    /**
     * Sets a parameter to a single decoded value, replacing any existing values.
     * The key and value will be percent-encoded before storage.
     *
     * @param key The decoded parameter name
     * @param value The decoded parameter value
     * @return A new QueryMap with the parameter set
     */
    fun setDecoded(key: String, value: String): QueryMap {
        return QueryMap(queryPairs.replaceFirstDecoded(key, value))
    }
    
    /**
     * Sets a parameter to multiple decoded values, replacing any existing values.
     * The key and values will be percent-encoded before storage.
     *
     * @param key The decoded parameter name
     * @param values The list of decoded parameter values
     * @return A new QueryMap with the parameter set
     */
    fun setDecoded(key: String, values: List<String>): QueryMap {
        if (values.isEmpty()) {
            return QueryMap(queryPairs.remove(key))
        }
        
        // Use the regular set method which handles multiple values correctly
        return set(key, values.map { UrlCodec.percentEncodeUtf8(it) })
    }
    
    /**
     * Removes a parameter completely.
     *
     * @param key The parameter name to remove
     * @return A new QueryMap without the specified parameter
     */
    fun remove(key: ParamName): QueryMap {
        return QueryMap(queryPairs.remove(key))
    }

    /**
     * Removes parameters whose names match the given predicate.
     *
     * @param predicate The predicate to test parameter names against
     * @return A new QueryMap without parameters matching the predicate
     */
    fun removeWhere(predicate: (String) -> Boolean): QueryMap {
        return QueryMap(queryPairs.removeWhere(predicate))
    }
    
    /**
     * Removes parameters whose names match any of the given predicates.
     *
     * @param predicates The list of predicates to test parameter names against
     * @return A new QueryMap without parameters matching any of the predicates
     */
    fun removeAnyOf(predicates: List<(String) -> Boolean>): QueryMap {
        return QueryMap(queryPairs.removeAnyOf(predicates))
    }

    /**
     * Gets the underlying QueryPairs representation.
     * Useful when you need direct access to the ordered parameter list.
     *
     * @return The underlying QueryPairs
     */
    fun toQueryPairs(): QueryPairs = queryPairs

    /** @return true if this QueryMap contains no parameters */
    fun isEmpty(): Boolean = queryPairs.isEmpty()
    
    /** @return true if this QueryMap contains at least one parameter */
    fun isNotEmpty(): Boolean = queryPairs.isNotEmpty()
}
