package com.gologlu.detracktor.utils

import com.gologlu.detracktor.data.CompiledRule
import com.gologlu.detracktor.data.CleaningRule
import com.gologlu.detracktor.data.PatternType

/**
 * Rule specificity calculator for hierarchical matching.
 * Calculates specificity scores to determine which rules should take precedence.
 */
class RuleSpecificity {
    
    companion object {
        // Base specificity weights
        private const val PRIORITY_WEIGHT = 1000
        private const val PATTERN_TYPE_WEIGHT = 100
        private const val HOST_LENGTH_WEIGHT = 10
        private const val PARAM_COUNT_WEIGHT = 1
        
        /**
         * Calculate specificity score for a rule.
         * Higher scores indicate more specific rules that should take precedence.
         */
        fun calculate(rule: CleaningRule): Int {
            var specificity = 0
            
            // Priority level (lower level = higher specificity)
            specificity += (5 - rule.priority.level) * PRIORITY_WEIGHT
            
            // Pattern type specificity
            specificity += getPatternTypeSpecificity(rule.patternType) * PATTERN_TYPE_WEIGHT
            
            // Host pattern length (longer patterns are more specific)
            specificity += rule.hostPattern.length * HOST_LENGTH_WEIGHT
            
            // Parameter count (more parameters = more specific)
            specificity += rule.params.size * PARAM_COUNT_WEIGHT
            
            // Additional specificity bonuses
            specificity += calculateHostPatternBonus(rule.hostPattern)
            
            return specificity
        }
        
        /**
         * Compare two compiled rules by specificity.
         * Returns negative if rule1 is less specific, positive if more specific, 0 if equal.
         */
        fun compare(rule1: CompiledRule, rule2: CompiledRule): Int {
            return rule1.specificity.compareTo(rule2.specificity)
        }
        
        /**
         * Sort rules by specificity in descending order (most specific first).
         */
        fun sortBySpecificity(rules: List<CompiledRule>): List<CompiledRule> {
            return rules.sortedByDescending { it.specificity }
        }
        
        /**
         * Get specificity score for pattern type.
         */
        private fun getPatternTypeSpecificity(patternType: PatternType): Int {
            return when (patternType) {
                PatternType.EXACT -> 4          // Most specific
                PatternType.PATH_PATTERN -> 3   // Path-specific patterns
                PatternType.REGEX -> 2          // Regex patterns
                PatternType.WILDCARD -> 1       // Least specific
            }
        }
        
        /**
         * Calculate additional specificity bonus based on host pattern characteristics.
         */
        private fun calculateHostPatternBonus(hostPattern: String): Int {
            var bonus = 0
            
            // Exact domain match (no wildcards) gets highest bonus
            if (!hostPattern.contains("*") && !hostPattern.contains("?")) {
                bonus += 500
            }
            
            // Subdomain wildcards are more specific than global wildcards
            if (hostPattern.startsWith("*.") && hostPattern.count { it == '*' } == 1) {
                bonus += 300
            }
            
            // Path-specific patterns get bonus
            if (hostPattern.contains("/")) {
                bonus += 200
                // More path segments = more specific
                bonus += hostPattern.count { it == '/' } * 50
            }
            
            // Domain depth bonus (more dots = more specific subdomain)
            val domainDepth = hostPattern.count { it == '.' }
            bonus += domainDepth * 25
            
            // Penalize global wildcards
            if (hostPattern == "*" || hostPattern == "*.*") {
                bonus -= 1000
            }
            
            // Penalize multiple wildcards (less specific)
            val wildcardCount = hostPattern.count { it == '*' }
            if (wildcardCount > 1) {
                bonus -= wildcardCount * 100
            }
            
            return bonus
        }
        
        /**
         * Determine if one rule is more specific than another for the same host.
         */
        fun isMoreSpecific(rule1: CleaningRule, rule2: CleaningRule): Boolean {
            return calculate(rule1) > calculate(rule2)
        }
        
        /**
         * Find the most specific rule from a list of rules.
         */
        fun findMostSpecific(rules: List<CleaningRule>): CleaningRule? {
            return rules.maxByOrNull { calculate(it) }
        }
        
        /**
         * Group rules by specificity level for debugging and analysis.
         */
        fun groupBySpecificityLevel(rules: List<CompiledRule>): Map<String, List<CompiledRule>> {
            return rules.groupBy { rule ->
                when {
                    rule.specificity >= 4000 -> "Exact Host"
                    rule.specificity >= 3000 -> "Subdomain Wildcard"
                    rule.specificity >= 2000 -> "Path Specific"
                    rule.specificity >= 1000 -> "Pattern Based"
                    else -> "Global Wildcard"
                }
            }
        }
        
        /**
         * Validate that rule specificity ordering is correct.
         * Returns true if rules are properly ordered by specificity.
         */
        fun validateOrdering(rules: List<CompiledRule>): Boolean {
            for (i in 0 until rules.size - 1) {
                if (rules[i].specificity < rules[i + 1].specificity) {
                    return false
                }
            }
            return true
        }
    }
}
