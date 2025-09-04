package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.application.types.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for RuleListItem component
 * Tests list item rendering, interactions, and different rule display variations
 */
@RunWith(AndroidJUnit4::class)
class RuleListItemTest {

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

    private fun createRuleWithWarnings(): UrlRule {
        return UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("test.com")),
                    subdomains = Subdomains.Any
                ),
                schemes = listOf("https", "http")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*"), Pattern("gclid")),
                warn = WarningSettings(
                    warnOnEmbeddedCredentials = true,
                    sensitiveParams = listOf("token", "key")
                )
            )
        )
    }

    @Test
    fun ruleListItem_displaysBasicRuleInfo() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify basic rule information is displayed
        composeTestRule.onNodeWithTag("rule-item-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-title-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rule 1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule-description-0").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysHostInformation() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify host information is displayed
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysRemovePatterns() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify remove patterns are displayed
        composeTestRule.onNodeWithTag("rule-removes-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("utm_*").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysSchemes_whenPresent() {
        val rule = createRuleWithWarnings()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify schemes are displayed
        composeTestRule.onNodeWithTag("rule-schemes-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("HTTPS, HTTP").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysWarningSettings_whenPresent() {
        val rule = createRuleWithWarnings()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify warning settings are displayed
        composeTestRule.onNodeWithTag("rule-warnings-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("credentials, sensitive params: token, key").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_showsEditButton() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify edit button is present
        composeTestRule.onNodeWithTag("edit-rule-0").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit rule").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_showsDeleteButton() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify delete button is present
        composeTestRule.onNodeWithTag("delete-rule-0").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete rule").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_callsOnEdit_whenEditClicked() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click edit button
        composeTestRule.onNodeWithTag("edit-rule-0").performClick()

        // Verify onEdit was called
        assert(onEditCalled)
        assert(!onDeleteCalled)
    }

    @Test
    fun ruleListItem_showsDeleteDialog_whenDeleteClicked() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click delete button
        composeTestRule.onNodeWithTag("delete-rule-0").performClick()

        // Verify delete confirmation dialog appears
        composeTestRule.onNodeWithTag("delete-dialog-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Rule").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this rule? This action cannot be undone.").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_callsOnDelete_whenDeleteConfirmed() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click delete button
        composeTestRule.onNodeWithTag("delete-rule-0").performClick()

        // Confirm deletion
        composeTestRule.onNodeWithTag("confirm-delete-0").performClick()

        // Verify onDelete was called
        assert(onDeleteCalled)
        assert(!onEditCalled)
    }

    @Test
    fun ruleListItem_dismissesDeleteDialog_whenCancelClicked() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click delete button
        composeTestRule.onNodeWithTag("delete-rule-0").performClick()

        // Cancel deletion
        composeTestRule.onNodeWithTag("cancel-delete-0").performClick()

        // Verify dialog is dismissed and onDelete was not called
        composeTestRule.onNodeWithTag("delete-dialog-0").assertDoesNotExist()
        assert(!onDeleteCalled)
        assert(!onEditCalled)
    }

    @Test
    fun ruleListItem_displaysMultipleDomains() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com", "test.org", "sample.net")),
                    subdomains = Subdomains.None
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify multiple domains are displayed
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("example.com, test.org, sample.net").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysSubdomainPrefix_whenAnySubdomains() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.Any
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify subdomain prefix is displayed
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("*.example.com").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysSpecificSubdomains() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.OneOf(listOf("www", "api"))
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify specific subdomains are displayed
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("www|api.example.com").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysCatchAllDomain() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.Any,
                    subdomains = null
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify catch-all domain is displayed
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("All domains (*)").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_displaysMultipleRemovePatterns() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.None
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*"), Pattern("gclid"), Pattern("fbclid")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify multiple remove patterns are displayed
        composeTestRule.onNodeWithTag("rule-removes-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("utm_*, gclid, fbclid").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_handlesLongDomainList() {
        val longDomainList = (1..10).map { "domain$it.com" }
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(longDomainList),
                    subdomains = Subdomains.None
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify long domain list is handled (should show truncation)
        composeTestRule.onNodeWithTag("rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithText("domain1.com, domain2.com, +8 more").assertIsDisplayed()
    }

    @Test
    fun ruleListItem_handlesEmptyRemovePatterns() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.None
                ),
                schemes = listOf("https")
            ),
            then = ThenBlock(
                remove = emptyList(),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify empty remove patterns don't show removes section
        composeTestRule.onNodeWithTag("rule-removes-0").assertDoesNotExist()
    }

    @Test
    fun ruleListItem_handlesNoSchemes() {
        val rule = UrlRule(
            when_ = WhenBlock(
                host = HostCond(
                    domains = Domains.ListOf(listOf("example.com")),
                    subdomains = Subdomains.None
                ),
                schemes = null
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            )
        )
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify no schemes section when schemes is null
        composeTestRule.onNodeWithTag("rule-schemes-0").assertDoesNotExist()
    }

    @Test
    fun ruleListItem_handlesNoWarnings() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            RuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify no warnings section when warn is null
        composeTestRule.onNodeWithTag("rule-warnings-0").assertDoesNotExist()
    }

    @Test
    fun compactRuleListItem_displaysBasicInfo() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            CompactRuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Verify compact display
        composeTestRule.onNodeWithTag("compact-rule-item-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-rule-description-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-rule-host-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-edit-rule-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact-delete-rule-0").assertIsDisplayed()
    }

    @Test
    fun compactRuleListItem_callsOnEdit_whenEditClicked() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            CompactRuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click edit button
        composeTestRule.onNodeWithTag("compact-edit-rule-0").performClick()

        // Verify onEdit was called
        assert(onEditCalled)
        assert(!onDeleteCalled)
    }

    @Test
    fun compactRuleListItem_callsOnDelete_whenDeleteClicked() {
        val rule = createSampleRule()
        var onEditCalled = false
        var onDeleteCalled = false

        composeTestRule.setContent {
            CompactRuleListItem(
                rule = rule,
                index = 0,
                onEdit = { onEditCalled = true },
                onDelete = { onDeleteCalled = true }
            )
        }

        // Click delete button
        composeTestRule.onNodeWithTag("compact-delete-rule-0").performClick()

        // Verify onDelete was called
        assert(onDeleteCalled)
        assert(!onEditCalled)
    }
}
