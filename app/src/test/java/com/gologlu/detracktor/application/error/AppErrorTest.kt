package com.gologlu.detracktor.application.error

import com.gologlu.detracktor.domain.error.DomainError
import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.error.ValidationError
import org.junit.Test
import org.junit.Assert.*

class AppErrorTest {

    @Test
    fun `DomainViolation should wrap domain error`() {
        val domainError = ValidationError.InvalidUrl("Test domain error")
        val appError = DomainViolation(domainError)
        
        assertEquals("Should have domain error message", domainError.message, appError.message)
        assertEquals("Should have domain error cause", domainError.cause, appError.cause)
        assertEquals("Should wrap the domain error", domainError, appError.domain)
    }

    @Test
    fun `AppValidationError should create with field path`() {
        val message = "Field validation failed"
        val fieldPath = "user.email"
        val cause = RuntimeException("Underlying cause")
        val error = AppValidationError(message, fieldPath, cause)
        
        assertEquals("Should have correct message", message, error.message)
        assertEquals("Should have correct field path", fieldPath, error.fieldPath)
        assertEquals("Should have correct cause", cause, error.cause)
    }

    @Test
    fun `AppValidationError should create without cause`() {
        val message = "Field validation failed"
        val fieldPath = "user.name"
        val error = AppValidationError(message, fieldPath)
        
        assertEquals("Should have correct message", message, error.message)
        assertEquals("Should have correct field path", fieldPath, error.fieldPath)
        assertNull("Should have no cause", error.cause)
    }

    @Test
    fun `Violation should create with path and message`() {
        val path = "sites[0].when.host.domains[1]"
        val message = "Invalid domain format"
        val violation = Violation(path, message)
        
        assertEquals("Should have correct path", path, violation.path)
        assertEquals("Should have correct message", message, violation.message)
    }

    @Test
    fun `RulesValidationError should create with empty violations`() {
        val error = RulesValidationError(emptyList())
        
        assertTrue("Should have empty violations", error.violations.isEmpty())
        assertEquals("Should have default message", "Rules validation failed with no details.", error.message)
    }

    @Test
    fun `RulesValidationError should create with single violation`() {
        val violation = Violation("sites[0].host", "Invalid host pattern")
        val error = RulesValidationError(listOf(violation))
        
        assertEquals("Should have one violation", 1, error.violations.size)
        assertEquals("Should contain violation", violation, error.violations[0])
        
        val expectedMessage = "Rules validation failed:\n - sites[0].host: Invalid host pattern"
        assertEquals("Should format message correctly", expectedMessage, error.message)
    }

    @Test
    fun `RulesValidationError should create with multiple violations`() {
        val violations = listOf(
            Violation("sites[0].host", "Invalid host pattern"),
            Violation("sites[1].when.path", "Invalid path regex"),
            Violation("settings.timeout", "Timeout must be positive")
        )
        val error = RulesValidationError(violations)
        
        assertEquals("Should have three violations", 3, error.violations.size)
        assertEquals("Should contain all violations", violations, error.violations)
        
        val expectedMessage = """Rules validation failed:
 - sites[0].host: Invalid host pattern
 - sites[1].when.path: Invalid path regex
 - settings.timeout: Timeout must be positive"""
        assertEquals("Should format message correctly", expectedMessage, error.message)
    }

    @Test
    fun `ConfigParseError should create with message and cause`() {
        val message = "JSON parsing failed"
        val cause = RuntimeException("Invalid JSON syntax")
        val error = ConfigParseError(message, cause)
        
        assertEquals("Should have correct message", message, error.message)
        assertEquals("Should have correct cause", cause, error.cause)
        assertTrue("Should be AppConfigError", error is AppConfigError)
    }

    @Test
    fun `ConfigParseError should create without cause`() {
        val message = "JSON parsing failed"
        val error = ConfigParseError(message)
        
        assertEquals("Should have correct message", message, error.message)
        assertNull("Should have no cause", error.cause)
    }

