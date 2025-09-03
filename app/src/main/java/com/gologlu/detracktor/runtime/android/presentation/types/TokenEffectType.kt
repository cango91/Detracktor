package com.gologlu.detracktor.runtime.android.presentation.types

/**
 * Enum representing the different types of effects that can be applied to URL tokens
 * for UI display purposes (blurring, masking, highlighting, etc.)
 */
enum class TokenEffectType {
    /** No special effect - token is displayed normally */
    NONE,
    
    /** Token will be removed by rules - typically highlighted */
    REMOVED,
    
    /** Token contains sensitive parameters that should be masked */
    SENSITIVE_PARAM,
    
    /** Token triggers a warning */
    WARNING,
    
    /** Token contains credentials that should be masked */
    CREDENTIALS,
    
    /** Token doesn't match any removal patterns but should be highlighted */
    NON_MATCHING
}
