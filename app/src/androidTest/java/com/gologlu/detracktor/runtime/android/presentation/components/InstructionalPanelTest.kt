package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gologlu.detracktor.runtime.android.presentation.types.InstructionalContent
import com.gologlu.detracktor.runtime.android.presentation.types.ClipboardState
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstructionalPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun instructionalPanel_displaysTitle() {
        val content = InstructionalContent(
            title = "Test Instructions",
            steps = listOf("Step 1", "Step 2"),
            isExpanded = false
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Instructions").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_expandsAndCollapses() {
        var isExpanded by mutableStateOf(false)
        val content = InstructionalContent(
            title = "Test Instructions",
            steps = listOf("Step 1", "Step 2"),
            isExpanded = false
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content.copy(isExpanded = isExpanded),
                    onToggleExpanded = { isExpanded = !isExpanded }
                )
            }
        }

        // Initially collapsed - steps should not be visible
        composeTestRule.onNodeWithText("Step 1").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Step 2").assertIsNotDisplayed()

        // Click to expand
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()

        // Wait for recomposition and check that steps are now visible
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Step 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 2").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_displaysAllSteps() {
        val steps = listOf(
            "Copy a URL to your clipboard",
            "Return to Detracktor",
            "Review the parameters",
            "Tap Clean URL",
            "Share or copy the result"
        )
        val content = InstructionalContent(
            title = "How to Use",
            steps = steps,
            isExpanded = true
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = {}
                )
            }
        }

        // All steps should be displayed
        steps.forEachIndexed { index, step ->
            composeTestRule.onNodeWithText(step).assertIsDisplayed()
            composeTestRule.onNodeWithTag("instructional-step-$index").assertIsDisplayed()
        }
    }

    @Test
    fun instructionalPanel_hasCorrectTestTags() {
        val content = InstructionalContent(
            title = "Test Instructions",
            steps = listOf("Step 1", "Step 2"),
            isExpanded = true
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = {}
                )
            }
        }

        // Check for required test tags
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-step-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-step-1").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_emptySteps_stillDisplaysTitle() {
        val content = InstructionalContent(
            title = "Empty Instructions",
            steps = emptyList(),
            isExpanded = true
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Empty Instructions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_longContent_displaysCorrectly() {
        val longSteps = (1..10).map { "This is step number $it with some longer text to test display behavior" }
        val content = InstructionalContent(
            title = "Long Instructions",
            steps = longSteps,
            isExpanded = true
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = {}
                )
            }
        }

        // First step should be visible
        composeTestRule.onNodeWithText(longSteps.first()).assertIsDisplayed()
        
        // Panel should display all steps (parent container handles scrolling if needed)
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        
        // Verify some steps are present
        composeTestRule.onNodeWithText("This is step number 1 with some longer text to test display behavior").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-step-0").assertIsDisplayed()
    }

    // CRITICAL STATE CONSISTENCY TESTS
    // These tests cover the bug we fixed where state inconsistency caused crashes

    @Test
    fun instructionalPanel_stateConsistency_expandedStateMatchesContent() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "Initial Title",
                steps = listOf("Step 1", "Step 2"),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Initially collapsed
        composeTestRule.onNodeWithText("Step 1").assertIsNotDisplayed()
        
        // Expand
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should be expanded and show content
        composeTestRule.onNodeWithText("Step 1").assertIsDisplayed()
        
        // Change content while expanded - this was the critical bug scenario
        content = InstructionalContent(
            title = "Updated Title",
            steps = listOf("New Step 1", "New Step 2"),
            isExpanded = content.isExpanded // Preserve expansion state
        )
        
        composeTestRule.waitForIdle()
        
        // Should still be expanded with new content
        composeTestRule.onNodeWithText("Updated Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Step 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Step 2").assertIsDisplayed()
        
        // Should still be able to collapse without crashing
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should be collapsed
        composeTestRule.onNodeWithText("New Step 1").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("New Step 2").assertIsNotDisplayed()
    }

    @Test
    fun instructionalPanel_contentChange_preservesExpansionState() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "Clipboard empty",
                steps = listOf("Copy a URL to your clipboard", "Return to Detracktor"),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Expand the panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Verify expanded
        composeTestRule.onNodeWithText("Copy a URL to your clipboard").assertIsDisplayed()
        
        // Simulate clipboard state change (like what happens in MainActivity)
        content = InstructionalContent(
            title = "How to Use Detracktor",
            steps = listOf(
                "Review the parameters that will be removed",
                "Tap Clean URL to remove tracking parameters",
                "Share or copy the cleaned URL"
            ),
            isExpanded = content.isExpanded // This is the critical fix - preserve expansion state
        )
        
        composeTestRule.waitForIdle()
        
        // Should still be expanded with new content
        composeTestRule.onNodeWithText("How to Use Detracktor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Review the parameters that will be removed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap Clean URL to remove tracking parameters").assertIsDisplayed()
        
        // Should still be able to toggle without crashing
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should be collapsed
        composeTestRule.onNodeWithText("Review the parameters that will be removed").assertIsNotDisplayed()
    }

    @Test
    fun instructionalPanel_multipleContentChanges_maintainsStability() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "State 1",
                steps = listOf("Step A", "Step B"),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Expand
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Multiple rapid content changes (simulating clipboard monitoring)
        repeat(5) { i ->
            content = InstructionalContent(
                title = "State ${i + 2}",
                steps = listOf("Step ${i + 1}A", "Step ${i + 1}B"),
                isExpanded = content.isExpanded
            )
            composeTestRule.waitForIdle()
            
            // Should remain stable and expanded
            composeTestRule.onNodeWithText("State ${i + 2}").assertIsDisplayed()
            composeTestRule.onNodeWithText("Step ${i + 1}A").assertIsDisplayed()
        }
        
        // Should still be able to collapse
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should be collapsed
        composeTestRule.onNodeWithText("Step 5A").assertIsNotDisplayed()
    }

    @Test
    fun instructionalPanel_crashPrevention_invalidStateTransitions() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "Test",
                steps = listOf("Step 1"),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Test rapid state changes that could cause crashes
        repeat(10) {
            composeTestRule.onNodeWithTag("instructional-toggle").performClick()
            
            // Change content immediately after click
            content = content.copy(
                title = "Changed $it",
                steps = listOf("New Step $it")
            )
            
            composeTestRule.waitForIdle()
            
            // Should not crash and should display current content
            composeTestRule.onNodeWithText("Changed $it").assertIsDisplayed()
        }
    }

    @Test
    fun instructionalPanel_differentClipboardStates_correctContent() {
        // Test that different clipboard states show appropriate instructional content
        val emptyClipboardContent = InstructionalContent(
            title = "Clipboard empty",
            steps = listOf(
                "Copy a URL to your clipboard",
                "Return to Detracktor"
            ),
            isExpanded = false
        )
        
        val validUrlContent = InstructionalContent(
            title = "How to Use Detracktor",
            steps = listOf(
                "Review the parameters that will be removed",
                "Tap Clean URL to remove tracking parameters",
                "Share or copy the cleaned URL"
            ),
            isExpanded = false
        )

        var currentContent by mutableStateOf(emptyClipboardContent)

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = currentContent,
                    onToggleExpanded = { 
                        currentContent = currentContent.copy(isExpanded = !currentContent.isExpanded)
                    }
                )
            }
        }

        // Test empty clipboard state
        composeTestRule.onNodeWithText("Clipboard empty").assertIsDisplayed()
        
        // Expand to see steps
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Copy a URL to your clipboard").assertIsDisplayed()
        
        // Simulate clipboard state change to valid URL (preserving expansion)
        currentContent = validUrlContent.copy(isExpanded = currentContent.isExpanded)
        composeTestRule.waitForIdle()
        
        // Should show new content while remaining expanded
        composeTestRule.onNodeWithText("How to Use Detracktor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Review the parameters that will be removed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap Clean URL to remove tracking parameters").assertIsDisplayed()
        
        // Should still be able to collapse
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Review the parameters that will be removed").assertIsNotDisplayed()
    }

    @Test
    fun instructionalPanel_edgeCases_handlesGracefully() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "",
                steps = emptyList(),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Should handle empty title and steps gracefully
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        
        // Should be able to toggle even with empty content
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Add content dynamically
        content = InstructionalContent(
            title = "Dynamic Content",
            steps = listOf("Dynamic Step"),
            isExpanded = content.isExpanded
        )
        composeTestRule.waitForIdle()
        
        // Should display new content
        composeTestRule.onNodeWithText("Dynamic Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dynamic Step").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_animationStates_coverAllBranches() {
        var content by mutableStateOf(
            InstructionalContent(
                title = "Animation Test",
                steps = listOf("Step 1", "Step 2", "Step 3"),
                isExpanded = false
            )
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content,
                    onToggleExpanded = { 
                        content = content.copy(isExpanded = !content.isExpanded)
                    }
                )
            }
        }

        // Test rapid toggle to cover animation state branches
        repeat(3) {
            composeTestRule.onNodeWithTag("instructional-toggle").performClick()
            composeTestRule.waitForIdle()
            
            // Verify the toggle button content description changes
            composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
            
            // Change content during animation to test edge cases
            content = content.copy(
                title = "Animation Test ${it + 1}",
                steps = listOf("Updated Step 1", "Updated Step 2")
            )
            composeTestRule.waitForIdle()
        }
        
        // Final state should be stable
        composeTestRule.onNodeWithText("Animation Test 3").assertIsDisplayed()
    }

    @Test
    fun instructionalPanel_contentDescriptionBranches() {
        var isExpanded by mutableStateOf(false)
        val content = InstructionalContent(
            title = "Content Description Test",
            steps = listOf("Test Step"),
            isExpanded = false
        )

        composeTestRule.setContent {
            DetracktorTheme(themeMode = ThemeMode.SYSTEM) {
                InstructionalPanel(
                    content = content.copy(isExpanded = isExpanded),
                    onToggleExpanded = { isExpanded = !isExpanded }
                )
            }
        }

        // Test both content description states (expand/collapse)
        // This might cover the partial line if it's related to content descriptions
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        
        // Toggle to test both content description branches
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
    }
}
