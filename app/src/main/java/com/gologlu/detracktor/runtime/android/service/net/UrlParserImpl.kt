package com.gologlu.detracktor.runtime.android.service.net

import android.net.Uri
import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.error.ValidationError
import com.gologlu.detracktor.domain.model.MaybeUrl
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.domain.service.UrlParser
import androidx.core.net.toUri

class UrlParserImpl : UrlParser {
    override fun parse(raw: MaybeUrl): DomainResult<UrlParts> {
        return try {
            val uri = raw.toUri()
            val parts = UrlParts(
                scheme = uri.scheme,
                host = parseHost(uri),
                port = parsePort(uri),
                userInfo = parseUserInfo(uri),
                path = parsePath(uri),
                queryPairs = parseQuery(uri),
                fragment = uri.fragment
            )
            return DomainResult.success(parts)
        } catch (e: Exception) {
            DomainResult.failure(ValidationError.InvalidUrl("Invalid URL: ${e.message ?: "unknown parsing error"}"))
        }
    }

    /**
     * Parses the host component, handling IPv6 zone ID encoding.
     * IPv6 zone IDs need to have '%' encoded as '%25' for proper handling.
     */
    private fun parseHost(uri: Uri): String? {
        val host = uri.host ?: return null

        // Handle IPv6 zone ID encoding - convert % to %25
        return if (host.contains('%')) {
            // For IPv6 addresses with zone IDs, encode the % character
            host.replace("%", "%25")
        } else {
            host
        }
    }

    /**
     * Parses the port component.
     * Returns null if port is -1 (default port) or not specified.
     */
    private fun parsePort(uri: Uri): Int? {
        val port = uri.port
        return if (port == -1) null else port
    }

    /**
     * Parses user info component (username:password format).
     */
    private fun parseUserInfo(uri: Uri): String? {
        return uri.userInfo
    }

    /**
     * Parses the path component.
     * Returns null for empty or root paths to maintain consistency.
     */
    private fun parsePath(uri: Uri): String? {
        val path = uri.path
        return if (path.isNullOrEmpty()) null else path
    }

    /**
     * Parses query parameters into QueryPairs, preserving exact order and wire format.
     * Uses android.net.Uri's query parameter handling and converts to domain QueryPairs.
     */
    private fun parseQuery(uri: Uri): QueryPairs {
        val query = uri.encodedQuery ?: return QueryPairs.empty()
        return QueryPairs.from(query)
    }
}