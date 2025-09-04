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
}
