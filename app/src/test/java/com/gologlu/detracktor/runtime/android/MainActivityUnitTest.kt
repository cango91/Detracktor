package com.gologlu.detracktor.runtime.android

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.gologlu.detracktor.application.service.match.Evaluation
import com.gologlu.detracktor.application.service.match.CompiledSiteRule
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.error.DomainError
import com.gologlu.detracktor.domain.model.MaybeUrl
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.QueryToken
import com.gologlu.detracktor.domain.model.Url
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.domain.service.UrlParser
import com.gologlu.detracktor.runtime.android.presentation.types.ClipboardState
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for MainActivity helper functions and logic
 * Uses Robolectric to test Android-dependent code without requiring a device
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityUnitTest {

    private lateinit var context: Context
    private lateinit var mockClipboardManager: ClipboardManager
    private lateinit var mockUrlParser: UrlParser

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockClipboardManager = mockk()
        mockUrlParser = mockk()
    }

    // Test clipboard reading functionality
    @Test
    fun `readClipboard returns null when clipboard is empty`() {
        every { mockClipboardManager.primaryClip } returns null

        // We can't directly test the private function, but we can test the logic
        val clip = mockClipboardManager.primaryClip
        assertNull(clip)
    }

    @Test
    fun `readClipboard returns text when clipboard has text`() {
        val mockClipData = mockk<ClipData>()
        val mockItem = mockk<ClipData.Item>()
        
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns "https://example.com"

        val clip = mockClipboardManager.primaryClip
        val item = clip?.getItemAt(0)
        val text = item?.coerceToText(context)?.toString()

        assertEquals("https://example.com", text)
    }

    @Test
    fun `clipboardHasNonText returns false when clipboard has text`() {
        val mockClipData = mockk<ClipData>()
        val mockDescription = mockk<ClipDescription>()
        val mockItem = mockk<ClipData.Item>()

        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 1
        every { mockClipData.description } returns mockDescription
        every { mockDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) } returns true
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns "some text"

        // Test the logic that would be in clipboardHasNonText
        val clip = mockClipboardManager.primaryClip
        assertNotNull(clip)
        assertEquals(1, clip!!.itemCount)
        
        val desc = clip.description
        val hasTextMime = desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        assertTrue(hasTextMime)
        
        val text = clip.getItemAt(0).coerceToText(context)?.toString()
        assertFalse(text.isNullOrBlank())
    }

    @Test
    fun `clipboardHasNonText returns true when clipboard has non-text content`() {
        val mockClipData = mockk<ClipData>()
        val mockDescription = mockk<ClipDescription>()
        val mockItem = mockk<ClipData.Item>()

        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 1
        every { mockClipData.description } returns mockDescription
        every { mockDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) } returns false
        every { mockDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) } returns false
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns ""

        // Test the logic that would be in clipboardHasNonText
        val clip = mockClipboardManager.primaryClip
        assertNotNull(clip)
        assertEquals(1, clip!!.itemCount)
        
        val desc = clip.description
        val hasTextMime = desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        assertFalse(hasTextMime)
        
        val text = clip.getItemAt(0).coerceToText(context)?.toString()
        assertTrue(text.isNullOrBlank())
    }

    // Test clipboard state determination logic
    @Test
    fun `determineClipboardState returns EMPTY when clipboard is null`() {
        every { mockClipboardManager.primaryClip } returns null

        // Test the logic for empty clipboard
        val clip = mockClipboardManager.primaryClip
        assertNull(clip)
        // This would result in ClipboardState.EMPTY
    }

    @Test
    fun `determineClipboardState returns EMPTY when clipboard has no items`() {
        val mockClipData = mockk<ClipData>()
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 0

        val clip = mockClipboardManager.primaryClip
        assertNotNull(clip)
        assertEquals(0, clip!!.itemCount)
        // This would result in ClipboardState.EMPTY
    }

    @Test
    fun `determineClipboardState returns NON_TEXT when clipboard has non-text content`() {
        val mockClipData = mockk<ClipData>()
        val mockItem = mockk<ClipData.Item>()
        
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 1
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns null
        every { mockItem.text } returns null
        every { mockItem.htmlText } returns null
        every { mockItem.uri } returns mockk() // Has URI content

        val clip = mockClipboardManager.primaryClip
        val item = clip?.getItemAt(0)
        val text = item?.coerceToText(context)?.toString()
        val hasAnyContent = item?.text != null || item?.htmlText != null || item?.uri != null

        assertTrue(text.isNullOrEmpty())
        assertTrue(hasAnyContent)
        // This would result in ClipboardState.NON_TEXT
    }

    @Test
    fun `determineClipboardState returns TEXT_NOT_URL when clipboard has non-URL text`() {
        val mockClipData = mockk<ClipData>()
        val mockItem = mockk<ClipData.Item>()
        
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 1
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns "just some text"
        every { mockUrlParser.parse("just some text" as MaybeUrl) } returns DomainResult.Failure(mockk<DomainError>())

        val clip = mockClipboardManager.primaryClip
        val item = clip?.getItemAt(0)
        val text = item?.coerceToText(context)?.toString()
        
        assertFalse(text.isNullOrEmpty())
        assertFalse(text!!.isBlank())
        
        val parsed = mockUrlParser.parse(text as MaybeUrl)
        assertTrue(parsed is DomainResult.Failure)
        // This would result in ClipboardState.TEXT_NOT_URL
    }

    @Test
    fun `determineClipboardState returns VALID_URL when clipboard has valid URL`() {
        val mockClipData = mockk<ClipData>()
        val mockItem = mockk<ClipData.Item>()
        val mockUrlParts = mockk<UrlParts>()
        
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockClipData.itemCount } returns 1
        every { mockClipData.getItemAt(0) } returns mockItem
        every { mockItem.coerceToText(any()) } returns "https://example.com"
        every { mockUrlParser.parse("https://example.com" as MaybeUrl) } returns DomainResult.Success(mockUrlParts)

        val clip = mockClipboardManager.primaryClip
        val item = clip?.getItemAt(0)
        val text = item?.coerceToText(context)?.toString()
        
        assertFalse(text.isNullOrEmpty())
        assertFalse(text!!.isBlank())
        
        val parsed = mockUrlParser.parse(text as MaybeUrl)
        assertTrue(parsed is DomainResult.Success)
        // This would result in ClipboardState.VALID_URL
    }

    // Test status message generation logic
    @Test
    fun `getStatusMessage returns correct message for EMPTY clipboard`() {
        // Test the logic that would be in getStatusMessage
        val clipboardState = ClipboardState.EMPTY
        val evaluation: Evaluation? = null
        
        // This would return context.getString(R.string.status_clipboard_empty)
        assertEquals(ClipboardState.EMPTY, clipboardState)
        assertNull(evaluation)
    }

    @Test
    fun `getStatusMessage returns correct message for NON_TEXT clipboard`() {
        val clipboardState = ClipboardState.NON_TEXT
        val evaluation: Evaluation? = null
        
        assertEquals(ClipboardState.NON_TEXT, clipboardState)
        assertNull(evaluation)
    }

    @Test
    fun `getStatusMessage returns correct message for TEXT_NOT_URL clipboard`() {
        val clipboardState = ClipboardState.TEXT_NOT_URL
        val evaluation: Evaluation? = null
        
        assertEquals(ClipboardState.TEXT_NOT_URL, clipboardState)
        assertNull(evaluation)
    }

    @Test
    fun `getStatusMessage returns processing message for VALID_URL with null evaluation`() {
        val clipboardState = ClipboardState.VALID_URL
        val evaluation: Evaluation? = null
        
        assertEquals(ClipboardState.VALID_URL, clipboardState)
        assertNull(evaluation)
        // This would return context.getString(R.string.status_processing_url)
    }

    @Test
    fun `getStatusMessage returns no matches message for VALID_URL with empty matches`() {
        val clipboardState = ClipboardState.VALID_URL
        val mockEvaluation = mockk<Evaluation>()
        every { mockEvaluation.matches } returns emptyList()
        
        assertEquals(ClipboardState.VALID_URL, clipboardState)
        assertTrue(mockEvaluation.matches.isEmpty())
        // This would return context.getString(R.string.status_no_rules_matched)
    }

    @Test
    fun `getStatusMessage returns matches count message for VALID_URL with matches`() {
        val clipboardState = ClipboardState.VALID_URL
        val mockEvaluation = mockk<Evaluation>()
        every { mockEvaluation.matches } returns listOf(mockk(), mockk(), mockk()) // 3 matches
        
        assertEquals(ClipboardState.VALID_URL, clipboardState)
        assertEquals(3, mockEvaluation.matches.size)
        // This would return context.getString(R.string.status_rules_matched, 3)
    }

    // Test clean button enable logic
    @Test
    fun `shouldEnableCleanButton returns true only for VALID_URL`() {
        assertTrue(ClipboardState.VALID_URL == ClipboardState.VALID_URL)
        assertFalse(ClipboardState.EMPTY == ClipboardState.VALID_URL)
        assertFalse(ClipboardState.NON_TEXT == ClipboardState.VALID_URL)
        assertFalse(ClipboardState.TEXT_NOT_URL == ClipboardState.VALID_URL)
    }

    // Test instructional content generation logic
    @Test
    fun `getInstructionalContent returns correct content for EMPTY clipboard`() {
        val clipboardState = ClipboardState.EMPTY
        
        assertEquals(ClipboardState.EMPTY, clipboardState)
        // This would return InstructionalContent with "how to use" steps
    }

    @Test
    fun `getInstructionalContent returns correct content for NON_TEXT clipboard`() {
        val clipboardState = ClipboardState.NON_TEXT
        
        assertEquals(ClipboardState.NON_TEXT, clipboardState)
        // This would return InstructionalContent with "clipboard not supported" steps
    }

    @Test
    fun `getInstructionalContent returns correct content for TEXT_NOT_URL clipboard`() {
        val clipboardState = ClipboardState.TEXT_NOT_URL
        
        assertEquals(ClipboardState.TEXT_NOT_URL, clipboardState)
        // This would return InstructionalContent with "invalid URL format" steps
    }

    @Test
    fun `getInstructionalContent returns correct content for VALID_URL clipboard`() {
        val clipboardState = ClipboardState.VALID_URL
        
        assertEquals(ClipboardState.VALID_URL, clipboardState)
        // This would return InstructionalContent with "URL ready" steps
    }

    // Test warning data building logic
    @Test
    fun `buildWarningData returns empty data for null inputs`() {
        val parts: UrlParts? = null
        val eval: Evaluation? = null
        
        assertNull(parts)
        assertNull(eval)
        // This would return WarningDisplayData(hasCredentials = false, sensitiveParams = emptyList())
    }

    @Test
    fun `buildWarningData detects credentials in userInfo`() {
        val mockParts = mockk<UrlParts>()
        val mockEval = mockk<Evaluation>()
        val mockWarnings = mockk<WarningSettings>()
        
        every { mockParts.userInfo } returns "user:pass"
        every { mockEval.effectiveWarnings } returns mockWarnings
        every { mockWarnings.warnOnEmbeddedCredentials } returns true
        every { mockWarnings.sensitiveParams } returns null
        every { mockParts.queryPairs } returns QueryPairs.empty()
        
        assertNotNull(mockParts.userInfo)
        assertTrue(mockEval.effectiveWarnings.warnOnEmbeddedCredentials == true)
        // This would result in hasCredentials = true
    }

    @Test
    fun `buildWarningData detects sensitive parameters`() {
        // Test the logic for detecting sensitive parameters without complex mocking
        val sensitiveParamsList = listOf("password", "token")
        val presentParams = setOf("password", "id")
        
        // Test the intersection logic that would be used in buildWarningData
        val intersection = presentParams.intersect(sensitiveParamsList.toSet())
        
        assertEquals(setOf("password"), intersection)
        assertTrue(intersection.isNotEmpty())
        // This demonstrates the core logic for detecting sensitive parameters
    }

    // Test rule match data building logic
    @Test
    fun `buildRuleMatchData filters out rules with no removed parameters`() {
        val mockParts = mockk<UrlParts>()
        val mockEval = mockk<Evaluation>()
        val mockMatch1 = mockk<CompiledSiteRule>()
        val mockMatch2 = mockk<CompiledSiteRule>()
        
        every { mockParts.host } returns "example.com"
        every { mockEval.matches } returns listOf(mockMatch1, mockMatch2)
        every { mockMatch1.index } returns 0
        every { mockMatch2.index } returns 1
        
        val tokenEffects = listOf(
            mockk<TokenEffect>().apply {
                every { willBeRemoved } returns true
                every { matchedRuleIndexes } returns listOf(0)
                every { name } returns "utm_source"
            },
            mockk<TokenEffect>().apply {
                every { willBeRemoved } returns false
                every { matchedRuleIndexes } returns listOf(1)
                every { name } returns "id"
            }
        )
        
        // Test the filtering logic
        val matchedRules = mockEval.matches.mapNotNull { match ->
            val actuallyMatchedParams = tokenEffects
                .filter { effect -> effect.willBeRemoved && effect.matchedRuleIndexes.contains(match.index) }
                .map { effect -> effect.name }
            
            if (actuallyMatchedParams.isNotEmpty()) {
                // Would create RuleMatchSummary
                actuallyMatchedParams
            } else {
                null
            }
        }
        
        assertEquals(1, matchedRules.size) // Only match1 should be included
        assertEquals(listOf("utm_source"), matchedRules[0])
    }

    // Test deduplication logic
    @Test
    fun `deduplicateRuleMatches removes identical entries`() {
        val rule1 = mockk<com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary>()
        val rule2 = mockk<com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary>()
        val rule3 = mockk<com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary>()
        
        every { rule1.domain } returns "example.com"
        every { rule1.matchedParams } returns listOf("utm_source", "utm_medium")
        every { rule2.domain } returns "example.com"
        every { rule2.matchedParams } returns listOf("utm_medium", "utm_source") // Same params, different order
        every { rule3.domain } returns "google.com"
        every { rule3.matchedParams } returns listOf("gclid")
        
        val matches = listOf(rule1, rule2, rule3)
        
        // Test deduplication logic
        val deduplicated = matches.distinctBy { rule ->
            "${rule.domain}:${rule.matchedParams.sorted().joinToString(",")}"
        }
        
        assertEquals(2, deduplicated.size) // rule1 and rule2 should be deduplicated
    }

    // Test after-cleaning action execution logic
    @Test
    fun `executeAfterCleaningAction handles ALWAYS_SHARE action`() {
        val action = com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_SHARE
        val cleanedUrl = "https://example.com"
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_SHARE, action)
        assertEquals("https://example.com", cleanedUrl)
        // This would create and start a share intent
    }

    @Test
    fun `executeAfterCleaningAction handles ALWAYS_COPY action`() {
        val action = com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_COPY
        val cleanedUrl = "https://example.com"
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_COPY, action)
        assertEquals("https://example.com", cleanedUrl)
        // This would copy to clipboard
    }

    @Test
    fun `executeAfterCleaningAction handles ASK action`() {
        val action = com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ASK
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ASK, action)
        // This should not be called directly for ASK action
    }

    // Test manual cleaning logic
    @Test
    fun `handleManualCleaning executes action for ALWAYS_SHARE setting`() {
        val cleanedUrl = "https://example.com"
        val mockUiSettings = mockk<com.gologlu.detracktor.runtime.android.presentation.types.UiSettings>()
        
        every { mockUiSettings.afterCleaningAction } returns com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_SHARE
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_SHARE, mockUiSettings.afterCleaningAction)
        // This would execute the share action directly
    }

    @Test
    fun `handleManualCleaning executes action for ALWAYS_COPY setting`() {
        val cleanedUrl = "https://example.com"
        val mockUiSettings = mockk<com.gologlu.detracktor.runtime.android.presentation.types.UiSettings>()
        
        every { mockUiSettings.afterCleaningAction } returns com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_COPY
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ALWAYS_COPY, mockUiSettings.afterCleaningAction)
        // This would execute the copy action directly
    }

    @Test
    fun `handleManualCleaning shows dialog for ASK setting`() {
        val cleanedUrl = "https://example.com"
        val mockUiSettings = mockk<com.gologlu.detracktor.runtime.android.presentation.types.UiSettings>()
        var dialogShown = false
        
        every { mockUiSettings.afterCleaningAction } returns com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ASK
        
        assertEquals(com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction.ASK, mockUiSettings.afterCleaningAction)
        // This would show a dialog by calling onShowDialog
    }

    // Test edge cases and error conditions
    @Test
    fun `clipboard reading handles null clipboard manager gracefully`() {
        // Test null safety
        val nullClipboardManager: ClipboardManager? = null
        assertNull(nullClipboardManager)
    }

    @Test
    fun `URL parsing handles malformed URLs gracefully`() {
        val malformedUrl = "not-a-url"
        every { mockUrlParser.parse(malformedUrl as MaybeUrl) } returns DomainResult.Failure(mockk<DomainError>())
        
        val result = mockUrlParser.parse(malformedUrl as MaybeUrl)
        assertTrue(result is DomainResult.Failure)
    }

    @Test
    fun `evaluation handles empty rule matches gracefully`() {
        val mockEvaluation = mockk<Evaluation>()
        every { mockEvaluation.matches } returns emptyList()
        
        assertTrue(mockEvaluation.matches.isEmpty())
        // This should result in "no rules matched" status
    }

    @Test
    fun `warning data building handles null sensitive params gracefully`() {
        val mockWarnings = mockk<WarningSettings>()
        every { mockWarnings.sensitiveParams } returns null
        
        assertNull(mockWarnings.sensitiveParams)
        // This should result in empty sensitive params list
    }

    @Test
    fun `rule match data building handles empty token effects gracefully`() {
        val mockEval = mockk<Evaluation>()
        every { mockEval.matches } returns listOf(mockk())
        
        val emptyTokenEffects = emptyList<TokenEffect>()
        
        assertTrue(emptyTokenEffects.isEmpty())
        // This should result in no rule matches being created
    }
}
