package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.AnnotatedUrlSegment
import com.gologlu.detracktor.data.SegmentType
import com.gologlu.detracktor.data.UrlAnalysis
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * Core URL analysis with segment creation for smart partial-blur rendering.
 * Replaces the complex UrlPrivacyAnalyzer with focused functionality.
 */
class UrlAnalyzer {
    
    private val credentialDetector = CredentialDetector()
    
    /**
     * Analyze URL and create annotated segments for display.
     * Non-rule-matching parameters are marked for blurring by default.
     */
    fun analyzeUrl(url: String, matchingParameterPatterns: List<Pattern>): UrlAnalysis {
        val segments = createSegments(url, matchingParameterPatterns)
        val hasCredentials = credentialDetector.hasEmbeddedCredentials(url)
        val cleanedUrl = createCleanedUrl(url, matchingParameterPatterns)
        
        return UrlAnalysis(
            originalUrl = url,
            segments = segments,
            hasEmbeddedCredentials = hasCredentials,
            matchingRules = emptyList(), // Will be populated by caller
            cleanedUrl = cleanedUrl
        )
    }
    
    /**
     * Create annotated segments from URL for smart rendering.
     * Parameters not matching rules are marked for blurring.
     */
    fun createSegments(url: String, matchingParameterPatterns: List<Pattern>): List<AnnotatedUrlSegment> {
        val segments = mutableListOf<AnnotatedUrlSegment>()
        
        try {
            val urlParts = parseUrl(url)
            
            // Protocol
            if (urlParts.protocol.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment(urlParts.protocol, SegmentType.PROTOCOL))
                segments.add(AnnotatedUrlSegment("://", SegmentType.SEPARATOR))
            }
            
            // Credentials (always blur)
            if (urlParts.credentials.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment(urlParts.credentials, SegmentType.CREDENTIALS, shouldBlur = true))
                segments.add(AnnotatedUrlSegment("@", SegmentType.SEPARATOR))
            }
            
            // Host
            if (urlParts.host.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment(urlParts.host, SegmentType.HOST))
            }
            
            // Port
            if (urlParts.port.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment(":", SegmentType.SEPARATOR))
                segments.add(AnnotatedUrlSegment(urlParts.port, SegmentType.HOST))
            }
            
            // Path
            if (urlParts.path.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment(urlParts.path, SegmentType.PATH))
            }
            
            // Parameters
            if (urlParts.parameters.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment("?", SegmentType.SEPARATOR))
                addParameterSegments(segments, urlParts.parameters, matchingParameterPatterns)
            }
            
            // Fragment
            if (urlParts.fragment.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment("#", SegmentType.SEPARATOR))
                segments.add(AnnotatedUrlSegment(urlParts.fragment, SegmentType.PATH))
            }
            
        } catch (e: Exception) {
            // Fallback: treat entire URL as single segment
            segments.clear()
            segments.add(AnnotatedUrlSegment(url, SegmentType.HOST))
        }
        
        return segments
    }
    
    /**
     * Create cleaned URL by removing matching parameters.
     */
    private fun createCleanedUrl(url: String, matchingParameterPatterns: List<Pattern>): String {
        return try {
            val urlParts = parseUrl(url)
            val cleanedParams = urlParts.parameters.filter { param ->
                val paramName = param.substringBefore("=")
                !matchingParameterPatterns.any { pattern ->
                    pattern.matcher(paramName).matches()
                }
            }
            
            buildString {
                if (urlParts.protocol.isNotEmpty()) {
                    append(urlParts.protocol)
                    append("://")
                }
                if (urlParts.credentials.isNotEmpty()) {
                    append(urlParts.credentials)
                    append("@")
                }
                append(urlParts.host)
                if (urlParts.port.isNotEmpty()) {
                    append(":")
                    append(urlParts.port)
                }
                append(urlParts.path)
                if (cleanedParams.isNotEmpty()) {
                    append("?")
                    append(cleanedParams.joinToString("&"))
                }
                if (urlParts.fragment.isNotEmpty()) {
                    append("#")
                    append(urlParts.fragment)
                }
            }
        } catch (e: Exception) {
            url // Return original URL if parsing fails
        }
    }
    
    /**
     * Add parameter segments with smart blurring logic.
     */
    private fun addParameterSegments(
        segments: MutableList<AnnotatedUrlSegment>,
        parameters: List<String>,
        matchingParameterPatterns: List<Pattern>
    ) {
        parameters.forEachIndexed { index, param ->
            if (index > 0) {
                segments.add(AnnotatedUrlSegment("&", SegmentType.SEPARATOR))
            }
            
            val parts = param.split("=", limit = 2)
            val paramName = parts[0]
            val paramValue = if (parts.size > 1) parts[1] else ""
            
            // Parameter name
            segments.add(AnnotatedUrlSegment(paramName, SegmentType.PARAM_NAME))
            
            if (paramValue.isNotEmpty()) {
                segments.add(AnnotatedUrlSegment("=", SegmentType.SEPARATOR))
                
                // Parameter value - blur if not matching any rule
                val shouldBlurValue = !matchingParameterPatterns.any { pattern ->
                    pattern.matcher(paramName).matches()
                }
                
                segments.add(AnnotatedUrlSegment(
                    paramValue,
                    SegmentType.PARAM_VALUE,
                    shouldBlur = shouldBlurValue
                ))
            }
        }
    }
    
    /**
     * Parse URL into components.
     */
    private fun parseUrl(url: String): UrlParts {
        val cleanUrl = url.trim()
        
        // Extract protocol
        val protocolMatch = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):").find(cleanUrl)
        val protocol = protocolMatch?.groups?.get(1)?.value ?: ""
        val afterProtocol = if (protocol.isNotEmpty()) {
            cleanUrl.substring(protocol.length + 3) // +3 for "://"
        } else {
            cleanUrl
        }
        
        // Extract credentials
        val credentialsMatch = Regex("^([^@/]+@)").find(afterProtocol)
        val credentials = credentialsMatch?.groups?.get(1)?.value?.dropLast(1) ?: "" // Remove @
        val afterCredentials = if (credentials.isNotEmpty()) {
            afterProtocol.substring(credentials.length + 1) // +1 for @
        } else {
            afterProtocol
        }
        
        // Split remaining parts
        val parts = afterCredentials.split("?", "#", limit = 3)
        val hostAndPath = parts[0]
        val queryString = if (parts.size > 1 && afterCredentials.contains("?")) parts[1] else ""
        val fragment = if (parts.size > 2 || (parts.size > 1 && afterCredentials.contains("#"))) {
            parts.last()
        } else ""
        
        // Extract host and path
        val pathStart = hostAndPath.indexOf("/")
        val (hostPart, path) = if (pathStart >= 0) {
            hostAndPath.substring(0, pathStart) to hostAndPath.substring(pathStart)
        } else {
            hostAndPath to ""
        }
        
        // Extract port from host
        val portMatch = Regex(":([0-9]+)$").find(hostPart)
        val port = portMatch?.groups?.get(1)?.value ?: ""
        val host = if (port.isNotEmpty()) {
            hostPart.substring(0, hostPart.length - port.length - 1)
        } else {
            hostPart
        }
        
        // Parse parameters
        val parameters = if (queryString.isNotEmpty()) {
            queryString.split("&").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        return UrlParts(protocol, credentials, host, port, path, parameters, fragment)
    }
    
    private data class UrlParts(
        val protocol: String,
        val credentials: String,
        val host: String,
        val port: String,
        val path: String,
        val parameters: List<String>,
        val fragment: String
    )
}
