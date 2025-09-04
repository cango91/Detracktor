package com.gologlu.detracktor.runtime.android.test

import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode

/**
 * Comprehensive test data sets for runtime package testing.
 * Provides realistic test scenarios covering edge cases, internationalization, and various configurations.
 */
object TestData {
    
    /**
     * URL test data covering various scenarios
     */
    object Urls {
        const val SIMPLE_HTTP = "http://example.com"
        const val SIMPLE_HTTPS = "https://example.com"
        const val WITH_PATH = "https://example.com/path/to/resource"
        const val WITH_QUERY = "https://example.com?param1=value1&param2=value2"
        const val WITH_FRAGMENT = "https://example.com#section"
        const val COMPLEX_URL = "https://user:pass@example.com:8080/path?param=value#fragment"
        
        // URLs with tracking parameters
        const val WITH_UTM = "https://example.com?utm_source=google&utm_medium=cpc&utm_campaign=test"
        const val WITH_FACEBOOK_TRACKING = "https://example.com?fbclid=IwAR123456789"
        const val WITH_GOOGLE_TRACKING = "https://example.com?gclid=Cj0KCQjw123456789"
        
        // International URLs
        const val INTERNATIONAL_DOMAIN = "https://例え.テスト"
        const val PUNYCODE_DOMAIN = "https://xn--r8jz45g.xn--zckzah"
        const val UNICODE_PATH = "https://example.com/测试/路径"
        const val UNICODE_QUERY = "https://example.com?名前=値"
        
        // Edge cases
        const val MALFORMED_SCHEME = "htp://example.com"
        const val MISSING_SCHEME = "example.com"
        const val EMPTY_URL = ""
        const val WHITESPACE_URL = "  https://example.com  "
        val VERY_LONG_URL = "https://example.com/" + "a".repeat(2000)

        // Social media URLs
        const val YOUTUBE_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=youtu.be"
        const val TWITTER_URL = "https://twitter.com/user/status/123456789?ref_src=twsrc%5Etfw"
        const val FACEBOOK_URL = "https://www.facebook.com/sharer/sharer.php?u=https%3A//example.com"
        
        // E-commerce URLs
        const val AMAZON_URL = "https://www.amazon.com/dp/B08N5WRWNW?tag=affiliate-20&linkCode=ogi"
        const val EBAY_URL = "https://www.ebay.com/itm/123456789?hash=item1234567890:g:abcAAOSw123456789"
        
        fun getAllTestUrls(): List<String> = listOf(
            SIMPLE_HTTP, SIMPLE_HTTPS, WITH_PATH, WITH_QUERY, WITH_FRAGMENT, COMPLEX_URL,
            WITH_UTM, WITH_FACEBOOK_TRACKING, WITH_GOOGLE_TRACKING,
            INTERNATIONAL_DOMAIN, PUNYCODE_DOMAIN, UNICODE_PATH, UNICODE_QUERY,
            MALFORMED_SCHEME, MISSING_SCHEME, EMPTY_URL, WHITESPACE_URL, VERY_LONG_URL,
            YOUTUBE_URL, TWITTER_URL, FACEBOOK_URL, AMAZON_URL, EBAY_URL
        )
    }
    
    /**
     * Settings test data for various configurations
     */
    object SettingsData {
        
        fun defaultSettings(): AppSettings = AppSettings(
            sites = emptyList(),
            version = AppSettings.VERSION
        )
        
