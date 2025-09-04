package com.gologlu.detracktor.domain.model

/**
 * Platform-specific URL encoding/decoding operations.
 * This abstracts away the Java-specific APIs to make the code multiplatform.
 */
expect object PlatformUrlCodec {
    /**
     * Decodes a percent-encoded string.
     * @param encoded The percent-encoded string (e.g., "Hello%20World")
     * @return The decoded string (e.g., "Hello World")
     */
    fun decode(encoded: String): String
    
    /**
     * Encodes a string using percent encoding.
     * @param decoded The string to encode
     * @return The percent-encoded string
     */
    fun encode(decoded: String): String
    
    /**
     * Checks if a character is a hex digit.
     * @param char The character to check
     * @return true if it's a hex digit (0-9, A-F, a-f)
     */
    fun isHexDigit(char: Char): Boolean
    
    /**
     * Formats a byte as a hex string (uppercase).
     * @param byte The byte value (0-255)
     * @return The hex string (e.g., "4A")
     */
    fun formatHex(byte: Int): String
}