    @Test
    fun `ConfigInvalidFieldError should create with path and message`() {
        val path = "settings.rules[0].pattern"
        val message = "Invalid regex pattern"
        val cause = RuntimeException("Regex compilation failed")
        val error = ConfigInvalidFieldError(path, message, cause)
        
        assertEquals("Should have correct path", path, error.path)
        assertEquals("Should have correct message", message, error.message)
        assertEquals("Should have correct cause", cause, error.cause)
        assertTrue("Should be AppConfigError", error is AppConfigError)
    }

    @Test
    fun `CanonicalizationError should create with message and cause`() {
        val message = "URL canonicalization failed"
        val cause = RuntimeException("Invalid URL format")
        val error = CanonicalizationError(message, cause)
        
        assertEquals("Should have correct message", message, error.message)
        assertEquals("Should have correct cause", cause, error.cause)
    }

    @Test
    fun `AppException should wrap AppError`() {
        val appError = AppValidationError("Test error", "field.path")
        val exception = AppException(appError)
        
        assertEquals("Should wrap the app error", appError, exception.error)
        assertEquals("Should have error message", appError.message, exception.message)
        assertEquals("Should have error cause", appError.cause, exception.cause)
    }

    @Test
    fun `RulesValidationException should wrap RulesValidationError`() {
        val violations = listOf(Violation("path", "message"))
        val error = RulesValidationError(violations)
        val exception = RulesValidationException(error)
        
        assertEquals("Should wrap the rules validation error", error, exception.err)
        assertEquals("Should wrap as app error", error, exception.error)
        assertTrue("Should be AppException", exception is AppException)
    }

    @Test
    fun `AppValidationException should wrap AppValidationError`() {
        val error = AppValidationError("Test error", "field.path")
        val exception = AppValidationException(error)
        
        assertEquals("Should wrap the app validation error", error, exception.err)
        assertEquals("Should wrap as app error", error, exception.error)
        assertTrue("Should be AppException", exception is AppException)
    }

    @Test
    fun `AppConfigException should wrap AppConfigError`() {
        val error = ConfigParseError("Parse error")
        val exception = AppConfigException(error)
        
        assertEquals("Should wrap the app config error", error, exception.err)
        assertEquals("Should wrap as app error", error, exception.error)
        assertTrue("Should be AppException", exception is AppException)
    }

    @Test
    fun `requireValid should add violation when condition is false`() {
        val violations = mutableListOf<Violation>()
        
        violations.requireValid(false, "test.path") { "Test violation message" }
        
        assertEquals("Should add one violation", 1, violations.size)
        assertEquals("Should have correct path", "test.path", violations[0].path)
        assertEquals("Should have correct message", "Test violation message", violations[0].message)
    }

    @Test
    fun `requireValid should not add violation when condition is true`() {
        val violations = mutableListOf<Violation>()
        
        violations.requireValid(true, "test.path") { "Test violation message" }
        
        assertTrue("Should not add any violations", violations.isEmpty())
    }

    @Test
    fun `requireValid should support lazy message evaluation`() {
        val violations = mutableListOf<Violation>()
        var messageEvaluated = false
        
        violations.requireValid(true, "test.path") { 
            messageEvaluated = true
            "Test violation message" 
        }
        
        assertTrue("Should not add any violations", violations.isEmpty())
        assertFalse("Message should not be evaluated when condition is true", messageEvaluated)
    }

    @Test
    fun `AppResult Success should create and behave correctly`() {
        val value = "test value"
        val result = AppResult.success(value)
        
        assertTrue("Should be success", result.isSuccess)
        assertFalse("Should not be failure", result.isFailure)
        assertEquals("Should return value", value, result.getOrThrow())
        assertEquals("Should return value with getOrNull", value, result.getOrNull())
    }

    @Test
    fun `AppResult Failure should create and behave correctly`() {
        val error = AppValidationError("Test error", "field.path")
        val result = AppResult.failure<String>(error)
        
        assertFalse("Should not be success", result.isSuccess)
        assertTrue("Should be failure", result.isFailure)
        assertNull("Should return null with getOrNull", result.getOrNull())
        
        try {
            result.getOrThrow()
            fail("Should have thrown AppException")
        } catch (e: AppException) {
            assertEquals("Should wrap the app error", error, e.error)
        }
    }

    @Test
    fun `AppResult map should transform success value`() {
        val result = AppResult.success(5)
            .map { it * 2 }
        
        assertTrue("Should remain success", result.isSuccess)
        assertEquals("Should transform value", 10, result.getOrThrow())
    }

