package com.gologlu.detracktor.utils

import android.util.Log
import com.gologlu.detracktor.data.HostNormalizationConfig
import com.gologlu.detracktor.data.NormalizedHost
import com.ibm.icu.text.IDNA
import java.util.Locale

/**
 * Host normalization engine with comprehensive IDN support and port normalization.
 * Handles case normalization, port defaults, and full IDN/Punycode conversion.
 */
class HostNormalizer(private val config: HostNormalizationConfig) {
    
    companion object {
        private const val TAG = "HostNormalizer"
        private val IDNA_PROCESSOR = IDNA.getUTS46Instance(IDNA.DEFAULT)
    }
    
    /**
     * Primary normalization function that applies all configured normalizations.
     */
    fun normalizeHost(host: String, scheme: String = "https"): NormalizedHost {
        try {
            val original = host.trim()
            if (original.isEmpty()) {
                return NormalizedHost(original, original, null, false, null)
            }
            
            // Extract port if present
            val (hostWithoutPort, extractedPort) = extractPort(original)
            
            // Apply case normalization
            val caseNormalized = if (config.enableCaseNormalization) {
                normalizeCasing(hostWithoutPort)
            } else {
                hostWithoutPort
            }
            
            // Apply IDN conversion
            val (idnNormalized, isIDN, punycode) = if (config.enableIDNConversion) {
                convertIDN(caseNormalized)
            } else {
                Triple(caseNormalized, false, null)
            }
            
            // Apply port normalization
            val finalPort = if (config.enablePortNormalization) {
                normalizePort(extractedPort, scheme)
            } else {
                extractedPort
            }
            
            return NormalizedHost(
                original = original,
                normalized = idnNormalized,
                port = finalPort,
                isIDN = isIDN,
                punycode = punycode
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to normalize host: $host", e)
            return NormalizedHost(host, host, null, false, null)
        }
    }
    
    /**
     * Normalize host casing to lowercase.
     */
    fun normalizeCasing(host: String): String {
        return host.lowercase(Locale.ROOT)
    }
    
    /**
     * Extract port from host string.
     * Returns pair of (host_without_port, port_number_or_null)
     */
    fun extractPort(hostWithPort: String): Pair<String, Int?> {
        // Handle IPv6 addresses with brackets
        if (hostWithPort.startsWith("[")) {
            val closingBracket = hostWithPort.indexOf(']')
            if (closingBracket != -1) {
                val ipv6Host = hostWithPort.substring(0, closingBracket + 1)
                val remainder = hostWithPort.substring(closingBracket + 1)
                if (remainder.startsWith(":")) {
                    val portStr = remainder.substring(1)
                    return try {
                        Pair(ipv6Host, portStr.toInt())
                    } catch (e: NumberFormatException) {
                        Pair(hostWithPort, null)
                    }
                }
                return Pair(ipv6Host, null)
            }
        }
        
        // Handle regular hosts with port
        val lastColonIndex = hostWithPort.lastIndexOf(':')
        if (lastColonIndex != -1 && lastColonIndex < hostWithPort.length - 1) {
            val hostPart = hostWithPort.substring(0, lastColonIndex)
            val portPart = hostWithPort.substring(lastColonIndex + 1)
            
            return try {
                val port = portPart.toInt()
                if (port in 1..65535) {
                    Pair(hostPart, port)
                } else {
                    Pair(hostWithPort, null)
                }
            } catch (e: NumberFormatException) {
                Pair(hostWithPort, null)
            }
        }
        
        return Pair(hostWithPort, null)
    }
    
    /**
     * Normalize port by removing default ports for schemes.
     */
    fun normalizePort(port: Int?, scheme: String): Int? {
        if (port == null) return null
        
        val defaultPort = config.defaultPorts[scheme.lowercase(Locale.ROOT)]
        return if (port == defaultPort) null else port
    }
    
    /**
     * Convert between Unicode and Punycode IDN formats.
     * Returns Triple of (normalized_host, is_idn, punycode_version)
     */
    fun convertIDN(host: String): Triple<String, Boolean, String?> {
        try {
            // Check if host contains non-ASCII characters (Unicode IDN)
            val hasNonAscii = host.any { it.code > 127 }
            
            if (hasNonAscii) {
                // Convert Unicode to Punycode
                val info = IDNA.Info()
                val punycode = IDNA_PROCESSOR.nameToASCII(host, StringBuilder(), info).toString()
                
                if (!info.hasErrors()) {
                    return Triple(punycode, true, punycode)
                }
            } else if (host.contains("xn--")) {
                // Convert Punycode to Unicode
                val info = IDNA.Info()
                val unicode = IDNA_PROCESSOR.nameToUnicode(host, StringBuilder(), info).toString()
                
                if (!info.hasErrors()) {
                    return Triple(unicode, true, host)
                }
            }
            
            return Triple(host, false, null)
            
        } catch (e: Exception) {
            Log.w(TAG, "IDN conversion failed for host: $host", e)
            return Triple(host, false, null)
        }
    }
    
    /**
     * Convert Unicode IDN to Punycode.
     */
    fun convertToIDN(host: String): String {
        return try {
            val info = IDNA.Info()
            val result = IDNA_PROCESSOR.nameToASCII(host, StringBuilder(), info).toString()
            if (info.hasErrors()) host else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert to IDN: $host", e)
            host
        }
    }
    
    /**
     * Convert Punycode IDN to Unicode.
     */
    fun convertFromIDN(host: String): String {
        return try {
            val info = IDNA.Info()
            val result = IDNA_PROCESSOR.nameToUnicode(host, StringBuilder(), info).toString()
            if (info.hasErrors()) host else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert from IDN: $host", e)
            host
        }
    }
    
    /**
     * Denormalize a NormalizedHost back to a displayable format.
     */
    fun denormalize(normalizedHost: NormalizedHost): String {
        val host = if (normalizedHost.isIDN && normalizedHost.punycode != null) {
            convertFromIDN(normalizedHost.punycode)
        } else {
            normalizedHost.normalized
        }
        
        return if (normalizedHost.port != null) {
            if (host.startsWith("[") && host.endsWith("]")) {
                "$host:${normalizedHost.port}"
            } else {
                "$host:${normalizedHost.port}"
            }
        } else {
            host
        }
    }
}
