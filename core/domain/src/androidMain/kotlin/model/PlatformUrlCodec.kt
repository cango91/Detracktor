package com.gologlu.detracktor.domain.model

import java.util.Locale

actual object PlatformUrlCodec {
    // RFC 3986 unreserved characters that should NOT be encoded
    private val unreservedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toSet()
    
    actual fun decode(encoded: String): String {
        return try {
            // Custom percent decoding that collects bytes and converts to UTF-8 string
            val bytes = mutableListOf<Byte>()
            val result = StringBuilder()
            var i = 0
            
            while (i < encoded.length) {
                val char = encoded[i]
                if (char == '%' && i + 2 < encoded.length) {
                    val hex = encoded.substring(i + 1, i + 3)
                    try {
                        val byte = hex.toInt(16).toByte()
                        bytes.add(byte)
                        i += 3
                    } catch (e: NumberFormatException) {
                        // Not valid hex, treat as literal
                        if (bytes.isNotEmpty()) {
                            result.append(bytes.toByteArray().toString(Charsets.UTF_8))
                            bytes.clear()
                        }
                        result.append(char)
                        i++
                    }
                } else {
                    // Literal character - flush any pending bytes first
                    if (bytes.isNotEmpty()) {
                        result.append(bytes.toByteArray().toString(Charsets.UTF_8))
                        bytes.clear()
                    }
                    result.append(char)
                    i++
                }
            }
            
            // Flush any remaining bytes
            if (bytes.isNotEmpty()) {
                result.append(bytes.toByteArray().toString(Charsets.UTF_8))
            }
            
            result.toString()
        } catch (e: Exception) {
            encoded // Return original if decoding fails
        }
    }
    
    actual fun encode(decoded: String): String {
        return try {
            val result = StringBuilder()
            
            // Process the string by Unicode code points (handles emojis properly)
            var i = 0
            while (i < decoded.length) {
                val codePoint = decoded.codePointAt(i)
                val charString = String(intArrayOf(codePoint), 0, 1)
                
                // Check if this is a single unreserved ASCII character
                if (codePoint < 128 && charString[0] in unreservedChars) {
                    result.append(charString[0])
                } else {
                    // Encode the entire code point (including multi-byte UTF-8 characters)
                    val utf8Bytes = charString.toByteArray(Charsets.UTF_8)
                    for (byte in utf8Bytes) {
                        result.append('%')
                        result.append(String.format(Locale.ROOT, "%02X", byte.toInt() and 0xFF))
                    }
                }
                
                // Move to next code point (might be 1 or 2 UTF-16 characters)
                i += Character.charCount(codePoint)
            }
            
            result.toString()
        } catch (e: Exception) {
            decoded // Return original if encoding fails
        }
    }
    
    actual fun isHexDigit(char: Char): Boolean {
        return Character.isDigit(char) || char in 'A'..'F' || char in 'a'..'f'
    }
    
    actual fun formatHex(byte: Int): String {
        return "%02X".format(Locale.ROOT, byte)
    }
}