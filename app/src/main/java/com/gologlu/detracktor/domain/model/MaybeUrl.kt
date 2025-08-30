package com.gologlu.detracktor.domain.model

/**
 * Type alias for a string that might represent a valid URL.
 * This is used for raw URL input before validation - it may or may not be a valid URL.
 * Use [Url.from] to validate and convert to a proper [Url] instance.
 */
typealias MaybeUrl = String