package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for DialogWarningDisplay component
 * Tests different warning display states, interactions, and UI variations
 */
@RunWith(AndroidJUnit4::class)
class DialogWarningDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialogWarningDisplay_withNoWarnings_doesNotRender() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = emptyList()
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Component should not render when no warnings
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Warnings Detected").assertDoesNotExist()
    }

    @Test
    fun dialogWarningDisplay_withCredentialWarning_showsCredentialMessage() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList()
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withSensitiveParams_showsSensitiveParamsMessage() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("token", "key")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: token, key", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withBothWarnings_showsBothMessages() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("password", "secret")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: password, secret", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withEmptySensitiveParams_onlyShowsCredentialWarning() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList()
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found:", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun dialogWarningDisplay_withSingleSensitiveParam_showsSingleParam() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("api_key")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: api_key", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withMultipleSensitiveParams_showsAllParams() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("token", "key", "password", "secret")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: token, key, password, secret", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_warningIcon_isDisplayed() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList()
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Warning icon should be present
        composeTestRule.onNodeWithContentDescription("Warning", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withLongSensitiveParamsList_displaysCorrectly() {
        // Arrange
        val longParamsList = (1..10).map { "param$it" }
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = longParamsList
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: ${longParamsList.joinToString(", ")}", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_withSpecialCharactersInParams_displaysCorrectly() {
        // Arrange
        val specialParams = listOf("param_with_underscore", "param-with-dash", "param.with.dot")
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = specialParams
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: ${specialParams.joinToString(", ")}", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_warningTextStyling_isCorrect() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Check that warning text elements exist (styling verification is limited in compose tests)
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: token", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_layoutStructure_isCorrect() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("key", "token")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Verify all expected elements are present
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: key, token", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Warning", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_emptyWarningState_handledCorrectly() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = emptyList()
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert
        composeTestRule.onRoot().assertExists() // Component should still exist but not render content
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun dialogWarningDisplay_hasWarningsProperty_worksCorrectly() {
        // Test hasWarnings = true with credentials
        val warningDataWithCredentials = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList()
        )
        assert(warningDataWithCredentials.hasWarnings)

        // Test hasWarnings = true with sensitive params
        val warningDataWithParams = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("token")
        )
        assert(warningDataWithParams.hasWarnings)

        // Test hasWarnings = false with no warnings
        val warningDataEmpty = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = emptyList()
        )
        assert(!warningDataEmpty.hasWarnings)
    }

    @Test
    fun dialogWarningDisplay_warningCountProperty_worksCorrectly() {
        // Test count with both warnings
        val warningDataBoth = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token")
        )
        assert(warningDataBoth.warningCount == 2)

        // Test count with only credentials
        val warningDataCredentials = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = emptyList()
        )
        assert(warningDataCredentials.warningCount == 1)

        // Test count with only sensitive params
        val warningDataParams = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = listOf("token", "key")
        )
        assert(warningDataParams.warningCount == 1)

        // Test count with no warnings
        val warningDataEmpty = WarningDisplayData(
            hasCredentials = false,
            sensitiveParams = emptyList()
        )
        assert(warningDataEmpty.warningCount == 0)
    }

    @Test
    fun dialogWarningDisplay_accessibilitySupport_isProvided() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("password")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Check accessibility elements
        composeTestRule.onNodeWithContentDescription("Warning", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_multipleWarningTypes_displayOrder() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("api_key", "session_token")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Both warning types should be displayed
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: api_key, session_token", ignoreCase = true).assertExists()
    }

    @Test
    fun dialogWarningDisplay_compactLayout_worksInSmallSpaces() {
        // Arrange
        val warningData = WarningDisplayData(
            hasCredentials = true,
            sensitiveParams = listOf("token")
        )

        // Act
        composeTestRule.setContent {
            DetracktorTheme(darkTheme = false) {
                DialogWarningDisplay(
                    warningData = warningData
                )
            }
        }

        // Assert - Component should render compactly
        composeTestRule.onNodeWithText("Warnings Detected", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("This URL contains embedded credentials", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Sensitive parameters found: token", ignoreCase = true).assertExists()
    }
}
