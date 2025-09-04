package com.gologlu.detracktor.domain.error

/**
 * Represents validation errors that occur when input data doesn't meet expected criteria.
 * These errors typically happen during parsing or validation of user input.
 */
sealed interface ValidationError : DomainError {
    /**
     * Error indicating that a URL string is invalid or malformed.
     * This happens when a URL cannot be parsed or is missing required components.
     *
     * @param message The error message explaining what's wrong with the URL
     */
    data class InvalidUrl(override val message: String = "Invalid or malformed URL") : ValidationError {
        override val cause: Throwable? = null
    }
}