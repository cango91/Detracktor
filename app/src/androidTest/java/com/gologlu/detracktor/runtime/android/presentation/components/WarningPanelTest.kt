package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WarningPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun warningPanel_doesNotDisplayWhenNoWarnings() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = emptyList(),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                WarningPanel(
                    warningData = warningData,
                    onToggleExpanded = { }
                )
            }
        }

        // Then
        composeTestRule.onNodeWithTag("warning-panel").assertIsNotDisplayed()
    }

    @Test
    fun warningPanel_displaysWhenWarningsPresent() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithTag("warning-panel").assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysCorrectWarningCount_singleWarning() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("1 warning detected").assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysCorrectWarningCount_multipleWarnings() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token", "api_key"),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("2 warnings detected").assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysWarningIcon() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true, // Ensure warnings are present
            sensitiveParams = listOf("utm_source"),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                WarningPanel(
                    warningData = warningData,
                    onToggleExpanded = { }
                )
            }
        }

        // Then - First verify the panel exists and has warnings
        assert(warningData.hasWarnings) { "Warning data should have warnings: hasCredentials=${warningData.hasCredentials}, sensitiveParams=${warningData.sensitiveParams}" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("warning-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("warning-header").assertIsDisplayed()
        
        // Debug: Print the semantic tree to see what's actually rendered
        composeTestRule.onRoot().printToLog("WarningPanelTest")
        
        // Try different approaches to find the warning icon
        try {
            composeTestRule.onNodeWithTag("warning-icon").assertIsDisplayed()
        } catch (e: AssertionError) {
            // If test tag doesn't work, try content description
            composeTestRule.onNodeWithContentDescription("Warning").assertIsDisplayed()
        }
    }

    @Test
    fun warningPanel_displaysExpandIcon_collapsed() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then - First verify the panel is displayed, then check for the icon
        composeTestRule.onNodeWithTag("warning-panel").assertIsDisplayed()
        try {
            composeTestRule.onNodeWithTag("warning-expand-icon").assertIsDisplayed()
        } catch (e: AssertionError) {
            // If test tag doesn't work, try content description
            composeTestRule.onNodeWithContentDescription("Expand").assertIsDisplayed()
        }
    }

    @Test
    fun warningPanel_displaysExpandIcon_expanded() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then - First verify the panel is displayed, then check for the icon
        composeTestRule.onNodeWithTag("warning-panel").assertIsDisplayed()
        try {
            composeTestRule.onNodeWithTag("warning-expand-icon").assertIsDisplayed()
        } catch (e: AssertionError) {
            // If test tag doesn't work, try content description
            composeTestRule.onNodeWithContentDescription("Collapse").assertIsDisplayed()
        }
    }

    @Test
    fun warningPanel_contentNotVisibleWhenCollapsed() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("session_id"),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithTag("warning-content").assertIsNotDisplayed()
    }

    @Test
    fun warningPanel_contentVisibleWhenExpanded() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token"),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithTag("warning-content").assertIsDisplayed()
    }

    @Test
    fun warningPanel_headerClickCallsOnToggleExpanded() {
        // Given
        var toggleCalled = false
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = false
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { toggleCalled = true }
            )
        }

        // Click the header
        composeTestRule.onNodeWithTag("warning-header").performClick()

        // Then
        assert(toggleCalled) { "onToggleExpanded should be called when header is clicked" }
    }

    @Test
    fun warningPanel_displaysCredentialsWarning_whenExpanded() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Embedded Credentials").assertIsDisplayed()
        composeTestRule.onNodeWithText("This URL contains embedded username/password credentials that may be visible to others.")
            .assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysSensitiveParamsWarning_whenExpanded() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("utm_source", "fbclid"),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Sensitive Parameters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Found sensitive parameters: utm_source, fbclid")
            .assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysBothWarnings_whenBothPresent() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("api_key", "token"),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Embedded Credentials").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sensitive Parameters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Found sensitive parameters: api_key, token")
            .assertIsDisplayed()
    }

    @Test
    fun warningPanel_displaysWarningItems_withSeverityIndicators() {
        // Given - Use only one warning type to avoid multiple nodes
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(), // Only credentials warning
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then - Should display warning items with severity indicators
        composeTestRule.onNodeWithTag("warning-item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("warning-severity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("warning-title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("warning-description").assertIsDisplayed()
    }

    @Test
    fun warningPanel_handlesEmptySensitiveParams() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then - Should only show credentials warning
        composeTestRule.onNodeWithText("Embedded Credentials").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sensitive Parameters").assertIsNotDisplayed()
    }

    @Test
    fun warningPanel_handlesOnlySensitiveParams() {
        // Given
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("tracking_id"),
            isExpanded = true
        )

        // When
        composeTestRule.setContent {
            WarningPanel(
                warningData = warningData,
                onToggleExpanded = { }
            )
        }

        // Then - Should only show sensitive params warning
        composeTestRule.onNodeWithText("Sensitive Parameters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Embedded Credentials").assertIsNotDisplayed()
    }

    @Test
    fun warningPanel_warningCountReflectsActualWarnings() {
        // Given - Test the warning count calculation
        val credentialsOnly = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList(),
            isExpanded = false
        )
        val sensitiveParamsOnly = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("utm_source"),
            isExpanded = false
        )
        val bothWarnings = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token", "api_key"),
            isExpanded = false
        )

        // Then - Verify warning counts
        assert(credentialsOnly.warningCount == 1) { "Should have 1 warning for credentials only" }
        assert(sensitiveParamsOnly.warningCount == 1) { "Should have 1 warning for sensitive params only" }
        assert(bothWarnings.warningCount == 2) { "Should have 2 warnings for both types" }
    }
}
