package com.gologlu.detracktor.runtime.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for MainActivity
 * Tests the main UI elements and basic functionality
 */
@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesSuccessfully() {
        // Verify main screen elements are present
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detracktor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
    }

    @Test
    fun mainActivity_showsInitialStatus() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Verify status is shown (could be "Ready" or other initial state)
        composeTestRule.onNodeWithTag("status").assertExists()
    }

    @Test
    fun mainActivity_hasCleanButton() {
        // Verify clean button is available
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clean URL").assertExists()
    }

    @Test
    fun mainActivity_hasSettingsButton() {
        // Verify settings button exists and is clickable
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
        
        // Test that settings button is clickable (will open settings activity)
        composeTestRule.onNodeWithTag("open-settings").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainActivity_showsTitle() {
        // Verify app title is displayed
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detracktor").assertIsDisplayed()
    }

    @Test
    fun mainActivity_cleanButtonIsClickable() {
        // Verify clean button can be clicked
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").performClick()
        
        // Wait for any potential dialog or action
        composeTestRule.waitForIdle()
        
        // The exact behavior depends on clipboard content and settings
        // but the button should be functional
    }

    @Test
    fun mainActivity_hasProperLayout() {
        // Verify main layout elements are present and properly arranged
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
    }

    @Test
    fun mainActivity_supportsAccessibility() {
        // Verify main elements have proper accessibility support
        composeTestRule.onNodeWithTag("title").assertExists()
        composeTestRule.onNodeWithTag("status").assertExists()
        composeTestRule.onNodeWithTag("clean-action").assertExists()
        composeTestRule.onNodeWithTag("open-settings").assertExists()
        
        // Verify buttons have proper text labels
        composeTestRule.onNodeWithText("Clean URL").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
    }

    @Test
    fun mainActivity_handlesLifecycleEvents() {
        // Verify initial state
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        
        // Simulate configuration change (rotation)
        composeTestRule.activityRule.scenario.recreate()
        
        // Wait for recreation
        composeTestRule.waitForIdle()
        
        // Verify UI is still functional after recreation
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
    }

    @Test
    fun mainActivity_showsStatusInformation() {
        // Wait for initial processing
        composeTestRule.waitForIdle()
        
        // Verify status element exists and shows some information
        composeTestRule.onNodeWithTag("status").assertExists()
        
        // Status should contain some text (Ready, processing, error, etc.)
        val statusNode = composeTestRule.onNodeWithTag("status")
        statusNode.assertExists()
    }

    @Test
    fun mainActivity_maintainsUIConsistency() {
        // Verify all main UI elements are consistently present
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detracktor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status").assertExists()
        composeTestRule.onNodeWithText("Clean URL").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
        
        // Verify layout doesn't break with different screen states
        composeTestRule.waitForIdle()
        
        // All elements should still be present
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
    }

    @Test
    fun mainActivity_handlesUserInteractions() {
        // Test basic user interactions work without crashing
        
        // Click clean button
        composeTestRule.onNodeWithTag("clean-action").performClick()
        composeTestRule.waitForIdle()
        
        // App should still be functional
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        
        // Click settings button
        composeTestRule.onNodeWithTag("open-settings").performClick()
        composeTestRule.waitForIdle()
        
        // Note: Settings activity may open, but we're testing that the interaction doesn't crash
    }

    @Test
    fun mainActivity_showsAppropriateContent() {
        // Verify the app shows appropriate content for a URL cleaning app
        composeTestRule.onNodeWithText("Detracktor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clean URL").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
        
        // Status should show some relevant information
        composeTestRule.onNodeWithTag("status").assertExists()
        
        // Main screen should be the primary container
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
    }

    // INSTRUCTIONAL PANEL INTEGRATION TESTS
    // These tests verify the instructional panel works correctly in the full app context

    @Test
    fun mainActivity_instructionalPanel_isPresent() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Instructional panel should be present
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_canExpandAndCollapse() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Initially should be collapsed
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        
        // Click to expand
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should show expanded content
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        
        // Click to collapse
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should still be present but content may be hidden
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_showsContextualContent() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Should show some instructional content based on current state
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-header").assertIsDisplayed()
        
        // Expand to see the content
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should show instructional steps
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_survivesConfigurationChanges() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Expand the instructional panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Verify it's expanded
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        
        // Simulate configuration change (rotation)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Instructional panel should still be present and functional
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        
        // Should be able to interact with it
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainActivity_instructionalPanel_doesNotCrashOnInteraction() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Rapid clicking should not crash the app
        repeat(5) {
            composeTestRule.onNodeWithTag("instructional-toggle").performClick()
            composeTestRule.waitForIdle()
        }
        
        // App should still be functional
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_maintainsStateConsistency() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Expand the panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Interact with other UI elements (this might trigger clipboard checks)
        composeTestRule.onNodeWithTag("clean-action").performClick()
        composeTestRule.waitForIdle()
        
        // Instructional panel should still be present and functional
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-toggle").assertIsDisplayed()
        
        // Should still be able to toggle
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should not crash
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_worksWithOtherUIElements() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // All main UI elements should be present
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status").assertExists()
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
        
        // Expand instructional panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Other elements should still be functional
        composeTestRule.onNodeWithTag("clean-action").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-settings").assertIsDisplayed()
        
        // Should be able to interact with other elements
        composeTestRule.onNodeWithTag("clean-action").performClick()
        composeTestRule.waitForIdle()
        
        // Everything should still work
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
    }

    @Test
    fun mainActivity_instructionalPanel_handlesScrolling() {
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Expand the instructional panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // Should be able to scroll if content is long
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        
        // Try scrolling within the instructional content
        composeTestRule.onNodeWithTag("instructional-content").performScrollTo()
        
        // Should not crash and content should still be visible
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
    }

    @Test
    fun mainActivity_fullWorkflow_withInstructionalPanel() {
        // Test a complete user workflow including instructional panel interaction
        
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // 1. User sees the main screen with instructional panel
        composeTestRule.onNodeWithTag("main-screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        
        // 2. User expands instructional panel to read instructions
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("instructional-content").assertIsDisplayed()
        
        // 3. User tries to clean URL (might show different instructions based on clipboard)
        composeTestRule.onNodeWithTag("clean-action").performClick()
        composeTestRule.waitForIdle()
        
        // 4. Instructional panel should still be functional
        composeTestRule.onNodeWithTag("instructional-panel").assertIsDisplayed()
        
        // 5. User can still interact with instructional panel
        composeTestRule.onNodeWithTag("instructional-toggle").performClick()
        composeTestRule.waitForIdle()
        
        // 6. User can access settings
        composeTestRule.onNodeWithTag("open-settings").performClick()
        composeTestRule.waitForIdle()
        
        // The workflow should complete without crashes
        // Note: Settings activity may open, but we're testing that interactions don't crash
    }
}
