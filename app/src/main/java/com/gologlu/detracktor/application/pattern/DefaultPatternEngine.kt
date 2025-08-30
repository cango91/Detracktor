package com.gologlu.detracktor.application.pattern

import java.util.Locale
import java.util.regex.Pattern as JavaPattern
import java.util.regex.PatternSyntaxException

/**
 * Default implementation of PatternEngine that supports EXACT, GLOB, and REGEX pattern matching
 * with both case-sensitive and case-insensitive modes.
 */
class DefaultPatternEngine : PatternEngine, PatternCapabilities {
    
    override fun validate(spec: PatternSpec): Result<Unit> {
        return try {
            when (spec.kind) {
                Kind.EXACT -> {
                    // Exact patterns are always valid
                    Result.success(Unit)
                }
                Kind.GLOB -> {
                    // Validate glob pattern by converting to regex
                    globToRegex(spec.raw)
                    Result.success(Unit)
                }
                Kind.REGEX -> {
                    // Validate regex pattern by compiling it
                    val flags = if (spec.case == Case.INSENSITIVE) {
                        JavaPattern.CASE_INSENSITIVE
                    } else {
                        0
                    }
                    JavaPattern.compile(spec.raw, flags)
                    Result.success(Unit)
                }
            }
        } catch (e: PatternSyntaxException) {
            Result.failure(IllegalArgumentException("Invalid ${spec.kind.name.lowercase()} pattern: ${spec.raw}", e))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Error validating pattern: ${spec.raw}", e))
        }
    }
    
    override fun compile(spec: PatternSpec): (String) -> Boolean {
        // Validate the pattern first and throw IllegalArgumentException if invalid
        val validationResult = validate(spec)
        if (validationResult.isFailure) {
            throw validationResult.exceptionOrNull() as IllegalArgumentException
        }
        
        return when (spec.kind) {
            Kind.EXACT -> compileExact(spec)
            Kind.GLOB -> compileGlob(spec)
            Kind.REGEX -> compileRegex(spec)
        }
    }
    
    private fun compileExact(spec: PatternSpec): (String) -> Boolean {
        return { input ->
            when (spec.case) {
                Case.SENSITIVE -> input == spec.raw
                Case.INSENSITIVE -> input.equals(spec.raw, ignoreCase = true)
            }
        }
    }
    
    private fun compileGlob(spec: PatternSpec): (String) -> Boolean {
        val regexPattern = globToRegex(spec.raw)
        val flags = if (spec.case == Case.INSENSITIVE) {
            JavaPattern.CASE_INSENSITIVE
        } else {
            0
        }
        val compiledPattern = JavaPattern.compile(regexPattern, flags)
        
        return { input ->
            compiledPattern.matcher(input).matches()
        }
    }
    
    private fun compileRegex(spec: PatternSpec): (String) -> Boolean {
        val flags = if (spec.case == Case.INSENSITIVE) {
            JavaPattern.CASE_INSENSITIVE
        } else {
            0
        }
        val compiledPattern = JavaPattern.compile(spec.raw, flags)
        
        return { input ->
            compiledPattern.matcher(input).matches()
        }
    }
    
    /**
     * Converts a glob pattern to a regular expression.
     * Supports * (match any characters) and ? (match single character).
     */
    private fun globToRegex(glob: String): String {
        val regex = StringBuilder()
        var i = 0
        
        while (i < glob.length) {
            when (val char = glob[i]) {
                '*' -> regex.append(".*")
                '?' -> regex.append(".")
                // Escape regex special characters
                '.', '^', '$', '+', '{', '}', '[', ']', '|', '(', ')', '\\' -> {
                    regex.append("\\").append(char)
                }
                else -> regex.append(char)
            }
            i++
        }
        
        return regex.toString()
    }
    
    // PatternCapabilities implementation
    override fun supportedKinds(): Set<Kind> {
        return setOf(Kind.EXACT, Kind.GLOB, Kind.REGEX)
    }
    
    override fun supportedCases(): Set<Case> {
        return setOf(Case.SENSITIVE, Case.INSENSITIVE)
    }
    
    override fun supports(spec: PatternSpec): Boolean {
        return spec.kind in supportedKinds() && spec.case in supportedCases()
    }
}
