package com.gologlu.detracktor.application.pattern

/**
 * Interface for discovering pattern engine capabilities.
 * Allows querying what pattern kinds and features are supported by an engine.
 */
interface PatternCapabilities {
    /**
     * Returns the set of pattern kinds supported by this engine.
     */
    fun supportedKinds(): Set<Kind>
    
    /**
     * Returns the set of case sensitivity modes supported by this engine.
     */
    fun supportedCases(): Set<Case>
    
    /**
     * Checks if a specific pattern specification is supported by this engine.
     * 
     * @param spec The pattern specification to check
     * @return true if the specification is supported, false otherwise
     */
    fun supports(spec: PatternSpec): Boolean
}
