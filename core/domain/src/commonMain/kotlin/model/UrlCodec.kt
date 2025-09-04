package com.gologlu.detracktor.domain.model

// Platform imports handled by expect/actual implementations

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
        // Use platform-specific encoding implementation
        return PlatformUrlCodec.encode(input)
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
        // Use platform-specific decoding implementation
        return PlatformUrlCodec.decode(input)
    }
}
