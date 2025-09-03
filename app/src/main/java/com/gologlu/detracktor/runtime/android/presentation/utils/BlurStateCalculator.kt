package com.gologlu.detracktor.runtime.android.presentation.utils

import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.runtime.android.presentation.types.TokenEffectType

/**
 * Data class representing what elements should be blurred/masked in the current URL
 */
data class BlurState(
    val hasCredentials: Boolean = false,
    val hasNonMatchingParams: Boolean = false,
    val hasSensitiveParams: Boolean = false,
    val hasBlurredElements: Boolean = false
) {
    /**
     * Returns true if there are any elements that should be blurred/masked
     */
    val shouldShowRevealButton: Boolean
        get() = hasCredentials || hasNonMatchingParams || hasSensitiveParams || hasBlurredElements
}

/**
 * Utility class for calculating what elements should be blurred/masked in URL previews
 * and determining when the reveal/hide button should be shown.
 */
object BlurStateCalculator {
    
    /**
     * Calculate the blur state for a given URL and its token effects
     * 
     * @param parts The parsed URL parts
     * @param tokenEffects List of token effects from rule matching
     * @return BlurState indicating what should be blurred/masked
     */
    fun calculateBlurState(
        parts: UrlParts,
        tokenEffects: List<TokenEffect>
    ): BlurState {
        val hasCredentials = parts.userInfo != null
        
        // Check for non-matching parameters (parameters that weren't removed by rules)
        val removedParams = tokenEffects.count { it.willBeRemoved }
        val totalParams = parts.queryPairs.size()
        val hasNonMatchingParams = totalParams > removedParams && totalParams > 0
        
        // Check for sensitive parameters (would need to be determined by warning settings)
        // For now, we'll assume no sensitive params since we don't have that info in TokenEffect
        val hasSensitiveParams = false
        
        // Check if there are any elements that would be blurred
        val hasBlurredElements = hasCredentials || hasNonMatchingParams || hasSensitiveParams
        
        return BlurState(
            hasCredentials = hasCredentials,
            hasNonMatchingParams = hasNonMatchingParams,
            hasSensitiveParams = hasSensitiveParams,
            hasBlurredElements = hasBlurredElements
        )
    }
    
    /**
     * Determine if a specific token should be masked based on its effect and blur settings
     * 
     * @param tokenEffect The effect applied to this token
     * @param blurEnabled Whether blur is currently enabled
     * @return True if the token should be masked
     */
    fun shouldMaskToken(
        tokenEffectType: TokenEffectType,
        blurEnabled: Boolean
    ): Boolean {
        if (!blurEnabled) return false
        
        return when (tokenEffectType) {
            TokenEffectType.CREDENTIALS -> true
            TokenEffectType.SENSITIVE_PARAM -> true
            TokenEffectType.WARNING -> true
            TokenEffectType.NON_MATCHING -> true
            TokenEffectType.REMOVED -> false // Already removed, no need to mask
            TokenEffectType.NONE -> false
        }
    }
    
    /**
     * Generate a mask string for sensitive content
     * 
     * @param originalValue The original value to mask
     * @param maskChar The character to use for masking (default: '•')
     * @param minLength Minimum length of the mask (default: 4)
     * @param maxLength Maximum length of the mask (default: 8)
     * @return Masked string
     */
    fun generateMask(
        originalValue: String,
        maskChar: Char = '•',
        minLength: Int = 4,
        maxLength: Int = 8
    ): String {
        if (originalValue.isEmpty()) return ""
        
        // Use a length based on the original value but within bounds
        val maskLength = originalValue.length.coerceIn(minLength, maxLength)
        return maskChar.toString().repeat(maskLength)
    }
    
    /**
     * Check if the reveal/hide button should be visible for the given blur state
     * 
     * @param blurState The current blur state
     * @return True if the button should be shown
     */
    fun shouldShowRevealButton(blurState: BlurState): Boolean {
        return blurState.shouldShowRevealButton
    }
    
    /**
     * Convert a TokenEffect data class to a TokenEffectType enum for UI purposes
     * 
     * @param tokenEffect The TokenEffect from rule evaluation
     * @param hasCredentials Whether this token contains credentials
     * @param isSensitive Whether this token is marked as sensitive
     * @return The appropriate TokenEffectType for UI display
     */
    fun getTokenEffectType(
        tokenEffect: TokenEffect,
        hasCredentials: Boolean = false,
        isSensitive: Boolean = false
    ): TokenEffectType {
        return when {
            hasCredentials -> TokenEffectType.CREDENTIALS
            isSensitive -> TokenEffectType.SENSITIVE_PARAM
            tokenEffect.willBeRemoved -> TokenEffectType.REMOVED
            // Any token that is NOT removed should be treated as non-matching for UI blur purposes
            else -> TokenEffectType.NON_MATCHING
        }
    }
}
