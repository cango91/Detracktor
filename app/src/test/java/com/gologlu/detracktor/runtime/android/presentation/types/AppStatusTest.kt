package com.gologlu.detracktor.runtime.android.presentation.types

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppStatus data class
 * Tests data class functionality, equality, and property access
 */
class AppStatusTest {

    @Test
    fun `constructor creates AppStatus with correct properties`() {
        val message = "Test message"
        val clipboardState = ClipboardState.VALID_URL
        val canCleanUrl = true

        val appStatus = AppStatus(
            message = message,
            clipboardState = clipboardState,
            canCleanUrl = canCleanUrl
        )

        assertEquals(message, appStatus.message)
        assertEquals(clipboardState, appStatus.clipboardState)
        assertEquals(canCleanUrl, appStatus.canCleanUrl)
    }

    @Test
    fun `equality works correctly for identical AppStatus objects`() {
        val appStatus1 = AppStatus(
            message = "Ready",
            clipboardState = ClipboardState.VALID_URL,
            canCleanUrl = true
        )

        val appStatus2 = AppStatus(
            message = "Ready",
            clipboardState = ClipboardState.VALID_URL,
            canCleanUrl = true
        )

        assertEquals(appStatus1, appStatus2)
        assertEquals(appStatus1.hashCode(), appStatus2.hashCode())
    }

    @Test
    fun `equality works correctly for different AppStatus objects`() {
        val appStatus1 = AppStatus(
            message = "Ready",
            clipboardState = ClipboardState.VALID_URL,
            canCleanUrl = true
        )

        val appStatus2 = AppStatus(
            message = "Error",
            clipboardState = ClipboardState.EMPTY,
            canCleanUrl = false
        )

        assertNotEquals(appStatus1, appStatus2)
        assertNotEquals(appStatus1.hashCode(), appStatus2.hashCode())
    }

    @Test
    fun `copy function works correctly`() {
        val original = AppStatus(
            message = "Original message",
            clipboardState = ClipboardState.EMPTY,
            canCleanUrl = false
        )

        val copied = original.copy(
            message = "Updated message",
            canCleanUrl = true
        )

        assertEquals("Updated message", copied.message)
        assertEquals(ClipboardState.EMPTY, copied.clipboardState) // Unchanged
        assertEquals(true, copied.canCleanUrl)
        
        // Original should be unchanged
        assertEquals("Original message", original.message)
        assertEquals(false, original.canCleanUrl)
    }

    @Test
    fun `copy function with no parameters creates identical object`() {
        val original = AppStatus(
            message = "Test message",
            clipboardState = ClipboardState.TEXT_NOT_URL,
            canCleanUrl = false
        )

        val copied = original.copy()

        assertEquals(original, copied)
        assertEquals(original.hashCode(), copied.hashCode())
        // Ensure they are different instances
        assertNotSame(original, copied)
    }

    @Test
    fun `toString contains all properties`() {
        val appStatus = AppStatus(
            message = "Status message",
            clipboardState = ClipboardState.NON_TEXT,
            canCleanUrl = true
        )

        val toString = appStatus.toString()

        assertTrue(toString.contains("Status message"))
        assertTrue(toString.contains("NON_TEXT"))
        assertTrue(toString.contains("true"))
    }

    @Test
    fun `all ClipboardState values work correctly`() {
        ClipboardState.values().forEach { state ->
            val appStatus = AppStatus(
                message = "Test for $state",
                clipboardState = state,
                canCleanUrl = state == ClipboardState.VALID_URL
            )

            assertEquals(state, appStatus.clipboardState)
            assertEquals("Test for $state", appStatus.message)
        }
    }

    @Test
    fun `canCleanUrl boolean values work correctly`() {
        val statusTrue = AppStatus(
            message = "Can clean",
            clipboardState = ClipboardState.VALID_URL,
            canCleanUrl = true
        )

        val statusFalse = AppStatus(
            message = "Cannot clean",
            clipboardState = ClipboardState.EMPTY,
            canCleanUrl = false
        )

        assertTrue(statusTrue.canCleanUrl)
        assertFalse(statusFalse.canCleanUrl)
    }

    @Test
    fun `empty and null-like message values work correctly`() {
        val emptyMessage = AppStatus(
            message = "",
            clipboardState = ClipboardState.EMPTY,
            canCleanUrl = false
        )

        val blankMessage = AppStatus(
            message = "   ",
            clipboardState = ClipboardState.EMPTY,
            canCleanUrl = false
        )

        assertEquals("", emptyMessage.message)
        assertEquals("   ", blankMessage.message)
        assertNotEquals(emptyMessage, blankMessage)
    }

    @Test
    fun `component functions work correctly`() {
        val appStatus = AppStatus(
            message = "Component test",
            clipboardState = ClipboardState.VALID_URL,
            canCleanUrl = true
        )

        val (message, clipboardState, canCleanUrl) = appStatus

        assertEquals("Component test", message)
        assertEquals(ClipboardState.VALID_URL, clipboardState)
        assertEquals(true, canCleanUrl)
    }
}
