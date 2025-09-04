package com.gologlu.detracktor.domain.model
import com.gologlu.detracktor.domain.error.DomainException
import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.error.ValidationError
import com.gologlu.detracktor.domain.error.flatMap
import com.gologlu.detracktor.domain.error.getOrThrow
import com.gologlu.detracktor.domain.service.UrlParser
/**
 * Interface defining the contract for URL-like objects.
 * Provides access to standard URL components and string conversion.
 */
interface IUrl {
    /** The URL scheme (e.g., "http", "https") */
    val scheme: String
    /** The host name or IP address */
    val host: String
    /** Optional port number */
    val port: Int?
    /** Optional user information (username:password) */
    val userInfo: String?
    /** Optional path component */
    val path: String?
    /** Query parameters as ordered pairs (preserves exact order and duplicates) */
    val queryPairs: QueryPairs
    /** Optional fragment identifier */
    val fragment: String?

    /** Query parameters as a map (convenience accessor for grouped access) */
    val queryMap: QueryMap get() = QueryMap.from(queryPairs)

    /**
     * Converts this URL to its string representation.
     * Delegates to UrlParts.toUrlString() to ensure consistent rendering across the domain model.
     *
     * @return The complete URL as a string
     */
    fun asString(): String {
        val urlParts = UrlParts(
            scheme = scheme,
            host = host,
            port = port,
            userInfo = userInfo,
            path = path,
            queryPairs = queryPairs,
            fragment = fragment
        )
        return urlParts.toUrlString()
    }
}
/**
 * Represents a validated URL with guaranteed scheme and host components.
 * This is a value class wrapping [UrlParts] with additional validation to ensure
 * required components (scheme and host) are present.
 */
data class Url private constructor(val parts: UrlParts) : IUrl {
    override val scheme: String
        get() = parts.scheme ?: throw DomainException(ValidationError.InvalidUrl("Invalid URL: scheme is required"))
    override val host: String
        get() = parts.host ?: throw DomainException(ValidationError.InvalidUrl("Invalid URL: host is required"))
    override val port: Int?
        get() = parts.port
    override val userInfo: String?
        get() = parts.userInfo
    override val path: String?
        get() = parts.path
    override val queryPairs: QueryPairs
        get() = parts.queryPairs
    override val fragment: String?
        get() = parts.fragment
    
    /**
     * Converts this URL to its string representation.
     * Delegates directly to the underlying UrlParts.toUrlString() for consistent rendering.
     */
    override fun asString(): String = parts.toUrlString()
    
    /**
     * Returns the URL string representation for debugging and logging.
     * This makes debugging friendlier and reduces accidental raw prints.
     */
    override fun toString(): String = asString()
    
    companion object {
        /**
         * Creates a validated Url from a raw URL string.
         * Parses the URL using the provided UrlParser and validates that required
         * components (scheme and host) are present.
         *
         * Uses flatMap for better error propagation instead of try/catch to preserve
         * original parser error messages.
         *
         * @param raw The raw URL string to parse and validate
         * @return A [DomainResult] containing either a valid [Url] or a [ValidationError.InvalidUrl]
         */
        context(pars: UrlParser)
        fun from(raw: MaybeUrl): DomainResult<Url> {
            return pars.parse(raw).flatMap { parts: UrlParts ->
                // Early validation for required components
                when {
                    parts.scheme == null -> DomainResult.failure(
                        ValidationError.InvalidUrl("Invalid URL: scheme is required")
                    )
                    parts.host == null -> DomainResult.failure(
                        ValidationError.InvalidUrl("Invalid URL: host is required")
                    )
                    else -> DomainResult.success(Url(parts))
                }
            }
        }
    }
}
