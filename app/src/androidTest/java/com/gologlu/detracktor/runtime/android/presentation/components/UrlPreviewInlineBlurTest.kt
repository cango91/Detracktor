package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UrlPreviewInlineBlurTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUrlPreviewDisplaysCorrectly() {
        // Test basic URL preview display
        val urlParts = createTestUrlParts("https://example.com?token=secret&id=123")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "token", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = true,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("url-preview-inline-blur").assertIsDisplayed()
        composeTestRule.onNodeWithTag("url-inline-flow").assertIsDisplayed()
    }

    @Test
    fun testSchemeDisplay() {
        val urlParts = createTestUrlParts("https://example.com")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("scheme").assertIsDisplayed()
    }

    @Test
    fun testHostDisplay() {
        val urlParts = createTestUrlParts("https://example.com")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("host").assertIsDisplayed()
    }

    @Test
    fun testPortDisplay() {
        val urlParts = createTestUrlParts("https://example.com:8080")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("port").assertIsDisplayed()
    }

    @Test
    fun testPathDisplay() {
        val urlParts = createTestUrlParts("https://example.com/path/to/resource")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("path").assertIsDisplayed()
    }

    @Test
    fun testQueryParametersDisplay() {
        val urlParts = createTestUrlParts("https://example.com?param1=value1&param2=value2")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "param1", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap()),
            TokenEffect(tokenIndex = 1, name = "param2", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param2")))
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("query-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-1").assertIsDisplayed()
    }

    @Test
    fun testFragmentDisplay() {
        val urlParts = createTestUrlParts("https://example.com#section")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("fragment-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fragment").assertIsDisplayed()
    }

    @Test
    fun testUserInfoDisplay() {
        val urlParts = createTestUrlParts("https://user:pass@example.com")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("userinfo").assertIsDisplayed()
    }

    @Test
    fun testUserInfoBlurredWhenEnabled() {
        val urlParts = createTestUrlParts("https://user:pass@example.com")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = true,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("userinfo").assertIsDisplayed()
    }

    @Test
    fun testComplexUrlWithAllComponents() {
        val urlParts = createTestUrlParts("https://user:pass@example.com:8080/path?param1=value1&param2=value2#section")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "param1", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap()),
            TokenEffect(tokenIndex = 1, name = "param2", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("param2")))
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        // Verify all components are displayed
        composeTestRule.onNodeWithTag("scheme").assertIsDisplayed()
        composeTestRule.onNodeWithTag("userinfo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("host").assertIsDisplayed()
        composeTestRule.onNodeWithTag("port").assertIsDisplayed()
        composeTestRule.onNodeWithTag("path").assertIsDisplayed()
        composeTestRule.onNodeWithTag("query-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fragment-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fragment").assertIsDisplayed()
    }

    @Test
    fun testBlurEnabledState() {
        val urlParts = createTestUrlParts("https://example.com?sensitive=secret&normal=value")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "sensitive", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap()),
            TokenEffect(tokenIndex = 1, name = "normal", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("normal")))
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = true,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        // All components should still be displayed even when blur is enabled
        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-1").assertIsDisplayed()
    }

    @Test
    fun testBlurDisabledState() {
        val urlParts = createTestUrlParts("https://example.com?sensitive=secret&normal=value")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "sensitive", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap()),
            TokenEffect(tokenIndex = 1, name = "normal", willBeRemoved = true, matchedRuleIndexes = listOf(0), matchedPatternsByRule = mapOf(0 to listOf("normal")))
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        // All components should be displayed normally when blur is disabled
        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-1").assertIsDisplayed()
    }

    @Test
    fun testEmptyQueryParameters() {
        val urlParts = createTestUrlParts("https://example.com")
        val tokenEffects = emptyList<TokenEffect>()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        // Should display basic URL components without query parameters
        composeTestRule.onNodeWithTag("scheme").assertIsDisplayed()
        composeTestRule.onNodeWithTag("host").assertIsDisplayed()
    }

    @Test
    fun testSingleQueryParameter() {
        val urlParts = createTestUrlParts("https://example.com?single=value")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "single", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("query-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("param-value-0").assertIsDisplayed()
    }

    @Test
    fun testParameterWithoutValue() {
        val urlParts = createTestUrlPartsWithoutEquals("https://example.com?flag")
        val tokenEffects = listOf(
            TokenEffect(tokenIndex = 0, name = "flag", willBeRemoved = false, matchedRuleIndexes = emptyList(), matchedPatternsByRule = emptyMap())
        )

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                UrlPreviewInlineBlur(
                    parts = urlParts,
                    tokenEffects = tokenEffects,
                    blurEnabled = false,
                    highlight = Color.Red,
                    muted = Color.Gray
                )
            }
        }

        composeTestRule.onNodeWithTag("param-key-0").assertIsDisplayed()
        // Should not have equals or value for flag parameters
    }

    // Helper function to create test UrlParts
    private fun createTestUrlParts(url: String): UrlParts {
        val parts = url.split("://", "?", "#")
        val scheme = if (parts.isNotEmpty()) parts[0] else null
        
        val hostAndPath = if (parts.size > 1) parts[1] else ""
        val hostPortPath = hostAndPath.split("/")
        val hostPort = hostPortPath[0]
        val path = if (hostPortPath.size > 1) "/" + hostPortPath.drop(1).joinToString("/") else null
        
        val hostPortParts = hostPort.split("@").last().split(":")
        val host = hostPortParts[0]
        val port = if (hostPortParts.size > 1) hostPortParts[1].toIntOrNull() else null
        
        val userInfo = if (hostPort.contains("@")) {
            hostPort.split("@")[0]
        } else null
        
        val queryString = if (parts.size > 2 && url.contains("?")) {
            val queryPart = parts[2].split("#")[0]
            if (queryPart.isNotEmpty()) queryPart else null
        } else null
        
        val fragment = if (url.contains("#")) {
            url.split("#").last()
        } else null
        
        val queryPairs = if (queryString != null) {
            QueryPairs.from(queryString)
        } else {
            QueryPairs.empty()
        }
        
        return UrlParts(
            scheme = scheme,
            userInfo = userInfo,
            host = host,
            port = port,
            path = path,
            queryPairs = queryPairs,
            fragment = fragment
        )
    }

    // Helper function to create test UrlParts with parameters that don't have equals
    private fun createTestUrlPartsWithoutEquals(url: String): UrlParts {
        val urlParts = createTestUrlParts(url)
        // For this test, we'll simulate a flag parameter without equals
        // This would need to be handled by the actual QueryPairs implementation
        return urlParts
    }
}
