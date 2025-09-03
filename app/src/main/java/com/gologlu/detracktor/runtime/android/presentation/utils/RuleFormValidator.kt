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
        
        // Validate domains
        errors.addAll(validateDomains(formData.domainsInput))
        
        // Validate subdomain mode and subdomains
        errors.addAll(validateSubdomainMode(formData.subdomainMode, formData.subdomainsInput))
        
        // Validate remove patterns
        errors.addAll(validateRemovePatterns(formData.removePatternsInput))
        
        // Validate sensitive parameters
        warnings.addAll(validateSensitiveParams(formData.sensitiveParamsInput))
        
        // Combined constraints for then block:
        val hasRemove = formData.removePatternsInput.isNotBlank()
        val hasWarn = formData.warnOnCredentials || formData.sensitiveParamsInput.isNotBlank()
        if (!hasRemove && !hasWarn) {
            errors.add("Rule must define at least one of removal patterns or warnings")
        } else if (!hasRemove && hasWarn) {
            warnings.add("Warn-only rule: it won't remove any parameters")
        } else if (hasRemove && !hasWarn) {
            // fine; purely removal rule
        }
        
        return RuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate domains input (comma-separated)
     */
    fun validateDomains(domainsInput: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (domainsInput.isBlank()) {
            errors.add("At least one domain is required")
            return errors
        }
        
        val domains = parseCommaSeparatedList(domainsInput)
        domains.forEach { domain ->
            if (!isValidDomainPattern(domain)) {
                errors.add("Invalid domain: $domain")
            }
        }
        
        return errors
    }
    
    /**
     * Validate subdomain mode and subdomains input
     */
    fun validateSubdomainMode(subdomainMode: SubdomainMode, subdomainsInput: String): List<String> {
        val errors = mutableListOf<String>()
        
        when (subdomainMode) {
            SubdomainMode.NONE -> {
                // No validation needed
            }
            SubdomainMode.ANY -> {
                // No validation needed
            }
            SubdomainMode.SPECIFIC_LIST -> {
                if (subdomainsInput.isBlank()) {
                    errors.add("Specific subdomains are required when using specific list mode")
                } else {
                    val subdomains = parseCommaSeparatedList(subdomainsInput)
                    subdomains.forEach { subdomain ->
                        if (subdomain.contains('.')) {
                            errors.add("Subdomain should not contain dots: $subdomain")
                        }
                        if (!isValidSubdomainName(subdomain)) {
                            errors.add("Invalid subdomain name: $subdomain")
                        }
                    }
                }
            }
        }
        
        return errors
    }
    
    /**
     * Validate removal patterns (comma-separated glob patterns)
     */
    fun validateRemovePatterns(patternsInput: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (patternsInput.isBlank()) {
            return errors // Empty is allowed, will be caught as warning in main validation
        }
        
        val patterns = parseCommaSeparatedList(patternsInput)
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
    private fun validateSensitiveParams(paramsInput: String): List<String> {
        val warnings = mutableListOf<String>()
        
        if (paramsInput.isNotBlank()) {
            val params = parseCommaSeparatedList(paramsInput)
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
    
    /**
     * Parse comma-separated list and trim whitespace
     */
    private fun parseCommaSeparatedList(input: String): List<String> {
        return input.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    
    /**
     * Validate subdomain name (no dots, valid characters)
     */
    private fun isValidSubdomainName(subdomain: String): Boolean {
        // Subdomain should be a valid hostname component (no dots)
        val subdomainRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$")
        return subdomain.matches(subdomainRegex)
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
