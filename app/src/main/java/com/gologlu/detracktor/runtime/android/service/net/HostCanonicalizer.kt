package com.gologlu.detracktor.runtime.android.service.net

import java.net.IDN
import java.util.Locale

object HostCanonicalizer {
    private const val DOT = '.'
    fun toAscii(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        
        var h = raw.replace('\u3002', DOT) // IDEOGRAPHIC FULL STOP
                   .replace('\uFF0E', DOT) // FULLWIDTH FULL STOP
                   .replace('\uFF61', DOT) // HALFWIDTH IDEOGRAPHIC FULL STOP
        
        // Handle multiple trailing dots - preserve single trailing dot if 4+ dots
        val trailingDots = h.takeLastWhile { it == '.' }.length
        h = h.trimEnd('.')
        if (trailingDots >= 4) {
            h += "."
        }
        
        // Convert to lowercase AFTER dot handling to preserve Unicode properly
        h = h.lowercase(Locale.ROOT)
        
        // Handle edge cases
        if (h.isEmpty()) return ""
        if (h.startsWith(".")) return null // Leading dots are invalid
        if (h.contains("..")) return null // Double dots are invalid
        if (h.contains(" ")) return null // Whitespace is invalid
        if (h.contains("@") || h.contains("#") || h.contains("$") || h.contains("%")) return null // Special chars invalid
        
        // Check for invalid hyphens in domain labels
        val labels = h.split('.')
        for (label in labels) {
            if (label.startsWith("-") || label.endsWith("-")) return null
        }
        
        return try { 
            val result = IDN.toASCII(h, IDN.ALLOW_UNASSIGNED)
            // Additional validation - if result is different and contains invalid patterns, return null
            if (result.isEmpty() || result.contains("..") || result.startsWith(".")) {
                null
            } else {
                result
            }
        } catch (_: Exception) { 
            null 
        }
    }
}
