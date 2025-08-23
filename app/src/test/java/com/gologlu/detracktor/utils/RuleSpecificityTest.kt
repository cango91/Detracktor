package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RuleSpecificityTest {

    @Test
    fun testCalculate_withExactDomainRule_returnsHighSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Exact domain rule"
        )

        // When
        val specificity = RuleSpecificity.calculate(rule)

        // Then
        assertTrue(specificity > 0)
        // Exact patterns should have higher specificity than wildcards
        assertTrue(specificity > 100)
    }

    @Test
    fun testCalculate_withSubdomainWildcardRule_returnsMediumSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = "*.example.com",
            params = listOf("utm_*"),
            priority = RulePriority.SUBDOMAIN_WILDCARD,
            patternType = PatternType.WILDCARD,
            enabled = true,
            description = "Subdomain wildcard rule"
        )

        // When
        val specificity = RuleSpecificity.calculate(rule)

        // Then
        assertTrue(specificity > 0)
        // Subdomain wildcards should have medium-high specificity (around 3500+)
        assertTrue(specificity > 3000)
        assertTrue(specificity < 4000)
    }

    @Test
    fun testCalculate_withGlobalWildcardRule_returnsLowSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = "*",
            params = listOf("utm_*", "fbclid"),
            priority = RulePriority.GLOBAL_WILDCARD,
            patternType = PatternType.WILDCARD,
            enabled = true,
            description = "Global wildcard rule"
        )

        // When
        val specificity = RuleSpecificity.calculate(rule)

        // Then
        assertTrue(specificity > 0)
        // Global wildcards should have low specificity (around 100+)
        assertTrue(specificity > 100)
        assertTrue(specificity < 200)
    }

    @Test
    fun testCalculate_withRegexRule_returnsAppropriateSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = ".*\\.example\\.com",
            params = listOf("tracking_.*"),
            priority = RulePriority.SUBDOMAIN_WILDCARD,
            patternType = PatternType.REGEX,
            enabled = true,
            description = "Regex pattern rule"
        )

        // When
        val specificity = RuleSpecificity.calculate(rule)

        // Then
        assertTrue(specificity > 0)
        // Regex patterns should have medium specificity
        assertTrue(specificity > 30)
    }

    @Test
    fun testCalculate_withPathPatternRule_returnsHighSpecificity() {
        // Given
        val rule = CleaningRule(
            hostPattern = "example.com/api/*",
            params = listOf("api_key"),
            priority = RulePriority.PATH_SPECIFIC,
            patternType = PatternType.PATH_PATTERN,
            enabled = true,
            description = "Path pattern rule"
        )

        // When
        val specificity = RuleSpecificity.calculate(rule)

        // Then
        assertTrue(specificity > 0)
        // Path patterns should have high specificity
        assertTrue(specificity > 100)
    }

    @Test
    fun testCalculate_withMoreParamsHasHigherSpecificity() {
        // Given
        val ruleWithFewParams = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Rule with few params"
        )

        val ruleWithManyParams = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium", "utm_campaign", "fbclid", "gclid"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Rule with many params"
        )

        // When
        val specificityFew = RuleSpecificity.calculate(ruleWithFewParams)
        val specificityMany = RuleSpecificity.calculate(ruleWithManyParams)

        // Then
        assertTrue(specificityMany > specificityFew)
    }

    @Test
    fun testCalculate_withLongerHostPatternHasHigherSpecificity() {
        // Given
        val shortHostRule = CleaningRule(
            hostPattern = "a.com",
            params = listOf("utm_source"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Short host rule"
        )

        val longHostRule = CleaningRule(
            hostPattern = "very.long.subdomain.example.com",
            params = listOf("utm_source"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Long host rule"
        )

        // When
        val specificityShort = RuleSpecificity.calculate(shortHostRule)
        val specificityLong = RuleSpecificity.calculate(longHostRule)

        // Then
        assertTrue(specificityLong > specificityShort)
    }

    @Test
    fun testSortBySpecificity_sortsCorrectly() {
        // Given
        val globalRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "*",
                params = listOf("utm_*"),
                priority = RulePriority.GLOBAL_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Global rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "*",
            specificity = RuleSpecificity.calculate(CleaningRule(
                hostPattern = "*",
                params = listOf("utm_*"),
                priority = RulePriority.GLOBAL_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Global rule"
            ))
        )

        val domainRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_source"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Domain rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "example.com",
            specificity = RuleSpecificity.calculate(CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_source"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Domain rule"
            ))
        )

        val subdomainRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "*.example.com",
                params = listOf("fbclid"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Subdomain rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "*.example.com",
            specificity = RuleSpecificity.calculate(CleaningRule(
                hostPattern = "*.example.com",
                params = listOf("fbclid"),
                priority = RulePriority.SUBDOMAIN_WILDCARD,
                patternType = PatternType.WILDCARD,
                enabled = true,
                description = "Subdomain rule"
            ))
        )

        val unsortedRules = listOf(globalRule, domainRule, subdomainRule)

        // When
        val sortedRules = RuleSpecificity.sortBySpecificity(unsortedRules)

        // Then
        assertEquals(3, sortedRules.size)
        // Should be sorted by specificity (highest first)
        assertTrue(sortedRules[0].specificity >= sortedRules[1].specificity)
        assertTrue(sortedRules[1].specificity >= sortedRules[2].specificity)
        
        // Domain-specific rule should be first (highest specificity)
        assertEquals("example.com", sortedRules[0].originalRule.hostPattern)
        // Global wildcard should be last (lowest specificity)
        assertEquals("*", sortedRules[2].originalRule.hostPattern)
    }

    @Test
    fun testCalculate_withDifferentPriorities_reflectsInSpecificity() {
        // Given
        val highPriorityRule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "High priority rule"
        )

        val lowPriorityRule = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source"),
            priority = RulePriority.GLOBAL_WILDCARD,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Low priority rule"
        )

        // When
        val highSpecificity = RuleSpecificity.calculate(highPriorityRule)
        val lowSpecificity = RuleSpecificity.calculate(lowPriorityRule)

        // Then
        assertTrue(highSpecificity > lowSpecificity)
    }

    @Test
    fun testCalculate_withSameRules_returnsSameSpecificity() {
        // Given
        val rule1 = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Test rule 1"
        )

        val rule2 = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Test rule 2"
        )

        // When
        val specificity1 = RuleSpecificity.calculate(rule1)
        val specificity2 = RuleSpecificity.calculate(rule2)

        // Then
        assertEquals(specificity1, specificity2)
    }

    @Test
    fun testCalculate_withEmptyParams_returnsLowerSpecificity() {
        // Given
        val ruleWithParams = CleaningRule(
            hostPattern = "example.com",
            params = listOf("utm_source", "utm_medium"),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Rule with params"
        )

        val ruleWithoutParams = CleaningRule(
            hostPattern = "example.com",
            params = emptyList(),
                            priority = RulePriority.EXACT_HOST,
            patternType = PatternType.EXACT,
            enabled = true,
            description = "Rule without params"
        )

        // When
        val specificityWithParams = RuleSpecificity.calculate(ruleWithParams)
        val specificityWithoutParams = RuleSpecificity.calculate(ruleWithoutParams)

        // Then
        assertTrue(specificityWithParams > specificityWithoutParams)
    }

    @Test
    fun testSortBySpecificity_withEmptyList_returnsEmptyList() {
        // Given
        val emptyList = emptyList<CompiledRule>()

        // When
        val result = RuleSpecificity.sortBySpecificity(emptyList)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSortBySpecificity_withSingleRule_returnsSameRule() {
        // Given
        val singleRule = CompiledRule(
            originalRule = CleaningRule(
                hostPattern = "example.com",
                params = listOf("utm_source"),
                priority = RulePriority.EXACT_HOST,
                patternType = PatternType.EXACT,
                enabled = true,
                description = "Single rule"
            ),
            compiledHostPattern = null,
            compiledParamPatterns = emptyList(),
            normalizedHostPattern = "example.com",
            specificity = 100
        )
        val singleRuleList = listOf(singleRule)

        // When
        val result = RuleSpecificity.sortBySpecificity(singleRuleList)

        // Then
        assertEquals(1, result.size)
        assertEquals(singleRule, result[0])
    }
}
