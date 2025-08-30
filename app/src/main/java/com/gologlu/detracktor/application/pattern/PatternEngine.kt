package com.gologlu.detracktor.application.pattern

/**
 * Interface for pattern compilation and validation engines.
 * Handles the conversion of pattern specifications into executable predicates.
 */
interface PatternEngine {
    /**
     * Validates that a pattern specification is well-formed and can be compiled.
     * 
     * @param spec The pattern specification to validate
     * @return Success if valid, failure with error details if invalid
     */
    fun validate(spec: PatternSpec): Result<Unit>
    
    /**
     * Compiles a pattern specification into an executable predicate function.
     * 
     * @param spec The pattern specification to compile
     * @return A predicate function that tests input strings against the pattern
     * @throws IllegalArgumentException if the pattern specification is invalid
     */
    fun compile(spec: PatternSpec): (String) -> Boolean
}
