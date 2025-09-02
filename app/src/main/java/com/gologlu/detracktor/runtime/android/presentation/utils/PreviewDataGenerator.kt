package com.gologlu.detracktor.runtime.android.presentation.utils

import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.domain.service.UrlParser
import com.gologlu.detracktor.runtime.android.presentation.types.*

/**
 * Utility for generating mock analysis data for preview functionality
 */
class PreviewDataGenerator {
    
    companion object {
        /**
         * Sample URL with embedded credentials and tracking parameters
         */
        const val SAMPLE_URL = "https://user:pass@example.com/path?token=abc&normal_param=1234&tracker_param=5678"
        
        /**
         * Generate complete preview data for ConfigActivity live preview
         */
        fun generatePreviewData(
            urlPreviewMode: UrlPreviewMode,
            urlParser: UrlParser
        ): PreviewData {
            val originalUrl = SAMPLE_URL
            val cleanedUrl = "https://user:pass@example.com/path?normal_param=1234"
            
            // Parse the URL to get parts
            val urlParts = when (val result = urlParser.parse(originalUrl)) {
                is com.gologlu.detracktor.domain.error.DomainResult.Success -> result.value
                is com.gologlu.detracktor.domain.error.DomainResult.Failure -> {
                    // Fallback to mock data if parsing fails
                    createMockUrlParts()
                }
            }
            
            val tokenEffects = createMockTokenEffects(urlParts)
            val matchedRules = createMockRuleMatches()
            val warningData = createMockWarningData(urlParts)
            
            return PreviewData(
                originalUrl = originalUrl,
                cleanedUrl = cleanedUrl,
                matchedRules = matchedRules,
                warningData = warningData,
                hasChanges = true
            )
        }
        
        /**
         * Create mock token effects showing which parameters would be removed
         */
        fun createMockTokenEffects(urlParts: UrlParts): List<TokenEffect> {
            val tokens = urlParts.queryPairs.getTokens()
            return tokens.mapIndexed { index, token ->
                val willBeRemoved = when (token.decodedKey) {
                    "token", "tracker_param" -> true
                    else -> false
                }
                
                TokenEffect(
                    tokenIndex = index,
                    name = token.decodedKey,
                    willBeRemoved = willBeRemoved,
                    matchedRuleIndexes = if (willBeRemoved) listOf(0) else emptyList(),
                    matchedPatternsByRule = if (willBeRemoved) mapOf(0 to listOf("*")) else emptyMap()
                )
            }
        }
        
        /**
         * Create mock warning data showing embedded credentials
         */
        fun createMockWarningData(urlParts: UrlParts): WarningDisplayData {
            return WarningDisplayData(
                hasCredentials = !urlParts.userInfo.isNullOrEmpty(),
                sensitiveParams = listOf("token"),
                isExpanded = false
            )
        }
        
        /**
         * Create mock rule matches for preview
         */
        private fun createMockRuleMatches(): List<RuleMatchSummary> {
            return listOf(
                RuleMatchSummary(
                    description = "Tracking parameter removal rule",
                    matchedParams = listOf("token", "tracker_param"),
                    domain = "example.com"
                )
            )
        }
        
        /**
         * Create mock URL parts when parsing fails
         */
        private fun createMockUrlParts(): UrlParts {
            // This would need to be implemented based on your UrlParts structure
            // For now, returning a basic structure - you may need to adjust this
            return UrlParts(
                scheme = "https",
                userInfo = "user:pass",
                host = "example.com",
                port = null,
                path = "/path",
                queryPairs = createMockQueryPairs(),
                fragment = null
            )
        }
        
        /**
         * Create mock query pairs for the sample URL
         */
        private fun createMockQueryPairs(): com.gologlu.detracktor.domain.model.QueryPairs {
            // This would need to be implemented based on your QueryPairs structure
            // You may need to adjust this based on how QueryPairs is constructed
            return com.gologlu.detracktor.domain.model.QueryPairs.from("token=abc&normal_param=1234&tracker_param=5678")
        }
        
        /**
         * Generate preview data with different scenarios for testing
         */
        fun generateScenarioPreviewData(scenario: PreviewScenario): PreviewData {
            return when (scenario) {
                PreviewScenario.NO_MATCHES -> PreviewData(
                    originalUrl = "https://clean-site.com/page?id=123",
                    cleanedUrl = "https://clean-site.com/page?id=123",
                    matchedRules = emptyList(),
                    warningData = WarningDisplayData(false, emptyList()),
                    hasChanges = false
                )
                
                PreviewScenario.WITH_TRACKING -> PreviewData(
                    originalUrl = "https://example.com/page?utm_source=google&utm_medium=cpc&id=123",
                    cleanedUrl = "https://example.com/page?id=123",
                    matchedRules = listOf(
                        RuleMatchSummary(
                            description = "UTM parameter removal",
                            matchedParams = listOf("utm_source", "utm_medium"),
                            domain = "example.com"
                        )
                    ),
                    warningData = WarningDisplayData(false, emptyList()),
                    hasChanges = true
                )
                
                PreviewScenario.WITH_CREDENTIALS -> PreviewData(
                    originalUrl = SAMPLE_URL,
                    cleanedUrl = "https://user:pass@example.com/path?normal_param=1234",
                    matchedRules = listOf(
                        RuleMatchSummary(
                            description = "Token removal rule",
                            matchedParams = listOf("token", "tracker_param"),
                            domain = "example.com"
                        )
                    ),
                    warningData = WarningDisplayData(
                        hasCredentials = true,
                        sensitiveParams = listOf("token")
                    ),
                    hasChanges = true
                )
                
                PreviewScenario.MULTIPLE_RULES -> PreviewData(
                    originalUrl = "https://shop.example.com/product?utm_campaign=sale&gclid=abc123&fbclid=def456&id=789",
                    cleanedUrl = "https://shop.example.com/product?id=789",
                    matchedRules = listOf(
                        RuleMatchSummary(
                            description = "UTM tracking removal",
                            matchedParams = listOf("utm_campaign"),
                            domain = "shop.example.com"
                        ),
                        RuleMatchSummary(
                            description = "Ad platform tracking removal",
                            matchedParams = listOf("gclid", "fbclid"),
                            domain = "shop.example.com"
                        )
                    ),
                    warningData = WarningDisplayData(false, emptyList()),
                    hasChanges = true
                )
            }
        }
    }
}

/**
 * Different preview scenarios for testing and demonstration
 */
enum class PreviewScenario {
    /** Clean URL with no tracking parameters */
    NO_MATCHES,
    /** URL with UTM tracking parameters */
    WITH_TRACKING,
    /** URL with embedded credentials and sensitive parameters */
    WITH_CREDENTIALS,
    /** URL matching multiple rules */
    MULTIPLE_RULES
}
