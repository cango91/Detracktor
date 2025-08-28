package com.gologlu.detracktor.utils

/**
 * Focused embedded credential detection for user safety.
 * Replaces the complex privacy analysis system with essential credential detection only.
 */
class CredentialDetector {
    
    companion object {
        // Common credential patterns in URLs
        private val CREDENTIAL_PATTERNS = listOf(
            // Basic auth pattern: user:pass@host
            Regex("://[^@/]+:[^@/]+@", RegexOption.IGNORE_CASE),
            // API key patterns in parameters
            Regex("[?&](api[_-]?key|apikey|key|token|access[_-]?token|auth[_-]?token)=([^&]+)", RegexOption.IGNORE_CASE),
            // Password patterns in parameters
            Regex("[?&](password|passwd|pwd|pass)=([^&]+)", RegexOption.IGNORE_CASE),
            // Secret patterns in parameters
            Regex("[?&](secret|client[_-]?secret|app[_-]?secret)=([^&]+)", RegexOption.IGNORE_CASE)
        )
        
        // Patterns that look like credentials but are usually safe
        private val FALSE_POSITIVE_PATTERNS = listOf(
            // Common test/demo values
            Regex("(test|demo|example|sample)", RegexOption.IGNORE_CASE),
            // Very short values (likely not real credentials)
            Regex("^.{1,3}$"),
            // Common placeholder values
            Regex("(your[_-]?key|your[_-]?token|placeholder)", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Check if URL contains embedded credentials.
     * Returns true if potentially sensitive credentials are detected.
     */
    fun hasEmbeddedCredentials(url: String): Boolean {
        return CREDENTIAL_PATTERNS.any { pattern ->
            val matches = pattern.findAll(url)
            matches.any { match ->
                // Check if this looks like a real credential (not a false positive)
                val credentialValue = when {
                    match.groups.size > 2 -> match.groups[2]?.value ?: ""
                    else -> match.value
                }
                !isFalsePositive(credentialValue)
            }
        }
    }
    
    /**
     * Extract embedded credentials from URL.
     * Returns pair of (username, password) if basic auth credentials are found.
     */
    fun extractCredentials(url: String): Pair<String, String>? {
        val basicAuthPattern = Regex("://([^@/]+):([^@/]+)@")
        val match = basicAuthPattern.find(url)
        
        return if (match != null && match.groups.size >= 3) {
            val username = match.groups[1]?.value ?: ""
            val password = match.groups[2]?.value ?: ""
            if (username.isNotBlank() && password.isNotBlank() && !isFalsePositive(password)) {
                Pair(username, password)
            } else {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Check if a credential value is likely a false positive.
     */
    private fun isFalsePositive(value: String): Boolean {
        return FALSE_POSITIVE_PATTERNS.any { it.containsMatchIn(value) }
    }
}
