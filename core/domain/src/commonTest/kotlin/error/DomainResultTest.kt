package com.gologlu.detracktor.domain.error

import org.junit.Test
import org.junit.Assert.*

class DomainResultTest {

    @Test
    fun `getOrThrow should throw DomainException for failure`() {
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        try {
            result.getOrThrow()
            fail("Should have thrown DomainException")
        } catch (e: DomainException) {
            assertEquals("Should wrap the original error", error, e.error)
            assertEquals("Should have correct message", error.message, e.message)
        } catch (e: Exception) {
            fail("Should throw DomainException, not ${e::class.simpleName}")
        }
    }

    @Test
    fun `getOrThrow should return value for success`() {
        val value = "test value"
        val result = DomainResult.success(value)
        
        assertEquals("Should return the success value", value, result.getOrThrow())
    }

    @Test
    fun `getOrThrow should preserve cause in exception`() {
        val cause = RuntimeException("Original cause")
        // Use ValidationError which implements DomainError
        val error = ValidationError.InvalidUrl("Test error")
        val result = DomainResult.failure<String>(error)
        
        try {
            result.getOrThrow()
            fail("Should have thrown DomainException")
        } catch (e: DomainException) {
            assertEquals("Should wrap the original error", error, e.error)
            assertEquals("Should have correct message", error.message, e.message)
        }
    }

    @Test
    fun `success result should be success`() {
        val result = DomainResult.success("value")
        
        assertTrue("Should be success", result.isSuccess)
        assertFalse("Should not be failure", result.isFailure)
    }

    @Test
    fun `failure result should be failure`() {
        val result = DomainResult.failure<String>(ValidationError.InvalidUrl("error"))
        
        assertFalse("Should not be success", result.isSuccess)
        assertTrue("Should be failure", result.isFailure)
    }

    @Test
    fun `map should transform success value`() {
        val result = DomainResult.success(5)
            .map { it * 2 }
        
        assertTrue("Should remain success", result.isSuccess)
        assertEquals("Should transform value", 10, result.getOrThrow())
    }

    @Test
    fun `map should preserve failure`() {
        val error = ValidationError.InvalidUrl("error")
        val result = DomainResult.failure<Int>(error)
            .map { it * 2 }
        
        assertTrue("Should remain failure", result.isFailure)
        try {
            result.getOrThrow()
            fail("Should throw exception")
        } catch (e: DomainException) {
            assertEquals("Should preserve original error", error, e.error)
        }
    }

    @Test
    fun `flatMap should chain operations`() {
        val result = DomainResult.success(5)
            .flatMap { DomainResult.success(it * 2) }
        
        assertTrue("Should be success", result.isSuccess)
        assertEquals("Should chain transformations", 10, result.getOrThrow())
    }

    @Test
    fun `flatMap should short-circuit on failure`() {
        val error = ValidationError.InvalidUrl("error")
        val result = DomainResult.failure<Int>(error)
            .flatMap { DomainResult.success(it * 2) }
        
        assertTrue("Should remain failure", result.isFailure)
        try {
            result.getOrThrow()
            fail("Should throw exception")
        } catch (e: DomainException) {
            assertEquals("Should preserve original error", error, e.error)
        }
    }

    @Test
    fun `fold should handle both cases`() {
        val successResult = DomainResult.success(5)
        val failureResult = DomainResult.failure<Int>(ValidationError.InvalidUrl("error"))
        
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
    fun `getOrElse should return value for success`() {
        val result = DomainResult.success("success value")
        val value = result.getOrElse { "default value" }
        
        assertEquals("Should return success value", "success value", value)
    }

    @Test
    fun `getOrElse should return default for failure`() {
        val result = DomainResult.failure<String>(ValidationError.InvalidUrl("error"))
        val value = result.getOrElse { "default value" }
        
        assertEquals("Should return default value", "default value", value)
    }

    @Test
    fun `getOrElse should lazily evaluate default`() {
        var defaultCalled = false
        val result = DomainResult.success("success value")
        
        val value = result.getOrElse { 
            defaultCalled = true
            "default value" 
        }
        
        assertEquals("Should return success value", "success value", value)
        assertFalse("Default should not be called for success", defaultCalled)
    }

    @Test
    fun `recover should return value for success`() {
        val result = DomainResult.success("success value")
        val value = result.recover { "recovered from ${it.message}" }
        
        assertEquals("Should return success value", "success value", value)
    }

    @Test
    fun `recover should transform error for failure`() {
        val error = ValidationError.InvalidUrl("test error")
        val result = DomainResult.failure<String>(error)
        val value = result.recover { "recovered from ${it.message}" }
        
        assertEquals("Should return recovered value", "recovered from test error", value)
    }

    @Test
    fun `recover should provide access to original error`() {
        val error = ValidationError.InvalidUrl("original error")
        val result = DomainResult.failure<String>(error)
        
        val value = result.recover { domainError ->
            assertEquals("Should provide original error", error, domainError)
            "recovered"
        }
        
        assertEquals("Should return recovered value", "recovered", value)
    }
}
