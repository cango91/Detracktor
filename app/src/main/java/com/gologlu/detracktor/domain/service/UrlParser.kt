package com.gologlu.detracktor.domain.service

import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.model.MaybeUrl
import com.gologlu.detracktor.domain.model.UrlParts

/**
 * Service Provider Interface (SPI) for URL parsing.
 * Allows swapping different URL parsing implementations (URI, HttpUrl, etc.) via dependency injection.
 * This abstraction enables testing with mock parsers and adapting to different URL parsing libraries.
 */
fun interface UrlParser {
    /**
     * Parses a raw URL string into its component parts.
     *
     * @param raw The raw URL string to parse
     * @return A [DomainResult] containing either the parsed [UrlParts] or a parsing error
     */
    fun parse(raw: MaybeUrl): DomainResult<UrlParts>
}