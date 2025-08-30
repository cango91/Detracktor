package com.gologlu.detracktor.application.pattern

/**
 * Pattern matching kind - defines how the pattern should be interpreted
 */
enum class Kind {
    /** Exact string matching */
    EXACT,
    /** Glob pattern matching with * and ? wildcards */
    GLOB,
    /** Regular expression matching */
    REGEX
}

/**
 * Case sensitivity policy for pattern matching
 */
enum class Case {
    /** Case-sensitive matching */
    SENSITIVE,
    /** Case-insensitive matching */
    INSENSITIVE
}

/**
 * Complete pattern specification combining pattern text with matching policies
 */
data class PatternSpec(
    val raw: String,
    val kind: Kind = Kind.GLOB,
    val case: Case = Case.INSENSITIVE
)
