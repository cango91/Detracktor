package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RulesSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rulesSummary_doesNotDisplayWhenNoRules() {
        // Given
        val matchedRules = emptyList<RuleMatchSummary>()

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("rules-summary").assertIsNotDisplayed()
    }

    @Test
    fun rulesSummary_displaysWhenRulesPresent() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Remove tracking parameters",
                matchedParams = listOf("utm_source"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("rules-summary").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysCorrectRuleCount_singleRule() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Remove Facebook tracking",
                matchedParams = listOf("fbclid"),
                domain = "facebook.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("1 rule matched").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysCorrectRuleCount_multipleRules() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Remove UTM parameters",
                matchedParams = listOf("utm_source", "utm_medium"),
                domain = "example.com"
            ),
            RuleMatchSummary(
                description = "Remove Google tracking",
                matchedParams = listOf("gclid"),
                domain = "google.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("2 rules matched").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysRuleHeader() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = listOf("test_param"),
                domain = "test.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("rules-header").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysRuleBulletItems() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "First rule",
                matchedParams = listOf("param1"),
                domain = "first.com"
            ),
            RuleMatchSummary(
                description = "Second rule",
                matchedParams = listOf("param2"),
                domain = "second.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("rule-item-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-item-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-bullet-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-bullet-1").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysRuleDomains() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = listOf("test_param"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-domain-0").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_displaysRuleDescriptions() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Remove tracking parameters",
                matchedParams = listOf("utm_source"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("Remove tracking parameters").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-description-0").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_handlesEmptyDescription() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "",
                matchedParams = listOf("test_param"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-description-0").assertIsNotDisplayed()
    }

    @Test
    fun rulesSummary_displaysMatchedParameters() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = listOf("utm_source", "utm_medium"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("matched-label-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matched:").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_source").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_medium").assertIsDisplayed()
    }

    @Test
    fun rulesSummary_handlesEmptyMatchedParameters() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = emptyList(),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Matched:").assertIsNotDisplayed()
    }

    @Test
    fun rulesSummary_displaysMultipleMatchedParametersInRows() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule with many params",
                matchedParams = listOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            RulesSummary(matchedRules = matchedRules)
        }

        // Then - Should display parameters in multiple rows (max 3 per row)
        composeTestRule.onNodeWithTag("matched-row-0-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-row-0-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_source").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_medium").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_campaign").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_term").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matched-param-utm_content").assertIsDisplayed()
    }

    @Test
    fun compactRulesSummary_doesNotDisplayWhenNoRules() {
        // Given
        val matchedRules = emptyList<RuleMatchSummary>()

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("compact-rules-summary").assertIsNotDisplayed()
    }

    @Test
    fun compactRulesSummary_displaysWhenRulesPresent() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = listOf("test_param"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("compact-rules-summary").assertIsDisplayed()
    }

    @Test
    fun compactRulesSummary_displaysCorrectElements() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Test rule",
                matchedParams = listOf("param1", "param2"),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("compact-bullet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-text").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 rule matched").assertIsDisplayed()
    }

    @Test
    fun compactRulesSummary_displaysMatchedCount() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "First rule",
                matchedParams = listOf("param1", "param2"),
                domain = "first.com"
            ),
            RuleMatchSummary(
                description = "Second rule",
                matchedParams = listOf("param3"),
                domain = "second.com"
            )
        )

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithTag("compact-matched-count").assertIsDisplayed()
        composeTestRule.onNodeWithText("(3 matched)").assertIsDisplayed()
    }

    @Test
    fun compactRulesSummary_handlesZeroMatchedParams() {
        // Given
        val matchedRules = listOf(
            RuleMatchSummary(
                description = "Rule with no matches",
                matchedParams = emptyList(),
                domain = "example.com"
            )
        )

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = matchedRules)
        }

        // Then
        composeTestRule.onNodeWithText("1 rule matched").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-matched-count").assertIsNotDisplayed()
    }

    @Test
    fun compactRulesSummary_displaysCorrectPluralForm() {
        // Given
        val multipleRules = listOf(
            RuleMatchSummary("Rule 1", listOf("param1"), "domain1.com"),
            RuleMatchSummary("Rule 2", listOf("param2"), "domain2.com")
        )

        // When
        composeTestRule.setContent {
            CompactRulesSummary(matchedRules = multipleRules)
        }

        // Then
        composeTestRule.onNodeWithText("2 rules matched").assertIsDisplayed()
    }
}
