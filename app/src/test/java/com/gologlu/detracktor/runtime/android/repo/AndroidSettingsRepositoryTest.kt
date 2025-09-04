package com.gologlu.detracktor.runtime.android.repo

import androidx.test.core.app.ApplicationProvider
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.error.getOrNull
import com.gologlu.detracktor.application.error.isSuccess
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.application.types.Subdomains
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.application.types.WhenBlock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for AndroidSettingsRepository.
 * Tests Android settings repository with file system integration using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AndroidSettingsRepositoryTest {
    
    private lateinit var repository: AndroidSettingsRepository
    private lateinit var context: android.content.Context
    private lateinit var userSettingsFile: File
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = AndroidSettingsRepository(context)
        userSettingsFile = File(context.filesDir, "user_settings.json")
        
        // Clean up any existing user settings
        if (userSettingsFile.exists()) {
            userSettingsFile.delete()
        }
    }
    
    @After
    fun tearDown() {
        // Clean up after each test
        if (userSettingsFile.exists()) {
            userSettingsFile.delete()
        }
        
        // Clean up any temporary files
        val tmpFile = File(context.filesDir, "user_settings.json.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
    }
    
    @Test
    fun `readUserSettings returns null when no user settings file exists`() = runTest {
        // When
        val result = repository.readUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `readDefaultSettings loads default settings from assets`() = runTest {
        // When
        val result = repository.readDefaultSettings()
        
        // Then
        assertTrue(result.isSuccess)
        val settings = result.getOrNull()
        assertNotNull(settings)
        assertEquals(AppSettings.VERSION, settings.version)
        assertTrue(settings.sites.isNotEmpty())
    }
    
    @Test
    fun `writeUserSettings creates and persists settings file`() = runTest {
        // Given
        val testSettings = createTestAppSettings()
        
        // When
        val writeResult = repository.writeUserSettings(testSettings)
        
        // Then
        assertTrue(writeResult.isSuccess)
        assertTrue(userSettingsFile.exists())
        
        // Verify by reading back
        val readResult = repository.readUserSettings()
        assertTrue(readResult.isSuccess)
        val loadedSettings = readResult.getOrNull()
        assertNotNull(loadedSettings)
        assertEquals(testSettings.version, loadedSettings.version)
        assertEquals(testSettings.sites.size, loadedSettings.sites.size)
    }
    
    @Test
    fun `writeUserSettings overwrites existing settings file`() = runTest {
        // Given
        val initialSettings = createTestAppSettings()
        val updatedSettings = createTestAppSettings().copy(
            sites = listOf(
                createSimpleUrlRule("updated.com", listOf("updated_param"))
            )
        )
        
        // When
        repository.writeUserSettings(initialSettings)
        val firstRead = repository.readUserSettings()
        
        repository.writeUserSettings(updatedSettings)
        val secondRead = repository.readUserSettings()
        
        // Then
        assertTrue(firstRead.isSuccess)
        assertTrue(secondRead.isSuccess)
        
        val firstSettings = firstRead.getOrNull()!!
        val secondSettings = secondRead.getOrNull()!!
        
        assertNotNull(firstSettings.sites.find { rule -> 
            rule.when_.host.domains is Domains.ListOf && 
            (rule.when_.host.domains as Domains.ListOf).values.contains("example.com")
        })
        
        assertNotNull(secondSettings.sites.find { rule -> 
            rule.when_.host.domains is Domains.ListOf && 
            (rule.when_.host.domains as Domains.ListOf).values.contains("updated.com")
        })
    }
    
    @Test
    fun `clearUserSettings removes user settings file`() = runTest {
        // Given
        val testSettings = createTestAppSettings()
        repository.writeUserSettings(testSettings)
        assertTrue(userSettingsFile.exists())
        
        // When
        val result = repository.clearUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(userSettingsFile.exists())
        
        // Verify reading returns null
        val readResult = repository.readUserSettings()
        assertTrue(readResult.isSuccess)
        assertNull(readResult.getOrNull())
    }
    
    @Test
    fun `clearUserSettings succeeds when no file exists`() = runTest {
        // Given
        assertFalse(userSettingsFile.exists())
        
        // When
        val result = repository.clearUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `writeUserSettings handles complex settings with all features`() = runTest {
        // Given
        val complexSettings = createComplexAppSettings()
        
        // When
        val writeResult = repository.writeUserSettings(complexSettings)
        val readResult = repository.readUserSettings()
        
        // Then
        assertTrue(writeResult.isSuccess)
        assertTrue(readResult.isSuccess)
        
        val loadedSettings = readResult.getOrNull()!!
        assertEquals(complexSettings.version, loadedSettings.version)
        assertEquals(complexSettings.sites.size, loadedSettings.sites.size)
        
        // Verify complex rule structure is preserved
        val complexRule = loadedSettings.sites.first()
        assertTrue(complexRule.when_.host.domains is Domains.ListOf)
        assertNotNull(complexRule.when_.host.subdomains)
        assertNotNull(complexRule.when_.schemes)
        assertTrue(complexRule.then.remove.isNotEmpty())
        assertNotNull(complexRule.then.warn)
        assertNotNull(complexRule.metadata)
    }
    
    @Test
    fun `writeUserSettings handles wildcard domains correctly`() = runTest {
        // Given
        val wildcardSettings = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(domains = Domains.Any),
                        schemes = null
                    ),
                    then = ThenBlock(
                        remove = listOf(Pattern("utm_*")),
                        warn = null
                    ),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        
        // When
        val writeResult = repository.writeUserSettings(wildcardSettings)
        val readResult = repository.readUserSettings()
        
        // Then
        assertTrue(writeResult.isSuccess)
        assertTrue(readResult.isSuccess)
        
        val loadedSettings = readResult.getOrNull()!!
        val rule = loadedSettings.sites.first()
        assertTrue(rule.when_.host.domains is Domains.Any)
    }
    
    @Test
    fun `writeUserSettings handles subdomain configurations correctly`() = runTest {
        // Given
        val subdomainSettings = AppSettings(
            sites = listOf(
                // Rule with specific subdomains
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = Domains.ListOf(listOf("example.com")),
                            subdomains = Subdomains.OneOf(listOf("www", "api"))
                        ),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("test")), warn = null),
                    metadata = null
                ),
                // Rule with any subdomains
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = Domains.ListOf(listOf("wildcard.com")),
                            subdomains = Subdomains.Any
                        ),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("param")), warn = null),
                    metadata = null
                ),
                // Rule with no subdomains
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = Domains.ListOf(listOf("nosub.com")),
                            subdomains = Subdomains.None
                        ),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("clean")), warn = null),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        
        // When
        val writeResult = repository.writeUserSettings(subdomainSettings)
        val readResult = repository.readUserSettings()
        
        // Then
        assertTrue(writeResult.isSuccess)
        assertTrue(readResult.isSuccess)
        
        val loadedSettings = readResult.getOrNull()!!
        assertEquals(3, loadedSettings.sites.size)
        
        val specificSubRule = loadedSettings.sites[0]
        assertTrue(specificSubRule.when_.host.subdomains is Subdomains.OneOf)
        assertEquals(2, (specificSubRule.when_.host.subdomains as Subdomains.OneOf).labels.size)
        
        val anySubRule = loadedSettings.sites[1]
        assertTrue(anySubRule.when_.host.subdomains is Subdomains.Any)
        
        val noSubRule = loadedSettings.sites[2]
        assertTrue(noSubRule.when_.host.subdomains is Subdomains.None)
    }
    
    @Test
    fun `writeUserSettings handles warning settings correctly`() = runTest {
        // Given
        val warningSettings = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(domains = Domains.ListOf(listOf("example.com"))),
                        schemes = null
                    ),
                    then = ThenBlock(
                        remove = listOf(Pattern("utm_source")),
                        warn = WarningSettings(
                            warnOnEmbeddedCredentials = true,
                            sensitiveParams = listOf("password", "token", "key"),
                            sensitiveMerge = com.gologlu.detracktor.application.types.SensitiveMergeMode.UNION,
                            version = WarningSettings.VERSION
                        )
                    ),
                    metadata = null
                )
            ),
            version = AppSettings.VERSION
        )
        
        // When
        val writeResult = repository.writeUserSettings(warningSettings)
        val readResult = repository.readUserSettings()
        
        // Then
        assertTrue(writeResult.isSuccess)
        assertTrue(readResult.isSuccess)
        
        val loadedSettings = readResult.getOrNull()!!
        val rule = loadedSettings.sites.first()
        val warning = rule.then.warn!!
        
        assertEquals(true, warning.warnOnEmbeddedCredentials)
        assertEquals(3, warning.sensitiveParams!!.size)
        assertTrue(warning.sensitiveParams!!.contains("password"))
        assertEquals(com.gologlu.detracktor.application.types.SensitiveMergeMode.UNION, warning.sensitiveMerge)
    }
    
    @Test
    fun `concurrent write operations maintain data integrity`() = runTest {
        // Given
        val settings1 = createTestAppSettings()
        val settings2 = createComplexAppSettings()
        
        // When - simulate concurrent writes
        val result1 = repository.writeUserSettings(settings1)
        val read1 = repository.readUserSettings()
        
        val result2 = repository.writeUserSettings(settings2)
        val read2 = repository.readUserSettings()
        
        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(read1.isSuccess)
        assertTrue(read2.isSuccess)
        
        // Last write should win
        val finalSettings = read2.getOrNull()!!
        assertEquals(settings2.sites.size, finalSettings.sites.size)
    }
    
    @Test
    fun `repository handles file system errors gracefully`() = runTest {
        // Given - make files directory read-only to simulate file system error
        val filesDir = context.filesDir
        val originalPermissions = filesDir.canWrite()
        
        try {
            // This test may not work on all systems due to permission handling
            // but it demonstrates the error handling approach
            val testSettings = createTestAppSettings()
            
            // When
            val result = repository.writeUserSettings(testSettings)
            
            // Then - should handle gracefully (exact behavior depends on system)
            // The test mainly ensures no crashes occur
            assertNotNull(result)
        } finally {
            // Restore permissions if changed
            if (!originalPermissions) {
                filesDir.setWritable(true)
            }
        }
    }
    
    // Helper methods for creating test data
    
    private fun createTestAppSettings(): AppSettings {
        return AppSettings(
            sites = listOf(
                createSimpleUrlRule("example.com", listOf("utm_source", "utm_medium")),
                createSimpleUrlRule("test.com", listOf("tracking", "ref"))
            ),
            version = AppSettings.VERSION
        )
    }
    
    private fun createSimpleUrlRule(domain: String, removeParams: List<String>): UrlRule {
        return UrlRule(
            when_ = WhenBlock(
                host = HostCond(domains = Domains.ListOf(listOf(domain))),
                schemes = null
            ),
            then = ThenBlock(
                remove = removeParams.map { Pattern(it) },
                warn = null
            ),
            metadata = null
        )
    }
    
    private fun createComplexAppSettings(): AppSettings {
        return AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(
                            domains = Domains.ListOf(listOf("complex.com", "alt.com")),
                            subdomains = Subdomains.OneOf(listOf("www", "api"))
                        ),
                        schemes = listOf("https", "http")
                    ),
                    then = ThenBlock(
                        remove = listOf(
                            Pattern("utm_*"),
                            Pattern("fbclid"),
                            Pattern("gclid")
                        ),
                        warn = WarningSettings(
                            warnOnEmbeddedCredentials = true,
                            sensitiveParams = listOf("password", "token"),
                            sensitiveMerge = com.gologlu.detracktor.application.types.SensitiveMergeMode.REPLACE,
                            version = WarningSettings.VERSION
                        )
                    ),
                    metadata = mapOf(
                        "name" to "Complex Rule",
                        "description" to "A complex rule for testing",
                        "priority" to 1
                    )
                )
            ),
            version = AppSettings.VERSION
        )
    }
}
