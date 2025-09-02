package com.gologlu.detracktor.runtime.android.presentation.utils

import com.gologlu.detracktor.application.service.globby.Globby
import com.gologlu.detracktor.runtime.android.presentation.types.RuleEditFormData
import com.gologlu.detracktor.runtime.android.presentation.types.RuleValidationResult
import com.gologlu.detracktor.runtime.android.presentation.types.SubdomainMode

/**
 * Validates rule form data and provides detailed error messages
 */
class RuleFormValidator {
    
    /**
     * Validate complete form data
     */
    fun validateComplete(formData: RuleEditFormData): RuleValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate host pattern
        errors.addAll(validateHostPattern(formData.hostPattern, formData.subdomainMode))
        
        // Validate remove patterns
        errors.addAll(validateRemovePatterns(formData.removePatterns))
        
        // Validate schemes
        errors.addAll(validateSchemes(formData.schemes))
        
        // Validate sensitive parameters
        warnings.addAll(validateSensitiveParams(formData.sensitiveParams))
        
        // Check for empty rule (no remove patterns)
        if (formData.removePatterns.all { it.isBlank() }) {
            warnings.add("Rule has no removal patterns - it won't remove any parameters")
        }
        
        return RuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate host pattern based on subdomain mode
     */
    fun validateHostPattern(pattern: String, subdomainMode: SubdomainMode): List<String> {
        val errors = mutableListOf<String>()
        
        when (subdomainMode) {
            SubdomainMode.ANY -> {
                // Global wildcard - no specific validation needed
                if (pattern.isNotBlank() && pattern != "*") {
                    errors.add("Global wildcard mode should use '*' or leave empty")
                }
            }
            SubdomainMode.EXACT, SubdomainMode.WILDCARD -> {
                if (pattern.isBlank()) {
                    errors.add("Host pattern is required")
                } else {
                    // Basic domain validation
                    if (!isValidDomainPattern(pattern)) {
                        errors.add("Invalid domain pattern: $pattern")
                    }
                }
            }
        }
        
        return errors
    }
    
    /**
     * Validate removal patterns (glob patterns)
     */
    fun validateRemovePatterns(patterns: List<String>): List<String> {
        val errors = mutableListOf<String>()
        
        patterns.forEachIndexed { index, pattern ->
            if (pattern.isNotBlank()) {
                try {
                    Globby.requireValid(pattern, "remove pattern")
                } catch (e: Exception) {
                    errors.add("Invalid removal pattern at position ${index + 1}: ${e.message}")
                }
            }
        }
        
        return errors
    }
    
    /**
     * Validate URL schemes
     */
    fun validateSchemes(schemes: List<String>): List<String> {
        val errors = mutableListOf<String>()
        val validSchemes = setOf("http", "https", "ftp", "ftps")
        
        if (schemes.isEmpty()) {
            errors.add("At least one scheme must be specified")
        }
        
        schemes.forEach { scheme ->
            if (scheme.isBlank()) {
                errors.add("Empty scheme not allowed")
            } else if (scheme !in validSchemes) {
                errors.add("Unsupported scheme: $scheme")
            }
        }
        
        return errors
    }
    
    /**
     * Validate sensitive parameters (warnings only)
     */
    private fun validateSensitiveParams(params: List<String>): List<String> {
        val warnings = mutableListOf<String>()
        
        if (params.isNotEmpty()) {
            val duplicates = params.groupBy { it }.filter { it.value.size > 1 }.keys
            if (duplicates.isNotEmpty()) {
                warnings.add("Duplicate sensitive parameters: ${duplicates.joinToString(", ")}")
            }
        }
        
        return warnings
    }
    
    /**
     * Basic domain pattern validation
     */
    private fun isValidDomainPattern(pattern: String): Boolean {
        // Allow basic domain patterns like example.com, sub.example.com
        // This is a simplified validation - could be enhanced
        val domainRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$")
        return pattern.matches(domainRegex) || pattern == "*"
    }
    
    companion object {
        /**
         * Common parameter patterns for quick selection
         */
        val COMMON_TRACKING_PATTERNS = listOf(
            "utm_*",
            "gclid",
            "fbclid",
            "msclkid",
            "_ga",
            "_gl",
            "mc_*",
            "campaign_*",
            "source",
            "medium",
            "ref",
            "referrer"
        )
        
        /**
         * Common sensitive parameters
         */
        val COMMON_SENSITIVE_PARAMS = listOf(
            "password",
            "pwd",
            "pass",
            "token",
            "key",
            "secret",
            "auth",
            "session",
            "sid",
            "api_key",
            "access_token"
        )
    }
}