        fun settingsWithCustomRules(): AppSettings = AppSettings(
            sites = listOf(
                // Simple example.com rule
                com.gologlu.detracktor.application.types.UrlRule(
                    when_ = com.gologlu.detracktor.application.types.WhenBlock(
                        host = com.gologlu.detracktor.application.types.HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("example.com"))
                        )
                    ),
                    then = com.gologlu.detracktor.application.types.ThenBlock(
                        remove = listOf(Pattern("utm_source"), Pattern("utm_medium"), Pattern("utm_campaign")),
                        warn = null
                    )
                ),
                // Social media rule
                com.gologlu.detracktor.application.types.UrlRule(
                    when_ = com.gologlu.detracktor.application.types.WhenBlock(
                        host = com.gologlu.detracktor.application.types.HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("social.com")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                        )
                    ),
                    then = com.gologlu.detracktor.application.types.ThenBlock(
                        remove = listOf(Pattern("fbclid"), Pattern("gclid")),
                        warn = null
                    )
                )
            ),
            version = AppSettings.VERSION
        )
        
        fun settingsWithDisabledRules(): AppSettings = AppSettings(
            sites = listOf(
                // Disabled rule (represented by empty sites list for simplicity)
            ),
            version = AppSettings.VERSION
        )
        
        fun settingsWithComplexPatterns(): AppSettings = AppSettings(
            sites = listOf(
                // Amazon rule
                com.gologlu.detracktor.application.types.UrlRule(
                    when_ = com.gologlu.detracktor.application.types.WhenBlock(
                        host = com.gologlu.detracktor.application.types.HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("amazon.com")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                        )
                    ),
                    then = com.gologlu.detracktor.application.types.ThenBlock(
                        remove = listOf(Pattern("tag"), Pattern("linkCode"), Pattern("ref"), Pattern("psc")),
                        warn = null
                    )
                ),
                // YouTube rule
                com.gologlu.detracktor.application.types.UrlRule(
                    when_ = com.gologlu.detracktor.application.types.WhenBlock(
                        host = com.gologlu.detracktor.application.types.HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("youtube.com"))
                        )
                    ),
                    then = com.gologlu.detracktor.application.types.ThenBlock(
                        remove = listOf(Pattern("feature"), Pattern("t"), Pattern("list")),
                        warn = null
                    )
                ),
                // Google rule
                com.gologlu.detracktor.application.types.UrlRule(
                    when_ = com.gologlu.detracktor.application.types.WhenBlock(
                        host = com.gologlu.detracktor.application.types.HostCond(
                            domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("google.com")),
                            subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                        )
                    ),
                    then = com.gologlu.detracktor.application.types.ThenBlock(
                        remove = listOf(Pattern("gclid"), Pattern("gclsrc"), Pattern("dclid")),
                        warn = null
                    )
                )
            ),
            version = AppSettings.VERSION
        )
        
        fun getAllTestSettings(): List<AppSettings> = listOf(
            defaultSettings(),
            settingsWithCustomRules(),
            settingsWithDisabledRules(),
            settingsWithComplexPatterns()
        )
    }
    
    /**
     * UI Settings test data for various configurations
     */
    object UiSettingsData {
        
        fun defaultUiSettings(): UiSettings = UiSettings()
        
        fun lightThemeSettings(): UiSettings = UiSettings(
            themeMode = ThemeMode.LIGHT,
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            afterCleaningAction = AfterCleaningAction.ASK,
            suppressShareWarnings = false
        )
        
        fun darkThemeSettings(): UiSettings = UiSettings(
            themeMode = ThemeMode.DARK,
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            afterCleaningAction = AfterCleaningAction.ALWAYS_COPY,
            suppressShareWarnings = true
        )
        
        fun alwaysShareSettings(): UiSettings = UiSettings(
            themeMode = ThemeMode.SYSTEM,
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE,
            suppressShareWarnings = false
        )
        
        fun alwaysCopySettings(): UiSettings = UiSettings(
            themeMode = ThemeMode.SYSTEM,
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            afterCleaningAction = AfterCleaningAction.ALWAYS_COPY,
            suppressShareWarnings = true
        )
        
        fun suppressWarningsSettings(): UiSettings = UiSettings(
            themeMode = ThemeMode.DARK,
            urlPreviewMode = UrlPreviewMode.INLINE_BLUR,
            afterCleaningAction = AfterCleaningAction.ASK,
            suppressShareWarnings = true
        )
        
        fun getAllTestUiSettings(): List<UiSettings> = listOf(
            defaultUiSettings(),
            lightThemeSettings(),
            darkThemeSettings(),
            alwaysShareSettings(),
            alwaysCopySettings(),
            suppressWarningsSettings()
        )
    }
    
    /**
     * Rule engine test data with complex pattern matching scenarios
     */
    object RuleData {
        
        fun simpleRule(): Pattern = Pattern("utm_source")
        
        fun wildcardRule(): Pattern = Pattern("tracking")
        
        fun globalWildcardRule(): Pattern = Pattern("fbclid")
        
        fun disabledRule(): Pattern = Pattern("param")
        
        fun complexDomainRule(): Pattern = Pattern("tag")
        
        fun getAllTestRules(): List<Pattern> = listOf(
            simpleRule(),
            wildcardRule(),
            globalWildcardRule(),
            disabledRule(),
            complexDomainRule()
        )
    }
    
    /**
     * Test scenarios combining URLs with rules for comprehensive testing
     */
    object Scenarios {
        
        data class TestScenario(
            val name: String,
            val url: String,
            val settings: AppSettings,
            val expectedCleaned: String,
            val shouldClean: Boolean
        )
        
        fun getCleaningScenarios(): List<TestScenario> = listOf(
            TestScenario(
                name = "Simple UTM removal",
                url = "https://example.com?utm_source=google&param=keep",
                settings = SettingsData.settingsWithCustomRules(),
                expectedCleaned = "https://example.com?param=keep",
                shouldClean = true
            ),
            TestScenario(
                name = "No matching rules",
                url = "https://nomatch.com?param=value",
                settings = SettingsData.settingsWithCustomRules(),
                expectedCleaned = "https://nomatch.com?param=value",
                shouldClean = false
            ),
            TestScenario(
                name = "Disabled settings",
                url = "https://example.com?utm_source=google",
                settings = SettingsData.settingsWithDisabledRules(),
                expectedCleaned = "https://example.com?utm_source=google",
                shouldClean = false
            ),
            TestScenario(
                name = "Complex Amazon URL",
                url = "https://www.amazon.com/dp/B08N5WRWNW?tag=affiliate-20&linkCode=ogi&keep=this",
                settings = SettingsData.settingsWithComplexPatterns(),
                expectedCleaned = "https://www.amazon.com/dp/B08N5WRWNW?keep=this",
                shouldClean = true
            )
        )
    }
    
    /**
     * Host canonicalization test data
     */
    object HostData {
        
        data class HostTestCase(
            val input: String,
            val expected: String?,
            val description: String
        )
        
        fun getHostCanonicalizationCases(): List<HostTestCase> = listOf(
            HostTestCase("example.com", "example.com", "Simple ASCII domain"),
            HostTestCase("EXAMPLE.COM", "example.com", "Uppercase domain"),
            HostTestCase("例え.テスト", "xn--r8jz45g.xn--zckzah", "Japanese IDN"),
            HostTestCase("münchen.de", "xn--mnchen-3ya.de", "German umlaut"),
            HostTestCase("россия.рф", "xn--h1alffa9f.xn--p1ai", "Cyrillic domain"),
            HostTestCase("🌟.example", "xn--ch8h.example", "Emoji domain"), // Corrected punycode
            HostTestCase("sub.example.com", "sub.example.com", "Subdomain"),
            HostTestCase("", null, "Empty string"), // Empty string returns null
            HostTestCase("localhost", "localhost", "Localhost")
        )
    }
    
    /**
     * Error scenarios for testing error handling
     */
    object ErrorScenarios {
        
        fun getInvalidUrls(): List<String> = listOf(
            "not-a-url",
            "://missing-scheme",
            "http://",
            "https://[invalid-ipv6",
            "ftp://unsupported-scheme.com",
            "javascript:alert('xss')",
            "data:text/html,<script>alert('xss')</script>"
        )
        
        fun getInvalidSettings(): List<AppSettings> = listOf(
            // Settings with invalid patterns could be added here
            // For now, the AppSettings class is well-structured
        )
    }
}
