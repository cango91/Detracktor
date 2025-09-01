package com.gologlu.detracktor.application.service.globby

import com.gologlu.detracktor.application.error.AppValidationError
import com.gologlu.detracktor.application.error.AppValidationException

/**
 * Globby: tiny wildcard matcher for *decoded* parameter names.
 *
 * Meta:
 *  - '*'  => any run (including empty)
 *  - '?'  => exactly one character
 *  - '\*' => literal asterisk
 *  - '\?' => literal question mark
 *  - '\\' => literal backslash
 *
 * Everything else is literal. Linear-time with backtracking on '*'.
 */
object Globby {

    /** Match 's' against 'pattern'. Pattern may use escapes as documented above. */
    fun matches(pattern: String, s: String): Boolean {
        var p = 0
        var i = 0
        var star = -1
        var mark = -1
        val pn = pattern.length
        val sn = s.length

        while (i < sn) {
            if (p < pn) {
                val ch = pattern[p]
                when (ch) {
                    '\\' -> {
                        // Escaped literal: next char must exist and match exactly
                        if (p + 1 >= pn) return false // invalid trailing escape (should be caught by validate)
                        val lit = pattern[p + 1]
                        if (s[i] == lit) { 
                            p += 2
                            i++
                            continue 
                        }
                        // backtrack if we had a previous '*' to try consuming one more input char
                        if (star != -1) { 
                            p = star + 1
                            i = ++mark
                            continue 
                        }
                        return false
                    }
                    '?' -> { 
                        p++
                        i++
                        continue 
                    }
                    '*' -> { 
                        star = p
                        mark = i
                        p++
                        continue 
                    }
                    else -> {
                        if (s[i] == ch) { 
                            p++
                            i++
                            continue 
                        }
                        if (star != -1) { 
                            p = star + 1
                            i = ++mark
                            continue 
                        }
                        return false
                    }
                }
            } else {
                // pattern consumed but input remains; can only succeed if we can backtrack on a previous '*'
                if (star != -1) { 
                    p = star + 1
                    i = ++mark
                    continue 
                }
                return false
            }
        }

        // Consume trailing '*' and ensure no dangling escapes remain
        while (p < pn) {
            val ch = pattern[p]
            if (ch == '*') { 
                p++
                continue 
            }
            if (ch == '\\') {
                // an escaped literal left in pattern cannot match empty input
                if (p + 1 >= pn) return false // invalid trailing escape (should be caught by validate)
                return false
            }
            // any other literal cannot match empty
            return false
        }
        return true
    }

    /**
     * Validate a Globby pattern intended for *parameter names*.
     * Throws AppValidationException on error.
     */
    fun requireValid(pattern: String, fieldPath: String) {
        if (pattern.isEmpty())
            throw AppValidationException(AppValidationError("Pattern must not be empty.", fieldPath))
        if (pattern.length > 256)
            throw AppValidationException(AppValidationError("Pattern too long (max 256).", fieldPath))

        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            when (ch) {
                '\n' -> throw AppValidationException(AppValidationError("Newline not allowed.", fieldPath))
                '\r' -> throw AppValidationException(AppValidationError("Carriage return not allowed.", fieldPath))
                '\t' -> throw AppValidationException(AppValidationError("Tab not allowed.", fieldPath))
                in '\u0000'..'\u001F', '\u007F' ->
                    throw AppValidationException(AppValidationError("Control character at index $i.", fieldPath))
                '\\' -> {
                    if (i + 1 >= pattern.length)
                        throw AppValidationException(AppValidationError("Trailing backslash escape.", fieldPath))
                    // allow escaping any char; no whitelist needed
                    i += 2
                    continue
                }
                else -> { /* ok */ }
            }
            i++
        }
    }
}
