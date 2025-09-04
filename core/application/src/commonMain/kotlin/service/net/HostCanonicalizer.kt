package com.gologlu.detracktor.application.service.net

/**
 * SPI for host canonicalization. Implemented in runtime.
 */
fun interface HostCanonicalizer {
    /**
     * Convert a raw host string to a canonical ASCII representation, or null if invalid.
     */
    fun toAscii(raw: String?): String?
}


