package com.gologlu.detracktor.data

/**
 * Pattern types for flexible URL matching strategies.
 */
enum class PatternType {
    EXACT,          // Exact string match
    WILDCARD,       // Simple wildcard (*.domain.com)
    REGEX,          // Full regex pattern
    PATH_PATTERN    // URL path pattern matching
}
