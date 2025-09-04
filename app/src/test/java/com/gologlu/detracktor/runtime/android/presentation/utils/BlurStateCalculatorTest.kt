package com.gologlu.detracktor.runtime.android.presentation.utils

import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.runtime.android.presentation.types.TokenEffectType
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for BlurStateCalculator utility class
 * Tests blur state calculation, token masking logic, and UI display decisions
 */
class BlurStateCalculatorTest {

    @Test
    fun `calculateBlurState - no credentials, no params, no effects`() {
        val parts = createUrlParts(userInfo = null, queryParams = emptyList())
        val tokenEffects = emptyList<TokenEffect>()
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertEquals(BlurState(), result)
        assertFalse(result.shouldShowRevealButton)
    }

    @Test
    fun `calculateBlurState - with credentials`() {
        val parts = createUrlParts(userInfo = "user:pass", queryParams = emptyList())
        val tokenEffects = emptyList<TokenEffect>()
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertEquals(
            BlurState(
                hasCredentials = true,
                hasNonMatchingParams = false,
                hasSensitiveParams = false,
                hasBlurredElements = true
            ),
            result
        )
        assertTrue(result.shouldShowRevealButton)
    }

    @Test
    fun `calculateBlurState - with non-matching parameters`() {
        val parts = createUrlParts(
            userInfo = null,
            queryParams = listOf("param1" to "value1", "param2" to "value2", "param3" to "value3")
        )
        // Only 2 out of 3 parameters will be removed
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "param1", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param1"))),
            TokenEffect(tokenIndex = 1, name = "param2", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param2"))),
            TokenEffect(tokenIndex = 2, name = "param3", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertEquals(
            BlurState(
                hasCredentials = false,
                hasNonMatchingParams = true,
                hasSensitiveParams = false,
                hasBlurredElements = true
            ),
            result
        )
        assertTrue(result.shouldShowRevealButton)
    }

    @Test
    fun `calculateBlurState - all parameters removed`() {
        val parts = createUrlParts(
            userInfo = null,
            queryParams = listOf("param1" to "value1", "param2" to "value2")
        )
        // All parameters will be removed
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "param1", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param1"))),
            TokenEffect(tokenIndex = 1, name = "param2", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param2")))
        )
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertEquals(
            BlurState(
                hasCredentials = false,
                hasNonMatchingParams = false,
                hasSensitiveParams = false,
                hasBlurredElements = false
            ),
            result
        )
        assertFalse(result.shouldShowRevealButton)
    }

    @Test
    fun `calculateBlurState - complex scenario with credentials and non-matching params`() {
        val parts = createUrlParts(
            userInfo = "admin:secret",
            queryParams = listOf("utm_source" to "google", "session_id" to "abc123", "debug" to "true")
        )
        // Only utm_source will be removed
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "utm_source", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("utm_*"))),
            TokenEffect(tokenIndex = 1, name = "session_id", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap()),
            TokenEffect(tokenIndex = 2, name = "debug", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertEquals(
            BlurState(
                hasCredentials = true,
                hasNonMatchingParams = true,
                hasSensitiveParams = false,
                hasBlurredElements = true
            ),
            result
        )
        assertTrue(result.shouldShowRevealButton)
    }

    @Test
    fun `shouldMaskToken - blur disabled`() {
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.CREDENTIALS, false))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.SENSITIVE_PARAM, false))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.WARNING, false))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.NON_MATCHING, false))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.REMOVED, false))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.NONE, false))
    }

    @Test
    fun `shouldMaskToken - blur enabled with different token types`() {
        assertTrue(BlurStateCalculator.shouldMaskToken(TokenEffectType.CREDENTIALS, true))
        assertTrue(BlurStateCalculator.shouldMaskToken(TokenEffectType.SENSITIVE_PARAM, true))
        assertTrue(BlurStateCalculator.shouldMaskToken(TokenEffectType.WARNING, true))
        assertTrue(BlurStateCalculator.shouldMaskToken(TokenEffectType.NON_MATCHING, true))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.REMOVED, true))
        assertFalse(BlurStateCalculator.shouldMaskToken(TokenEffectType.NONE, true))
    }

    @Test
    fun `generateMask - empty string`() {
        assertEquals("", BlurStateCalculator.generateMask(""))
    }

    @Test
    fun `generateMask - short string uses minimum length`() {
        val result = BlurStateCalculator.generateMask("ab")
        assertEquals("••••", result)
        assertEquals(4, result.length)
    }

    @Test
    fun `generateMask - medium string uses actual length`() {
        val result = BlurStateCalculator.generateMask("medium")
        assertEquals("••••••", result)
        assertEquals(6, result.length)
    }

    @Test
    fun `generateMask - long string uses maximum length`() {
        val result = BlurStateCalculator.generateMask("verylongpassword123")
        assertEquals("••••••••", result)
        assertEquals(8, result.length)
    }

    @Test
    fun `generateMask - custom parameters`() {
        val result = BlurStateCalculator.generateMask(
            originalValue = "test",
            maskChar = '*',
            minLength = 2,
            maxLength = 6
        )
        assertEquals("****", result)
    }

    @Test
    fun `generateMask - custom parameters with long string`() {
        val result = BlurStateCalculator.generateMask(
            originalValue = "verylongstring",
            maskChar = '#',
            minLength = 3,
            maxLength = 5
        )
        assertEquals("#####", result)
        assertEquals(5, result.length)
    }

    @Test
    fun `shouldShowRevealButton - delegates to BlurState property`() {
        val blurStateWithElements = BlurState(hasBlurredElements = true)
        assertTrue(BlurStateCalculator.shouldShowRevealButton(blurStateWithElements))
        
        val blurStateWithCredentials = BlurState(hasCredentials = true)
        assertTrue(BlurStateCalculator.shouldShowRevealButton(blurStateWithCredentials))
        
        val blurStateWithNonMatching = BlurState(hasNonMatchingParams = true)
        assertTrue(BlurStateCalculator.shouldShowRevealButton(blurStateWithNonMatching))
        
        val blurStateWithSensitive = BlurState(hasSensitiveParams = true)
        assertTrue(BlurStateCalculator.shouldShowRevealButton(blurStateWithSensitive))
        
        val emptyBlurState = BlurState()
        assertFalse(BlurStateCalculator.shouldShowRevealButton(emptyBlurState))
    }

    @Test
    fun `getTokenEffectType - credentials take priority`() {
        val tokenEffect = TokenEffect(tokenIndex = 0, name = "user", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("user")))
        
        val result = BlurStateCalculator.getTokenEffectType(
            tokenEffect = tokenEffect,
            hasCredentials = true,
            isSensitive = true
        )
        
        assertEquals(TokenEffectType.CREDENTIALS, result)
    }

    @Test
    fun `getTokenEffectType - sensitive params take priority over removal`() {
        val tokenEffect = TokenEffect(tokenIndex = 0, name = "password", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("password")))
        
        val result = BlurStateCalculator.getTokenEffectType(
            tokenEffect = tokenEffect,
            hasCredentials = false,
            isSensitive = true
        )
        
        assertEquals(TokenEffectType.SENSITIVE_PARAM, result)
    }

    @Test
    fun `getTokenEffectType - removed token`() {
        val tokenEffect = TokenEffect(tokenIndex = 0, name = "utm_source", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("utm_*")))
        
        val result = BlurStateCalculator.getTokenEffectType(
            tokenEffect = tokenEffect,
            hasCredentials = false,
            isSensitive = false
        )
        
        assertEquals(TokenEffectType.REMOVED, result)
    }

    @Test
    fun `getTokenEffectType - non-matching token`() {
        val tokenEffect = TokenEffect(tokenIndex = 0, name = "custom_param", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        
        val result = BlurStateCalculator.getTokenEffectType(
            tokenEffect = tokenEffect,
            hasCredentials = false,
            isSensitive = false
        )
        
        assertEquals(TokenEffectType.NON_MATCHING, result)
    }

    @Test
    fun `BlurState shouldShowRevealButton property - comprehensive test`() {
        assertFalse(BlurState().shouldShowRevealButton)
        assertTrue(BlurState(hasCredentials = true).shouldShowRevealButton)
        assertTrue(BlurState(hasNonMatchingParams = true).shouldShowRevealButton)
        assertTrue(BlurState(hasSensitiveParams = true).shouldShowRevealButton)
        assertTrue(BlurState(hasBlurredElements = true).shouldShowRevealButton)
        
        // Multiple flags
        assertTrue(
            BlurState(
                hasCredentials = true,
                hasNonMatchingParams = true,
                hasSensitiveParams = true,
                hasBlurredElements = true
            ).shouldShowRevealButton
        )
    }

    // Edge cases and error conditions

    @Test
    fun `calculateBlurState - no query parameters`() {
        val parts = createUrlParts(userInfo = null, queryParams = emptyList())
        val tokenEffects = emptyList<TokenEffect>()
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        assertFalse(result.hasNonMatchingParams)
        assertFalse(result.shouldShowRevealButton)
    }

    @Test
    fun `calculateBlurState - mismatched token effects count`() {
        val parts = createUrlParts(
            userInfo = null,
            queryParams = listOf("param1" to "value1", "param2" to "value2")
        )
        // More token effects than parameters - should handle gracefully
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "param1", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param1"))),
            TokenEffect(tokenIndex = 1, name = "param2", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param2"))),
            TokenEffect(tokenIndex = 2, name = "param3", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )
        
        val result = BlurStateCalculator.calculateBlurState(parts, tokenEffects)
        
        // Should still calculate correctly based on actual parameters
        assertFalse(result.hasNonMatchingParams) // All 2 params removed by first 2 effects
    }

    @Test
    fun `generateMask - boundary conditions`() {
        // Test minimum length boundary
        assertEquals("•", BlurStateCalculator.generateMask("x", minLength = 1, maxLength = 10))
        
        // Test maximum length boundary  
        assertEquals("••••••••••", BlurStateCalculator.generateMask("verylongstring", minLength = 1, maxLength = 10))
        
        // Test when min > max (should still work)
        assertEquals("•••••", BlurStateCalculator.generateMask("test", minLength = 5, maxLength = 3))
    }

    // Helper methods

    private fun createUrlParts(
        userInfo: String?,
        queryParams: List<Pair<String, String>>
    ): UrlParts {
        val queryPairs = QueryPairs.of(*queryParams.toTypedArray())
        
        return UrlParts(
            scheme = "https",
            userInfo = userInfo,
            host = "example.com",
            port = null,
            path = "/path",
            queryPairs = queryPairs,
            fragment = null
        )
    }
}
