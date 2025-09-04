package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.application.types.*
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for RuleEditDialog component
 * Tests dialog rendering, form interactions, validation, and submission flow
 */
@RunWith(AndroidJUnit4::class)
class RuleEditDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleRule(): UrlRule {
        return UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.None
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
    }

    @Test
    fun ruleEditDialog_showsAddTitle_whenRuleIsNull() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Verify add title is shown
        composeTestRule.onNodeWithText("Add New Rule").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-edit-dialog").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_showsEditTitle_whenRuleProvided() {
        var onSaveCalled = false
        var onCancelCalled = false
        val rule = createSampleRule()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = rule,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Verify edit title is shown
        composeTestRule.onNodeWithText("Edit Rule").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-edit-dialog").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_showsFormFields() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Verify form fields are present
        composeTestRule.onNodeWithTag("domains-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("remove-patterns-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("subdomain-mode-dropdown").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_populatesFields_whenEditingExistingRule() {
        var onSaveCalled = false
        var onCancelCalled = false
        val rule = createSampleRule()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = rule,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Verify fields are populated with existing rule data
        composeTestRule.onNodeWithTag("domains-input").assertTextContains("example.com")
        composeTestRule.onNodeWithTag("remove-patterns-input").assertTextContains("utm_*")
    }

    @Test
    fun ruleEditDialog_allowsDomainsInput() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enter domains
        composeTestRule.onNodeWithTag("domains-input").performTextInput("test.com, example.org")

        // Verify input was accepted
        composeTestRule.onNodeWithTag("domains-input").assertTextContains("test.com, example.org")
    }

    @Test
    fun ruleEditDialog_allowsRemovePatternsInput() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enter remove patterns
        composeTestRule.onNodeWithTag("remove-patterns-input").performTextInput("utm_*, gclid, fbclid")

        // Verify input was accepted
        composeTestRule.onNodeWithTag("remove-patterns-input").assertTextContains("utm_*, gclid, fbclid")
    }

    @Test
    fun ruleEditDialog_allowsSubdomainModeSelection() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Click subdomain mode dropdown
        composeTestRule.onNodeWithTag("subdomain-mode-dropdown").performClick()

        // Select "Any" option
        composeTestRule.onNodeWithTag("subdomain-mode-any").performClick()

        // Verify selection was made (dropdown should show "Any")
        composeTestRule.onNodeWithText("Any").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_showsSubdomainsInput_whenSpecificListSelected() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Click subdomain mode dropdown
        composeTestRule.onNodeWithTag("subdomain-mode-dropdown").performClick()

        // Select "Specific List" option
        composeTestRule.onNodeWithTag("subdomain-mode-specific_list").performClick()

        // Verify subdomains input field appears
        composeTestRule.onNodeWithTag("subdomains-input").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_allowsSubdomainsInput_whenSpecificListMode() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Select specific list mode
        composeTestRule.onNodeWithTag("subdomain-mode-dropdown").performClick()
        composeTestRule.onNodeWithTag("subdomain-mode-specific_list").performClick()

        // Enter subdomains
        composeTestRule.onNodeWithTag("subdomains-input").performTextInput("www, api, cdn")

        // Verify input was accepted
        composeTestRule.onNodeWithTag("subdomains-input").assertTextContains("www, api, cdn")
    }

    @Test
    fun ruleEditDialog_showsWarningSettings() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Verify warning settings toggle is present
        composeTestRule.onNodeWithTag("warning-settings-toggle").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_allowsWarningSettingsToggle() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Toggle warning settings on
        composeTestRule.onNodeWithTag("warning-settings-switch").performClick()

        // Wait for UI to update
        composeTestRule.waitForIdle()

        // Scroll to make sure warning options are visible
        composeTestRule.onNodeWithTag("warn-credentials-row").performScrollTo()

        // Verify warning options appear
        composeTestRule.onNodeWithTag("warn-credentials-row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sensitive-params-input").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_allowsSensitiveParamsInput() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enable warning settings
        composeTestRule.onNodeWithTag("warning-settings-switch").performClick()

        // Enter sensitive parameters
        composeTestRule.onNodeWithTag("sensitive-params-input").performTextInput("token, key, password")

        // Verify input was accepted
        composeTestRule.onNodeWithTag("sensitive-params-input").assertTextContains("token, key, password")
    }

    @Test
    fun ruleEditDialog_showsSensitiveMergeOptions() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enable warning settings
        composeTestRule.onNodeWithTag("warning-settings-switch").performClick()

        // Enter some sensitive parameters to make merge options visible
        composeTestRule.onNodeWithTag("sensitive-params-input").performTextInput("token")

        // Scroll to make sure merge options are visible
        composeTestRule.onNodeWithTag("sensitive-merge-row").performScrollTo()

        // Verify merge mode options are present
        composeTestRule.onNodeWithTag("sensitive-merge-row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Union").assertIsDisplayed()
        composeTestRule.onNodeWithText("Replace").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_callsOnCancel_whenCancelClicked() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Click cancel button
        composeTestRule.onNodeWithTag("cancel-rule-button").performClick()

        // Verify onCancel was called
        assert(onCancelCalled)
        assert(!onSaveCalled)
    }

    @Test
    fun ruleEditDialog_saveButtonDisabled_whenFormInvalid() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Save button should be disabled initially (no domains entered)
        composeTestRule.onNodeWithTag("save-rule-button").assertIsNotEnabled()
    }

    @Test
    fun ruleEditDialog_saveButtonEnabled_whenFormValid() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enter valid domains
        composeTestRule.onNodeWithTag("domains-input").performTextInput("example.com")

        // Enter valid remove patterns
        composeTestRule.onNodeWithTag("remove-patterns-input").performTextInput("utm_*")

        // Save button should now be enabled
        composeTestRule.onNodeWithTag("save-rule-button").assertIsEnabled()
    }

    @Test
    fun ruleEditDialog_callsOnSave_whenSaveClicked() {
        var savedRule: UrlRule? = null
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { savedRule = it },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Fill in valid form data
        composeTestRule.onNodeWithTag("domains-input").performTextInput("example.com")
        composeTestRule.onNodeWithTag("remove-patterns-input").performTextInput("utm_*")

        // Click save button
        composeTestRule.onNodeWithTag("save-rule-button").performClick()

        // Verify onSave was called with a rule
        assert(savedRule != null)
        assert(!onCancelCalled)
    }

    @Test
    fun ruleEditDialog_showsValidationErrors_whenFormInvalid() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enter invalid domains (empty)
        composeTestRule.onNodeWithTag("domains-input").performTextInput("")

        // Validation results should appear
        composeTestRule.onNodeWithTag("validation-results").assertExists()
    }

    @Test
    fun ruleEditDialog_hidesSubdomainSection_forCatchAllDomain() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enter catch-all domain
        composeTestRule.onNodeWithTag("domains-input").performTextInput("*")

        // Subdomain mode dropdown should not be visible
        composeTestRule.onNodeWithTag("subdomain-mode-dropdown").assertDoesNotExist()
    }

    @Test
    fun ruleEditDialog_scrollsCorrectly_withLongContent() {
        var onSaveCalled = false
        var onCancelCalled = false

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = null,
                    onSave = { onSaveCalled = true },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Enable warning settings to show more content
        composeTestRule.onNodeWithTag("warning-settings-switch").performClick()

        // Verify we can scroll to see all content
        composeTestRule.onNodeWithTag("sensitive-params-input").performScrollTo()
        composeTestRule.onNodeWithTag("sensitive-params-input").assertIsDisplayed()
    }

    @Test
    fun ruleEditDialog_handlesComplexRuleEditing() {
        var savedRule: UrlRule? = null
        var onCancelCalled = false
        val existingRule = createSampleRule()

        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                RuleEditDialog(
                    rule = existingRule,
                    onSave = { savedRule = it },
                    onCancel = { onCancelCalled = true }
                )
            }
        }

        // Modify the existing rule
        composeTestRule.onNodeWithTag("domains-input").performTextClearance()
        composeTestRule.onNodeWithTag("domains-input").performTextInput("newdomain.com, test.org")

        // Add more remove patterns
        composeTestRule.onNodeWithTag("remove-patterns-input").performTextClearance()
        composeTestRule.onNodeWithTag("remove-patterns-input").performTextInput("utm_*, gclid, fbclid")

        // Enable warnings
        composeTestRule.onNodeWithTag("warning-settings-switch").performClick()
        composeTestRule.onNodeWithTag("sensitive-params-input").performTextInput("token, key")

        // Save the modified rule
        composeTestRule.onNodeWithTag("save-rule-button").performClick()

        // Verify the rule was saved with modifications
        assert(savedRule != null)
        assert(!onCancelCalled)
    }
}
