package com.gologlu.detracktor.domain.model

import java.util.Locale

/**
 * Utility object for percent encoding and decoding operations in URL components.
 * 
 * Provides centralized, reusable implementations of percent encoding/decoding that
 * handle Unicode properly and use locale-safe operations. This replaces scattered
 * encoding/decoding logic across QueryToken and QueryPairs classes.
 * 
 * ## Key Features
 * 
 * - **Proper Unicode Handling**: Encodes/decodes UTF-8 byte sequences correctly
 * - **Locale Safety**: Uses Locale.ROOT for all case operations to avoid Turkish locale issues
 * - **URL Component Focus**: Only handles %XX sequences, does NOT convert + to space
 * - **Lossless Operations**: Preserves non-ASCII characters and handles encoding failures gracefully
 */
object UrlCodec {
    
    /**
     * Percent-encode a string for use in URL components.
     * Encodes characters that are unsafe in URLs while preserving safe Unicode characters.
     * 
     * This implementation correctly handles Unicode by encoding the entire string as UTF-8 bytes
     * first, then percent-encoding only unsafe bytes. This ensures proper handling of
     * Unicode surrogate pairs while preserving readable Unicode characters.
     * 
     * ## Encoding Rules
     * 
     * **Safe Characters (not encoded):**
     * - A-Z, a-z, 0-9 (alphanumeric)
     * - Unreserved characters: - . _ ~
     * 
     * **Unsafe Characters (encoded):**
     * - All other ASCII characters (including spaces, special chars)
     * - All non-ASCII bytes from UTF-8 encoding
     * 
     * ## Locale Safety
     * 
     * Uses `Locale.ROOT` for hex formatting to avoid issues with Turkish locale
     * where case operations behave differently.
     * 
     * @param input The string to percent-encode
     * @return The percent-encoded string with unsafe characters as %XX sequences
     */
    fun percentEncodeUtf8(input: String): String {
        val utf8Bytes = input.toByteArray(Charsets.UTF_8)
        val result = StringBuilder(utf8Bytes.size * 3) // Worst case: every byte becomes %XX
        
        for (byte in utf8Bytes) {
            val unsigned = byte.toInt() and 0xFF
            
            when {
                // Unreserved ASCII characters (safe to keep as-is)
                unsigned in 0x41..0x5A || // A-Z
                unsigned in 0x61..0x7A || // a-z  
                unsigned in 0x30..0x39 || // 0-9
                unsigned == 0x2D || unsigned == 0x2E || unsigned == 0x5F || unsigned == 0x7E -> { // -._~
                    result.append(unsigned.toChar())
                }
                // All other bytes (including unsafe ASCII and all non-ASCII) - encode
                else -> {
                    result.append("%${String.format(Locale.ROOT, "%02X", unsigned)}")
                }
            }
        }
        return result.toString()
    }
    
    /**
     * Performs proper percent-decoding for URL components.
     * 
     * Unlike URLDecoder, this only handles %XX percent-encoding and does NOT
     * convert + to space. This is correct for URL components where + should
     * remain as a literal plus character unless the server specifically
     * applies application/x-www-form-urlencoded rules.
     * 
     * This prevents issues with keys like "utm_source+alias" being incorrectly
     * decoded as "utm_source alias".
     * 
     * ## Decoding Algorithm
     * 
     * The improved algorithm only decodes contiguous %XX runs to bytes (for proper
     * multi-byte UTF-8 reassembly) while preserving literal non-ASCII characters
     * without corruption.
     * 
     * **Process:**
     * 1. Scan for %XX sequences
     * 2. Collect contiguous runs of valid %XX sequences
     * 3. Convert hex pairs to bytes
     * 4. Decode byte sequence as UTF-8
     * 5. Preserve invalid sequences and non-ASCII characters as-is
     * 
     * ## Locale Safety
     * 
     * This implementation uses locale-safe operations to avoid issues with Turkish locale
     * where 'i'.toUpperCase() != 'I' and 'I'.toLowerCase() != 'i'. All case operations
     * use `Character.digit()` which is locale-independent for hex digit parsing.
     * 
     * ## Error Handling
     * 
     * - Invalid %XX sequences are preserved as-is (lossless)
     * - Malformed UTF-8 byte sequences are handled by UTF-8 decoder
     * - Non-ASCII characters are preserved without modification
     * 
     * @param input The percent-encoded string to decode
     * @return The decoded string with %XX sequences converted back to characters
     */
    fun percentDecodeUtf8(input: String): String {
        val result = StringBuilder(input.length)
        var i = 0
        val hex = { c: Char -> Character.digit(c, 16) }
        
        while (i < input.length) {
            val c = input[i]
            if (c == '%' && i + 1 < input.length && i + 2 < input.length) {
                val hi = hex(input[i + 1])
                val lo = hex(input[i + 2])
                if (hi >= 0 && lo >= 0) {
                    // Found valid %XX - collect contiguous run for proper UTF-8 decoding
                    val bytes = mutableListOf<Byte>()
                    var j = i
                    while (j + 1 < input.length && j + 2 < input.length && input[j] == '%') {
                        val h = hex(input[j + 1])
                        val l = hex(input[j + 2])
                        if (h >= 0 && l >= 0) {
                            bytes.add(((h shl 4) + l).toByte())
                            j += 3
                        } else {
                            break
                        }
                    }
                    // Decode the byte sequence as UTF-8
                    result.append(String(bytes.toByteArray(), Charsets.UTF_8))
                    i = j
                    continue
                }
            }
            // Not a valid %XX sequence - append character directly (preserves non-ASCII)
            result.append(c)
            i++
        }
        return result.toString()
    }
}
