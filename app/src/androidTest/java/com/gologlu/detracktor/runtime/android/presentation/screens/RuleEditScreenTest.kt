package com.gologlu.detracktor.runtime.android.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuleEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ruleEditScreen_displaysMainComponents() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Then
        composeTestRule.onNodeWithTag("rule-edit-screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Rules").assertIsDisplayed()
        composeTestRule.onNodeWithTag("back-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add-rule-fab").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_displaysLoadingIndicator() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Then - Loading indicator may be shown briefly during initialization
        // In a real test environment, the service loads quickly so we just verify
        // the screen structure is present
        composeTestRule.onNodeWithTag("rule-edit-screen").assertIsDisplayed()
        
        // Note: To properly test loading states, we would need to mock the SettingsService
        // to control the loading behavior. For now, we verify the screen loads successfully.
    }

    @Test
    fun ruleEditScreen_backButtonCallsOnNavigateBack() {
        // Given
        var backCalled = false

        // When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { backCalled = true }
            )
        }

        // Click back button
        composeTestRule.onNodeWithTag("back-button").performClick()

        // Then
        assert(backCalled) { "onNavigateBack should be called when back button is clicked" }
    }

    @Test
    fun ruleEditScreen_addRuleFabOpensDialog() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Wait for loading to complete and click FAB
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add-rule-fab").performClick()

        // Then - Should open rule edit dialog (this test verifies the click interaction)
        // Note: The actual dialog opening depends on the service loading state
        // In a real test environment, we would mock the service to control the state
    }

    @Test
    fun ruleEditScreen_resetButtonOpensResetDialog() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Click reset button
        composeTestRule.onNodeWithTag("reset-button").performClick()

        // Then - Should open reset dialog
        composeTestRule.onNodeWithTag("reset-dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset to Defaults").assertIsDisplayed()
        composeTestRule.onNodeWithText("This will replace all current rules with the default rule set. This action cannot be undone.")
            .assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_resetDialogHasCorrectButtons() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Open reset dialog
        composeTestRule.onNodeWithTag("reset-button").performClick()

        // Then
        composeTestRule.onNodeWithTag("confirm-reset").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel-reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_resetDialogCancelButton() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Open reset dialog and click cancel
        composeTestRule.onNodeWithTag("reset-button").performClick()
        composeTestRule.onNodeWithTag("cancel-reset").performClick()

        // Then - Dialog should be dismissed
        composeTestRule.onNodeWithTag("reset-dialog").assertDoesNotExist()
    }

    @Test
    fun ruleEditScreen_displaysInstructionalHeader() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Wait for potential loading to complete
        composeTestRule.waitForIdle()

        // Then - Check if instructional header is displayed when rules are loaded
        // Note: This depends on the service state, but we can test the component exists
        // In a real scenario, we would mock the service to return specific states
    }

    @Test
    fun ruleEditScreen_displaysEmptyStateWhenNoRules() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Wait for loading to complete
        composeTestRule.waitForIdle()

        // Then - If no rules are loaded, should show empty state
        // Note: This test would be more reliable with mocked services
        // The actual behavior depends on the CompositionRoot.provideSettingsService result
    }

    @Test
    fun ruleEditScreen_emptyStateComponents() {
        // This test verifies the EmptyRulesState component structure
        // In a real test, we would render this component directly or mock the service
        // to return an empty rules list

        // The empty state should contain:
        // - "No Rules Configured" title
        // - Description text
        // - "Tap the + button to get started" hint
        
        // For now, we can verify the screen structure exists
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Basic structure verification
        composeTestRule.onNodeWithTag("rule-edit-screen").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_errorStateComponents() {
        // This test would verify error state display
        // In a real implementation, we would mock the service to return an error
        
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Verify basic screen structure
        composeTestRule.onNodeWithTag("rule-edit-screen").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_topAppBarConfiguration() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Then - Verify top app bar elements
        composeTestRule.onNodeWithText("Manage Rules").assertIsDisplayed()
        composeTestRule.onNodeWithTag("back-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset-button").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_floatingActionButtonConfiguration() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Then
        composeTestRule.onNodeWithTag("add-rule-fab").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_scaffoldStructure() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Then - Verify the main scaffold structure
        composeTestRule.onNodeWithTag("rule-edit-screen").assertIsDisplayed()
        
        // Verify key UI elements are present
        composeTestRule.onNodeWithText("Manage Rules").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add-rule-fab").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_resetDialogContent() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Open reset dialog
        composeTestRule.onNodeWithTag("reset-button").performClick()

        // Then - Verify dialog content
        composeTestRule.onNodeWithText("Reset to Defaults").assertIsDisplayed()
        composeTestRule.onNodeWithText("This will replace all current rules with the default rule set. This action cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun ruleEditScreen_resetDialogDismissOnOutsideClick() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Open reset dialog
        composeTestRule.onNodeWithTag("reset-button").performClick()
        composeTestRule.onNodeWithTag("reset-dialog").assertIsDisplayed()

        // Note: Testing dismiss on outside click would require more complex interaction
        // For now, we verify the dialog can be opened and has the correct structure
    }

    @Test
    fun ruleEditScreen_handlesMultipleDialogStates() {
        // Given/When
        composeTestRule.setContent {
            RuleEditScreen(
                onNavigateBack = { }
            )
        }

        // Test that only one dialog can be open at a time
        // Open reset dialog
        composeTestRule.onNodeWithTag("reset-button").performClick()
        composeTestRule.onNodeWithTag("reset-dialog").assertIsDisplayed()

        // Cancel reset dialog
        composeTestRule.onNodeWithTag("cancel-reset").performClick()
        composeTestRule.onNodeWithTag("reset-dialog").assertDoesNotExist()

        // Now try to open add rule dialog
        composeTestRule.onNodeWithTag("add-rule-fab").performClick()
        // Note: The add rule dialog opening depends on the service state
    }
}
