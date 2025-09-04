package com.gologlu.detracktor.domain.error

import org.junit.Test
import org.junit.Assert.*

class ValidationErrorTest {

    @Test
    fun `InvalidUrl should create with default message`() {
        val error = ValidationError.InvalidUrl()
        
        assertEquals("Should have default message", "Invalid or malformed URL", error.message)
        assertNull("Should have no cause", error.cause)
    }

    @Test
    fun `InvalidUrl should create with custom message`() {
        val customMessage = "Custom validation error message"
        val error = ValidationError.InvalidUrl(customMessage)
        
        assertEquals("Should have custom message", customMessage, error.message)
        assertNull("Should have no cause", error.cause)
    }

    @Test
    fun `InvalidUrl should implement DomainError interface`() {
        val error = ValidationError.InvalidUrl("Test message")
        
        assertTrue("Should be instance of DomainError", error is DomainError)
        assertEquals("Should expose message through DomainError interface", "Test message", (error as DomainError).message)
        assertNull("Should expose null cause through DomainError interface", (error as DomainError).cause)
    }

    @Test
    fun `InvalidUrl should support equality comparison`() {
        val error1 = ValidationError.InvalidUrl("Same message")
        val error2 = ValidationError.InvalidUrl("Same message")
        val error3 = ValidationError.InvalidUrl("Different message")
        
        assertEquals("Errors with same message should be equal", error1, error2)
        assertNotEquals("Errors with different messages should not be equal", error1, error3)
        assertEquals("Equal errors should have same hashCode", error1.hashCode(), error2.hashCode())
    }

    @Test
    fun `InvalidUrl should have proper toString representation`() {
        val error = ValidationError.InvalidUrl("Test error message")
        val toString = error.toString()
        
        assertTrue("toString should contain class name", toString.contains("InvalidUrl"))
        assertTrue("toString should contain message", toString.contains("Test error message"))
    }

    @Test
    fun `ValidationError should be sealed interface`() {
        val error = ValidationError.InvalidUrl("Test")
        
        assertTrue("Should be instance of ValidationError", error is ValidationError)
        assertTrue("Should be instance of DomainError", error is DomainError)
    }

    @Test
    fun `InvalidUrl should work with DomainResult`() {
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        assertTrue("Result should be failure", result.isFailure)
        assertFalse("Result should not be success", result.isSuccess)
        
        try {
            result.getOrThrow()
            fail("Should have thrown DomainException")
        } catch (e: DomainException) {
            assertEquals("Should wrap the ValidationError", error, e.error)
            assertEquals("Should have correct message", error.message, e.message)
        }
    }

    @Test
    fun `InvalidUrl should work with DomainResult operations`() {
        val error = ValidationError.InvalidUrl("Validation failed")
        val result = DomainResult.failure<Int>(error)
        
        // Test map operation
        val mappedResult = result.map { it * 2 }
        assertTrue("Mapped result should remain failure", mappedResult.isFailure)
        
        // Test flatMap operation
        val flatMappedResult = result.flatMap { DomainResult.success(it * 2) }
        assertTrue("FlatMapped result should remain failure", flatMappedResult.isFailure)
        
        // Test fold operation
        val foldResult = result.fold(
            onSuccess = { "Success: $it" },
            onFailure = { "Error: ${it.message}" }
        )
        assertEquals("Should handle failure case", "Error: Validation failed", foldResult)
        
        // Test getOrElse operation
        val defaultValue = result.getOrElse { 42 }
        assertEquals("Should return default value", 42, defaultValue)
        
        // Test recover operation
        val recoveredValue = result.recover { "Recovered from: ${it.message}" }
        assertEquals("Should recover with transformed value", "Recovered from: Validation failed", recoveredValue)
    }

    @Test
    fun `InvalidUrl should work with onFailure callback`() {
        val error = ValidationError.InvalidUrl("Test failure")
        val result = DomainResult.failure<String>(error)
        
        var callbackExecuted = false
        var capturedError: DomainError? = null
        
        val returnedResult = result.onFailure { domainError ->
            callbackExecuted = true
            capturedError = domainError
        }
        
        assertTrue("Callback should be executed", callbackExecuted)
        assertEquals("Should capture the original error", error, capturedError)
        assertSame("Should return the same result instance", result, returnedResult)
    }

    @Test
    fun `InvalidUrl should not trigger onSuccess callback`() {
        val error = ValidationError.InvalidUrl("Test failure")
        val result = DomainResult.failure<String>(error)
        
        var callbackExecuted = false
        
        val returnedResult = result.onSuccess { 
            callbackExecuted = true
        }
        
        assertFalse("onSuccess callback should not be executed for failure", callbackExecuted)
        assertSame("Should return the same result instance", result, returnedResult)
    }

    @Test
    fun `InvalidUrl should work with getOrNull`() {
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        val value = result.getOrNull()
        assertNull("Should return null for failure", value)
    }

    @Test
    fun `InvalidUrl should work with getOrDefault`() {
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        val value = result.getOrDefault("default value")
        assertEquals("Should return default value", "default value", value)
    }

    @Test
    fun `InvalidUrl should work with exceptionOrNull`() {
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        val exception = result.exceptionOrNull()
        assertNotNull("Should return exception for failure", exception)
        assertEquals("Exception should wrap the error", error, exception?.error)
        assertEquals("Exception should have correct message", error.message, exception?.message)
    }
}