    @Test
    fun `AppResult map should preserve failure`() {
        val error = AppValidationError("Test error", "field.path")
        val result = AppResult.failure<Int>(error)
            .map { it * 2 }
        
        assertTrue("Should remain failure", result.isFailure)
        try {
            result.getOrThrow()
            fail("Should throw exception")
        } catch (e: AppException) {
            assertEquals("Should preserve original error", error, e.error)
        }
    }

    @Test
    fun `AppResult flatMap should chain operations`() {
        val result = AppResult.success(5)
            .flatMap { AppResult.success(it * 2) }
        
        assertTrue("Should be success", result.isSuccess)
        assertEquals("Should chain transformations", 10, result.getOrThrow())
    }

    @Test
    fun `AppResult flatMap should short-circuit on failure`() {
        val error = AppValidationError("Test error", "field.path")
        val result = AppResult.failure<Int>(error)
            .flatMap { AppResult.success(it * 2) }
        
        assertTrue("Should remain failure", result.isFailure)
        try {
            result.getOrThrow()
            fail("Should throw exception")
        } catch (e: AppException) {
            assertEquals("Should preserve original error", error, e.error)
        }
    }

    @Test
    fun `AppResult fold should handle both cases`() {
        val successResult = AppResult.success(5)
        val failureResult = AppResult.failure<Int>(AppValidationError("error", "path"))
        
        val successValue = successResult.fold(
            onSuccess = { "Success: $it" },
            onFailure = { "Failure: ${it.message}" }
        )
        
        val failureValue = failureResult.fold(
            onSuccess = { "Success: $it" },
            onFailure = { "Failure: ${it.message}" }
        )
        
        assertEquals("Success: 5", successValue)
        assertEquals("Failure: error", failureValue)
    }

    @Test
    fun `AppResult getOrElse should work correctly`() {
        val successResult = AppResult.success("success value")
        val failureResult = AppResult.failure<String>(AppValidationError("error", "path"))
        
        assertEquals("Should return success value", "success value", successResult.getOrElse { "default" })
        assertEquals("Should return default for failure", "default", failureResult.getOrElse { "default" })
    }

    @Test
    fun `DomainResult toAppResult should convert correctly`() {
        val domainError = ValidationError.InvalidUrl("Test error")
        val successResult = DomainResult.success("test value")
        val failureResult = DomainResult.failure<String>(domainError)
        
        val appSuccessResult = successResult.toAppResult()
        val appFailureResult = failureResult.toAppResult()
        
        assertTrue("Should convert success", appSuccessResult.isSuccess)
        assertEquals("Should preserve success value", "test value", appSuccessResult.getOrThrow())
        
        assertTrue("Should convert failure", appFailureResult.isFailure)
        try {
            appFailureResult.getOrThrow()
            fail("Should throw AppException")
        } catch (e: AppException) {
            assertTrue("Should wrap as DomainViolation", e.error is DomainViolation)
            assertEquals("Should preserve domain error", domainError, (e.error as DomainViolation).domain)
        }
    }

    @Test
    fun `DomainError toAppError should convert correctly`() {
        val domainError = ValidationError.InvalidUrl("Test error")
        val appError = domainError.toAppError()
        
        assertTrue("Should be DomainViolation", appError is DomainViolation)
        assertEquals("Should wrap domain error", domainError, (appError as DomainViolation).domain)
        assertEquals("Should preserve message", domainError.message, appError.message)
        assertEquals("Should preserve cause", domainError.cause, appError.cause)
    }

    @Test
    fun `DomainResult getOrThrowApp should work correctly`() {
        val successResult = DomainResult.success("test value")
        val failureResult = DomainResult.failure<String>(ValidationError.InvalidUrl("Test error"))
        
        assertEquals("Should return success value", "test value", successResult.getOrThrowApp())
        
        try {
            failureResult.getOrThrowApp()
            fail("Should throw AppException")
        } catch (e: AppException) {
            assertTrue("Should wrap as DomainViolation", e.error is DomainViolation)
            assertTrue("Should preserve ValidationError", (e.error as DomainViolation).domain is ValidationError.InvalidUrl)
        }
    }
}
