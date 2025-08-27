package com.gologlu.detracktor.utils

import android.net.Uri
import android.util.Log

/**
 * Analyzes URLs for privacy-sensitive components and provides safe display formatting
 */
object UrlPrivacyAnalyzer {
    private const val TAG = "UrlPrivacyAnalyzer"
    
    /**
     * Result of URL privacy analysis with component breakdown
     */
    data class UrlPrivacyAnalysis(
        val scheme: String,                      // URL scheme (http/https)
        val safeHost: String,                    // Host without credentials
        val port: String,                        // Port number if present
        val path: String,                        // Path component
        val fragment: String,                    // Fragment component
        val hasCredentials: Boolean,             // Whether URL contains username:password
        val matchingParameters: Map<String, String>, // Parameters that match cleaning rules
        val nonMatchingParameters: Map<String, String> // Parameters that don't match rules (values blurred by default)
    )
    
    /**
     * Extracts safe host without credentials
     */
    private fun extractSafeHost(uri: Uri): String {
        return uri.host ?: ""
    }
    
    /**
     * Extracts port as string, filtering out standard ports
     */
    private fun extractPort(uri: Uri): String {
        val port = uri.port
        return if (port != -1 && port != 80 && port != 443) {
            port.toString()
        } else {
            ""
        }
    }
    
    /**
     * Simplified parameter analysis - just matching vs non-matching parameters
     * Non-matching parameters have their values blurred by default for privacy
     */
    fun analyzeUrlEnhanced(
        url: String, 
        parametersToRemove: List<String>
    ): UrlPrivacyAnalysis {
        return try {
            if (url.isEmpty()) {
                return UrlPrivacyAnalysis(
                    scheme = "",
                    safeHost = "",
                    port = "",
                    path = "",
                    fragment = "",
                    hasCredentials = false,
                    matchingParameters = emptyMap(),
                    nonMatchingParameters = emptyMap()
                )
            }
            
            val uri = Uri.parse(url)
            val scheme = uri.scheme ?: ""
            val safeHost = extractSafeHost(uri)
            val port = extractPort(uri)
            val path = uri.path ?: ""
            val fragment = uri.fragment ?: ""
            val hasCredentials = uri.userInfo != null
            
            val allParams = uri.queryParameterNames?.toList() ?: emptyList()
            val matchingParams = mutableMapOf<String, String>()
            val nonMatchingParams = mutableMapOf<String, String>()
            
            allParams.forEach { paramName ->
                val paramValue = uri.getQueryParameter(paramName) ?: ""
                if (parametersToRemove.contains(paramName)) {
                    matchingParams[paramName] = paramValue
                } else {
                    nonMatchingParams[paramName] = paramValue
                }
            }
            
            UrlPrivacyAnalysis(
                scheme = scheme,
                safeHost = safeHost,
                port = port,
                path = path,
                fragment = fragment,
                hasCredentials = hasCredentials,
                matchingParameters = matchingParams,
                nonMatchingParameters = nonMatchingParams
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to analyze URL for enhanced privacy: $url", e)
            UrlPrivacyAnalysis(
                scheme = "",
                safeHost = "",
                port = "",
                path = "",
                fragment = "",
                hasCredentials = false,
                matchingParameters = emptyMap(),
                nonMatchingParameters = emptyMap()
            )
        }
    }
}
