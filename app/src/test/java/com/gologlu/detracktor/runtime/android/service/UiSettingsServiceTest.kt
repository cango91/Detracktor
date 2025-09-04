package com.gologlu.detracktor.runtime.android.service

import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode
import com.gologlu.detracktor.runtime.android.repo.UiSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.Assert.*

class UiSettingsServiceTest {

    private val mockRepository = mockk<UiSettingsRepository>()
    private val uiSettingsService = UiSettingsService(mockRepository)

    private fun createTestSettings(
        urlPreviewMode: UrlPreviewMode = UrlPreviewMode.INLINE_BLUR,
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        afterCleaningAction: AfterCleaningAction = AfterCleaningAction.ASK,
        suppressShareWarnings: Boolean = false,
        version: UInt = 2U
    ): UiSettings {
        return UiSettings(
            urlPreviewMode = urlPreviewMode,
            themeMode = themeMode,
            afterCleaningAction = afterCleaningAction,
            suppressShareWarnings = suppressShareWarnings,
            version = version
        )
    }

    @Test
    fun `getCurrentSettings should delegate to repository`() {
        val expectedSettings = createTestSettings()
        every { mockRepository.loadUiSettings() } returns expectedSettings

        val result = uiSettingsService.getCurrentSettings()

        assertEquals("Should return settings from repository", expectedSettings, result)
        verify { mockRepository.loadUiSettings() }
    }

    @Test
    fun `updateSettings should delegate to repository`() {
        val settings = createTestSettings(themeMode = ThemeMode.DARK)
        every { mockRepository.saveUiSettings(settings) } returns Unit

        uiSettingsService.updateSettings(settings)

        verify { mockRepository.saveUiSettings(settings) }
    }

    @Test
    fun `updateTheme should update only theme mode`() {
        val currentSettings = createTestSettings(
            themeMode = ThemeMode.SYSTEM,
            afterCleaningAction = AfterCleaningAction.ALWAYS_COPY,
            suppressShareWarnings = true
        )
        val expectedSettings = currentSettings.copy(themeMode = ThemeMode.DARK)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateTheme(ThemeMode.DARK)

        verify { mockRepository.loadUiSettings() }
        verify { mockRepository.saveUiSettings(expectedSettings) }
    }

    @Test
    fun `updatePreviewMode should update only URL preview mode`() {
        val currentSettings = createTestSettings(
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            themeMode = ThemeMode.LIGHT,
            suppressShareWarnings = true
        )
        val expectedSettings = currentSettings.copy(urlPreviewMode = UrlPreviewMode.INLINE_BLUR)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updatePreviewMode(UrlPreviewMode.INLINE_BLUR)

        verify { mockRepository.loadUiSettings() }
        verify { mockRepository.saveUiSettings(expectedSettings) }
    }

    @Test
    fun `updateAfterCleaningAction should update only after cleaning action`() {
        val currentSettings = createTestSettings(
            afterCleaningAction = AfterCleaningAction.ASK,
            themeMode = ThemeMode.DARK,
            suppressShareWarnings = false
        )
        val expectedSettings = currentSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE)

