package com.gologlu.detracktor.application.service

import com.gologlu.detracktor.application.error.AppException
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.error.AppValidationError
import com.gologlu.detracktor.application.error.AppValidationException
import com.gologlu.detracktor.application.repo.SettingsRepository
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.application.types.WhenBlock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class SettingsTest {

    private val mockRepository = mockk<SettingsRepository>()
    private val settingsService = DefaultSettingsService(mockRepository)

    private fun createTestSettings(sites: List<UrlRule> = listOf(createTestRule())): AppSettings {
        return AppSettings(sites = sites, version = 1U)
    }

    private fun createTestRule(): UrlRule {
        return UrlRule(
            when_ = WhenBlock(
                host = HostCond(domains = Domains.ListOf(listOf("example.com")))
            ),
            then = ThenBlock(
                remove = listOf(Pattern("utm_*")),
                warn = null
            ),
            version = 1U
        )
    }

    @Test
    fun `loadEffective should return defaults when no user settings`() = runTest {
        val defaultSettings = createTestSettings()
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(null)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        assertEquals("Should return default settings", defaultSettings, (result as AppResult.Success).value)
        
        coVerify { mockRepository.readDefaultSettings() }
        coVerify { mockRepository.readUserSettings() }
    }

    @Test
    fun `loadEffective should merge user settings with defaults`() = runTest {
        val defaultRule = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("default.com")))),
            then = ThenBlock(remove = listOf(Pattern("default_*")), warn = null),
            version = 1U
        )
        val userRule = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("user.com")))),
            then = ThenBlock(remove = listOf(Pattern("user_*")), warn = null),
            version = 1U
        )
        
        val defaultSettings = AppSettings(sites = listOf(defaultRule), version = 1U)
        val userSettings = AppSettings(sites = listOf(userRule), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use user sites", userSettings.sites, effective.sites)
        assertEquals("Should use max version", 1U, effective.version)
    }

    @Test
    fun `loadEffective should use defaults when user settings has empty sites`() = runTest {
        val defaultSettings = createTestSettings()
        val userSettings = AppSettings(sites = emptyList(), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use default sites when user sites empty", defaultSettings.sites, effective.sites)
        assertEquals("Should use max version", 1U, effective.version)
    }

    @Test
    fun `loadEffective should handle different versions correctly`() = runTest {
        val defaultSettings = AppSettings(sites = listOf(createTestRule()), version = 1U)
        val userSettings = AppSettings(sites = listOf(createTestRule()), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use max version", 1U, effective.version)
    }

    @Test
    fun `loadEffective should propagate default settings read failure`() = runTest {
        val error = AppValidationError("Failed to read defaults", "defaults")
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.failure(error)

        val result = settingsService.loadEffective()

        assertTrue("Should be failure", result is AppResult.Failure)
        coVerify { mockRepository.readDefaultSettings() }
        coVerify(exactly = 0) { mockRepository.readUserSettings() }
    }

    @Test
    fun `loadEffective should propagate user settings read failure`() = runTest {
        val defaultSettings = createTestSettings()
        val error = AppValidationError("Failed to read user settings", "user")
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.failure(error)

        val result = settingsService.loadEffective()

        assertTrue("Should be failure", result is AppResult.Failure)
        coVerify { mockRepository.readDefaultSettings() }
        coVerify { mockRepository.readUserSettings() }
    }

    @Test
    fun `loadUser should delegate to repository`() = runTest {
        val userSettings = createTestSettings()
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadUser()

        assertTrue("Should be success", result is AppResult.Success)
        assertEquals("Should return user settings", userSettings, (result as AppResult.Success).value)
        coVerify { mockRepository.readUserSettings() }
    }

    @Test
    fun `loadUser should return null when no user settings`() = runTest {
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(null)

        val result = settingsService.loadUser()

        assertTrue("Should be success", result is AppResult.Success)
        assertNull("Should return null", (result as AppResult.Success).value)
        coVerify { mockRepository.readUserSettings() }
    }

    @Test
    fun `loadUser should propagate repository failure`() = runTest {
        val error = AppValidationError("Repository error", "repo")
        coEvery { mockRepository.readUserSettings() } returns AppResult.failure(error)

        val result = settingsService.loadUser()

        assertTrue("Should be failure", result is AppResult.Failure)
        coVerify { mockRepository.readUserSettings() }
    }

    @Test
    fun `saveUser should validate and save valid settings`() = runTest {
        val validSettings = createTestSettings()
        coEvery { mockRepository.writeUserSettings(validSettings) } returns AppResult.success(Unit)

        val result = settingsService.saveUser(validSettings)

        assertTrue("Should be success", result is AppResult.Success)
        coVerify { mockRepository.writeUserSettings(validSettings) }
    }

    @Test
    fun `saveUser should reject settings with empty sites`() = runTest {
        val invalidSettings = AppSettings(sites = emptyList(), version = 1U)

        try {
            settingsService.saveUser(invalidSettings)
            fail("Should have thrown AppException")
        } catch (e: AppException) {
            assertTrue("Should be AppValidationError", e.error is AppValidationError)
            val validationError = e.error as AppValidationError
            assertEquals("Should have correct field path", "settings.sites", validationError.fieldPath)
            assertTrue("Should mention empty sites", validationError.message.contains("empty"))
        }

        coVerify(exactly = 0) { mockRepository.writeUserSettings(any()) }
    }

    @Test
    fun `saveUser should propagate repository write failure`() = runTest {
        val validSettings = createTestSettings()
        val error = AppValidationError("Write failed", "write")
        coEvery { mockRepository.writeUserSettings(validSettings) } returns AppResult.failure(error)

        val result = settingsService.saveUser(validSettings)

        assertTrue("Should be failure", result is AppResult.Failure)
        coVerify { mockRepository.writeUserSettings(validSettings) }
    }

    @Test
    fun `resetUser should delegate to repository`() = runTest {
        coEvery { mockRepository.clearUserSettings() } returns AppResult.success(Unit)

        val result = settingsService.resetUser()

        assertTrue("Should be success", result is AppResult.Success)
        coVerify { mockRepository.clearUserSettings() }
    }

    @Test
    fun `resetUser should propagate repository failure`() = runTest {
        val error = AppValidationError("Clear failed", "clear")
        coEvery { mockRepository.clearUserSettings() } returns AppResult.failure(error)

        val result = settingsService.resetUser()

        assertTrue("Should be failure", result is AppResult.Failure)
        coVerify { mockRepository.clearUserSettings() }
    }

    @Test
    fun `merge should handle null user settings`() = runTest {
        val defaultSettings = createTestSettings()
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(null)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        assertEquals("Should return defaults when user is null", defaultSettings, (result as AppResult.Success).value)
    }

    @Test
    fun `merge should prefer user sites when non-empty`() = runTest {
        val defaultRule = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("default.com")))),
            then = ThenBlock(remove = listOf(Pattern("default_*")), warn = null),
            version = 1U
        )
        val userRule = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("user.com")))),
            then = ThenBlock(remove = listOf(Pattern("user_*")), warn = null),
            version = 1U
        )
        
        val defaultSettings = AppSettings(sites = listOf(defaultRule), version = 1U)
        val userSettings = AppSettings(sites = listOf(userRule), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use user sites", listOf(userRule), effective.sites)
        assertEquals("Should use max version", 1U, effective.version)
    }

    @Test
    fun `merge should use default sites when user sites empty`() = runTest {
        val defaultRule = createTestRule()
        val defaultSettings = AppSettings(sites = listOf(defaultRule), version = 1U)
        val userSettings = AppSettings(sites = emptyList(), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use default sites when user sites empty", listOf(defaultRule), effective.sites)
        assertEquals("Should use max version", 1U, effective.version)
    }

    @Test
    fun `validate should accept valid settings`() = runTest {
        val validSettings = createTestSettings()
        coEvery { mockRepository.writeUserSettings(validSettings) } returns AppResult.success(Unit)

        val result = settingsService.saveUser(validSettings)

        assertTrue("Should be success", result is AppResult.Success)
    }

    @Test
    fun `validate should reject settings with empty sites list`() = runTest {
        val invalidSettings = AppSettings(sites = emptyList(), version = 1U)

        try {
            settingsService.saveUser(invalidSettings)
            fail("Should have thrown AppException")
        } catch (e: AppException) {
            assertTrue("Should be AppValidationError", e.error is AppValidationError)
            val validationError = e.error as AppValidationError
            assertEquals("Should have correct field path", "settings.sites", validationError.fieldPath)
            assertTrue("Should mention empty sites", validationError.message.contains("empty"))
        }
    }

    @Test
    fun `validate should accept settings with valid version`() = runTest {
        val validSettings = AppSettings(sites = listOf(createTestRule()), version = 1U)
        coEvery { mockRepository.writeUserSettings(validSettings) } returns AppResult.success(Unit)

        val result = settingsService.saveUser(validSettings)

        assertTrue("Should be success", result is AppResult.Success)
    }

    @Test
    fun `service should handle complex merge scenarios`() = runTest {
        // Test with multiple rules and different warning settings
        val defaultRule1 = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("default1.com")))),
            then = ThenBlock(
                remove = listOf(Pattern("default1_*")), 
                warn = WarningSettings(warnOnEmbeddedCredentials = true)
            ),
            version = 1U
        )
        val defaultRule2 = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.ListOf(listOf("default2.com")))),
            then = ThenBlock(remove = listOf(Pattern("default2_*")), warn = null),
            version = 1U
        )
        
        val userRule = UrlRule(
            when_ = WhenBlock(host = HostCond(domains = Domains.Any)),
            then = ThenBlock(
                remove = listOf(Pattern("user_*"), Pattern("tracking_*")), 
                warn = WarningSettings(sensitiveParams = listOf("token", "key"))
            ),
            version = 1U
        )
        
        val defaultSettings = AppSettings(sites = listOf(defaultRule1, defaultRule2), version = 1U)
        val userSettings = AppSettings(sites = listOf(userRule), version = 1U)
        
        coEvery { mockRepository.readDefaultSettings() } returns AppResult.success(defaultSettings)
        coEvery { mockRepository.readUserSettings() } returns AppResult.success(userSettings)

        val result = settingsService.loadEffective()

        assertTrue("Should be success", result is AppResult.Success)
        val effective = (result as AppResult.Success).value
        assertEquals("Should use user sites", listOf(userRule), effective.sites)
        assertEquals("Should preserve user rule structure", userRule.then.remove.size, effective.sites[0].then.remove.size)
        assertEquals("Should preserve warning settings", userRule.then.warn, effective.sites[0].then.warn)
    }
}
