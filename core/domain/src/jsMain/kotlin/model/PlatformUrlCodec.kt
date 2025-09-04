package com.gologlu.detracktor.domain.model

actual object PlatformUrlCodec {
    actual fun decode(encoded: String): String {
        return try {
            js("decodeURIComponent").call(null, encoded) as String
        } catch (e: Exception) {
            encoded // Return original if decoding fails
        }
    }
    
    actual fun encode(decoded: String): String {
        return try {
            js("encodeURIComponent").call(null, decoded) as String
        } catch (e: Exception) {
            decoded // Return original if encoding fails
        }
    }
    
    actual fun isHexDigit(char: Char): Boolean {
        return char in '0'..'9' || char in 'A'..'F' || char in 'a'..'f'
    }
    
    actual fun formatHex(byte: Int): String {
        val hex = byte.toString(16).uppercase()
        return if (hex.length == 1) "0$hex" else hex
    }
}