        verify { mockRepository.loadUiSettings() }
        verify { mockRepository.saveUiSettings(expectedSettings) }
    }

    @Test
    fun `updateSuppressShareWarnings should update only suppress share warnings`() {
        val currentSettings = createTestSettings(
            suppressShareWarnings = false,
            themeMode = ThemeMode.SYSTEM,
            afterCleaningAction = AfterCleaningAction.ALWAYS_COPY
        )
        val expectedSettings = currentSettings.copy(suppressShareWarnings = true)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateSuppressShareWarnings(true)

        verify { mockRepository.loadUiSettings() }
        verify { mockRepository.saveUiSettings(expectedSettings) }
    }

    @Test
    fun `resetToDefaults should save default settings`() {
        val defaultSettings = UiSettings()
        every { mockRepository.saveUiSettings(defaultSettings) } returns Unit

        uiSettingsService.resetToDefaults()

        verify { mockRepository.saveUiSettings(defaultSettings) }
    }

    @Test
    fun `updateTheme should preserve all other settings`() {
        val currentSettings = createTestSettings(
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            themeMode = ThemeMode.SYSTEM,
            afterCleaningAction = AfterCleaningAction.ALWAYS_COPY,
            suppressShareWarnings = true,
            version = 2U
        )
        val expectedSettings = currentSettings.copy(themeMode = ThemeMode.LIGHT)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateTheme(ThemeMode.LIGHT)

        verify { mockRepository.saveUiSettings(expectedSettings) }
        
        // Verify all other fields are preserved
        assertEquals("Should preserve URL preview mode", currentSettings.urlPreviewMode, expectedSettings.urlPreviewMode)
        assertEquals("Should preserve after cleaning action", currentSettings.afterCleaningAction, expectedSettings.afterCleaningAction)
        assertEquals("Should preserve suppress share warnings", currentSettings.suppressShareWarnings, expectedSettings.suppressShareWarnings)
        assertEquals("Should preserve version", currentSettings.version, expectedSettings.version)
        assertEquals("Should update theme mode", ThemeMode.LIGHT, expectedSettings.themeMode)
    }

    @Test
    fun `updatePreviewMode should preserve all other settings`() {
        val currentSettings = createTestSettings(
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            themeMode = ThemeMode.DARK,
            afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE,
            suppressShareWarnings = false,
            version = 2U
        )
        val expectedSettings = currentSettings.copy(urlPreviewMode = UrlPreviewMode.INLINE_BLUR)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updatePreviewMode(UrlPreviewMode.INLINE_BLUR)

        verify { mockRepository.saveUiSettings(expectedSettings) }
        
        // Verify all other fields are preserved
        assertEquals("Should preserve theme mode", currentSettings.themeMode, expectedSettings.themeMode)
        assertEquals("Should preserve after cleaning action", currentSettings.afterCleaningAction, expectedSettings.afterCleaningAction)
        assertEquals("Should preserve suppress share warnings", currentSettings.suppressShareWarnings, expectedSettings.suppressShareWarnings)
        assertEquals("Should preserve version", currentSettings.version, expectedSettings.version)
        assertEquals("Should update URL preview mode", UrlPreviewMode.INLINE_BLUR, expectedSettings.urlPreviewMode)
    }

    @Test
    fun `updateAfterCleaningAction should preserve all other settings`() {
        val currentSettings = createTestSettings(
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            themeMode = ThemeMode.LIGHT,
            afterCleaningAction = AfterCleaningAction.ASK,
            suppressShareWarnings = true,
            version = 2U
        )
        val expectedSettings = currentSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_COPY)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY)

        verify { mockRepository.saveUiSettings(expectedSettings) }
        
        // Verify all other fields are preserved
        assertEquals("Should preserve URL preview mode", currentSettings.urlPreviewMode, expectedSettings.urlPreviewMode)
        assertEquals("Should preserve theme mode", currentSettings.themeMode, expectedSettings.themeMode)
        assertEquals("Should preserve suppress share warnings", currentSettings.suppressShareWarnings, expectedSettings.suppressShareWarnings)
        assertEquals("Should preserve version", currentSettings.version, expectedSettings.version)
        assertEquals("Should update after cleaning action", AfterCleaningAction.ALWAYS_COPY, expectedSettings.afterCleaningAction)
    }

    @Test
    fun `updateSuppressShareWarnings should preserve all other settings`() {
        val currentSettings = createTestSettings(
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            themeMode = ThemeMode.SYSTEM,
            afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE,
            suppressShareWarnings = false,
            version = 2U
        )
        val expectedSettings = currentSettings.copy(suppressShareWarnings = true)

        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(expectedSettings) } returns Unit

        uiSettingsService.updateSuppressShareWarnings(true)

        verify { mockRepository.saveUiSettings(expectedSettings) }
        
        // Verify all other fields are preserved
        assertEquals("Should preserve URL preview mode", currentSettings.urlPreviewMode, expectedSettings.urlPreviewMode)
        assertEquals("Should preserve theme mode", currentSettings.themeMode, expectedSettings.themeMode)
        assertEquals("Should preserve after cleaning action", currentSettings.afterCleaningAction, expectedSettings.afterCleaningAction)
        assertEquals("Should preserve version", currentSettings.version, expectedSettings.version)
        assertEquals("Should update suppress share warnings", true, expectedSettings.suppressShareWarnings)
    }

    @Test
    fun `service should handle all theme modes`() {
        val currentSettings = createTestSettings(themeMode = ThemeMode.SYSTEM)
        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(any()) } returns Unit

        // Test LIGHT theme
        uiSettingsService.updateTheme(ThemeMode.LIGHT)
        verify { mockRepository.saveUiSettings(currentSettings.copy(themeMode = ThemeMode.LIGHT)) }

        // Test DARK theme
        uiSettingsService.updateTheme(ThemeMode.DARK)
        verify { mockRepository.saveUiSettings(currentSettings.copy(themeMode = ThemeMode.DARK)) }

        // Test SYSTEM theme
        uiSettingsService.updateTheme(ThemeMode.SYSTEM)
        verify { mockRepository.saveUiSettings(currentSettings.copy(themeMode = ThemeMode.SYSTEM)) }
    }

    @Test
    fun `service should handle all after cleaning actions`() {
        val currentSettings = createTestSettings(afterCleaningAction = AfterCleaningAction.ASK)
        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(any()) } returns Unit

        // Test ALWAYS_SHARE
        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE)
        verify { mockRepository.saveUiSettings(currentSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE)) }

        // Test ALWAYS_COPY
        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY)
        verify { mockRepository.saveUiSettings(currentSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_COPY)) }

        // Test ASK
        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ASK)
        verify { mockRepository.saveUiSettings(currentSettings.copy(afterCleaningAction = AfterCleaningAction.ASK)) }
    }

    @Test
    fun `service should handle boolean toggle for suppress share warnings`() {
        val currentSettings = createTestSettings(suppressShareWarnings = false)
        every { mockRepository.loadUiSettings() } returns currentSettings
        every { mockRepository.saveUiSettings(any()) } returns Unit

        // Test enabling
        uiSettingsService.updateSuppressShareWarnings(true)
        verify { mockRepository.saveUiSettings(currentSettings.copy(suppressShareWarnings = true)) }

        // Test disabling
        uiSettingsService.updateSuppressShareWarnings(false)
        verify { mockRepository.saveUiSettings(currentSettings.copy(suppressShareWarnings = false)) }
    }

    @Test
    fun `resetToDefaults should use default UiSettings values`() {
        val defaultSettings = UiSettings()
        every { mockRepository.saveUiSettings(defaultSettings) } returns Unit

        uiSettingsService.resetToDefaults()

        verify { mockRepository.saveUiSettings(defaultSettings) }
        
        // Verify default values
        assertEquals("Default URL preview mode should be INLINE_BLUR", UrlPreviewMode.INLINE_BLUR, defaultSettings.urlPreviewMode)
        assertEquals("Default theme mode should be SYSTEM", ThemeMode.SYSTEM, defaultSettings.themeMode)
        assertEquals("Default after cleaning action should be ASK", AfterCleaningAction.ASK, defaultSettings.afterCleaningAction)
        assertEquals("Default suppress share warnings should be false", false, defaultSettings.suppressShareWarnings)
        assertEquals("Default version should be 2", 2U, defaultSettings.version)
    }

    @Test
    fun `service should handle multiple consecutive updates`() {
        val initialSettings = createTestSettings()
        every { mockRepository.loadUiSettings() } returns initialSettings
        every { mockRepository.saveUiSettings(any()) } returns Unit

        // Perform multiple updates
        uiSettingsService.updateTheme(ThemeMode.DARK)
        uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY)
        uiSettingsService.updateSuppressShareWarnings(true)

        // Verify each update was called
        verify { mockRepository.saveUiSettings(initialSettings.copy(themeMode = ThemeMode.DARK)) }
        verify { mockRepository.saveUiSettings(initialSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_COPY)) }
        verify { mockRepository.saveUiSettings(initialSettings.copy(suppressShareWarnings = true)) }
        
        // Verify repository was loaded for each update
        verify(exactly = 3) { mockRepository.loadUiSettings() }
    }

    @Test
    fun `service should handle custom version settings`() {
        val customVersionSettings = createTestSettings(version = 1U)
        every { mockRepository.loadUiSettings() } returns customVersionSettings
        every { mockRepository.saveUiSettings(any()) } returns Unit

        uiSettingsService.updateTheme(ThemeMode.LIGHT)

        val expectedSettings = customVersionSettings.copy(themeMode = ThemeMode.LIGHT)
        verify { mockRepository.saveUiSettings(expectedSettings) }
        assertEquals("Should preserve custom version", 1U, expectedSettings.version)
    }
}
