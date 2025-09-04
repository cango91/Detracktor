package com.gologlu.detracktor.runtime.android.test

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Turbine-based flow testing utilities for reactive state management testing.
 * Provides comprehensive utilities for testing Kotlin coroutines and flows in the runtime package.
 */
object FlowTestUtils {
    
    /**
     * Default timeout for flow testing operations
     */
    val DEFAULT_TIMEOUT: Duration = 5.seconds
    
    /**
     * Test a flow with a single expected emission
     */
    suspend fun <T> Flow<T>.testSingleEmission(
        timeout: Duration = DEFAULT_TIMEOUT,
        expectedValue: T
    ) {
        test(timeout = timeout) {
            val emission = awaitItem()
            assert(emission == expectedValue) {
                "Expected $expectedValue but got $emission"
            }
            awaitComplete()
        }
    }
    
    /**
     * Test a flow with multiple expected emissions in order
     */
    suspend fun <T> Flow<T>.testEmissions(
        timeout: Duration = DEFAULT_TIMEOUT,
        vararg expectedValues: T
    ) {
        test(timeout = timeout) {
            expectedValues.forEach { expected ->
                val emission = awaitItem()
                assert(emission == expected) {
                    "Expected $expected but got $emission"
                }
            }
            awaitComplete()
        }
    }
    
    /**
     * Test a flow that should emit a specific number of items
     */
    suspend fun <T> Flow<T>.testEmissionCount(
        timeout: Duration = DEFAULT_TIMEOUT,
        expectedCount: Int,
        validator: ((T) -> Boolean)? = null
    ): List<T> {
        val emissions = mutableListOf<T>()
        test(timeout = timeout) {
            repeat(expectedCount) {
                val emission = awaitItem()
                emissions.add(emission)
                validator?.let { validate ->
                    assert(validate(emission)) {
                        "Validation failed for emission: $emission"
                    }
                }
            }
            awaitComplete()
        }
        return emissions
    }
    
    /**
     * Test a flow that should never emit (empty flow)
     */
    suspend fun <T> Flow<T>.testNoEmissions(
        timeout: Duration = DEFAULT_TIMEOUT
    ) {
        test(timeout = timeout) {
            awaitComplete()
        }
    }
    
    /**
     * Test a flow that should emit and then complete without waiting for more emissions
     */
    suspend fun <T> Flow<T>.testEmissionAndComplete(
        timeout: Duration = DEFAULT_TIMEOUT,
        validator: (T) -> Boolean = { true }
    ): T {
        var result: T? = null
        test(timeout = timeout) {
            val emission = awaitItem()
            assert(validator(emission)) {
                "Validation failed for emission: $emission"
            }
            result = emission
            awaitComplete()
        }
        return result!!
    }
    
    /**
     * Test a flow that should emit an error
     */
    suspend fun <T> Flow<T>.testError(
        timeout: Duration = DEFAULT_TIMEOUT,
        expectedErrorType: Class<out Throwable>? = null
    ): Throwable {
        var caughtError: Throwable? = null
        test(timeout = timeout) {
            caughtError = awaitError()
            expectedErrorType?.let { expectedType ->
                assert(expectedType.isInstance(caughtError)) {
                    "Expected error of type $expectedType but got ${caughtError?.javaClass}"
                }
            }
        }
        return caughtError!!
    }
    
    /**
     * Test a flow with custom validation logic
     */
    suspend fun <T> Flow<T>.testWithValidation(
        timeout: Duration = DEFAULT_TIMEOUT,
        validate: suspend app.cash.turbine.ReceiveTurbine<T>.() -> Unit
    ) {
        test(timeout = timeout, validate = validate)
    }
    
    /**
     * Collect all emissions from a flow within a timeout period
     */
    suspend fun <T> Flow<T>.collectAllEmissions(
        timeout: Duration = DEFAULT_TIMEOUT
    ): List<T> {
        val emissions = mutableListOf<T>()
        test(timeout = timeout) {
            while (true) {
                try {
                    val emission = awaitItem()
                    emissions.add(emission)
                } catch (e: Exception) {
                    // Flow completed or timed out
                    break
                }
            }
        }
        return emissions
    }
}

/**
 * Extension functions for easier flow testing in test classes
 */

/**
 * Run a test with TestScope and provide flow testing utilities
 */
fun runFlowTest(
    timeout: Duration = FlowTestUtils.DEFAULT_TIMEOUT,
    testBody: suspend TestScope.() -> Unit
) = runTest(timeout = timeout, testBody = testBody)

/**
 * Test a flow that represents UI state changes
 */
suspend fun <T> Flow<T>.testStateChanges(
    initialState: T,
    vararg expectedStates: T,
    timeout: Duration = FlowTestUtils.DEFAULT_TIMEOUT
) {
    test(timeout = timeout) {
        // First emission should be initial state
        val initial = awaitItem()
        assert(initial == initialState) {
            "Expected initial state $initialState but got $initial"
        }
        
        // Test subsequent state changes
        expectedStates.forEach { expectedState ->
            val emission = awaitItem()
            assert(emission == expectedState) {
                "Expected state $expectedState but got $emission"
            }
        }
        
        // Ensure no more emissions
        expectNoEvents()
    }
}

/**
 * Test a flow that represents settings updates
 */
suspend fun <T> Flow<T>.testSettingsFlow(
    defaultValue: T,
    updates: List<T>,
    timeout: Duration = FlowTestUtils.DEFAULT_TIMEOUT
) {
    test(timeout = timeout) {
        // First emission should be default
        val initial = awaitItem()
        assert(initial == defaultValue) {
            "Expected default value $defaultValue but got $initial"
        }
        
        // Test each update
        updates.forEach { update ->
            val emission = awaitItem()
            assert(emission == update) {
                "Expected update $update but got $emission"
            }
        }
        
        awaitComplete()
    }
}

/**
 * Test a flow with debouncing behavior
 */
suspend fun <T> Flow<T>.testDebouncedFlow(
    expectedFinalValue: T,
    timeout: Duration = FlowTestUtils.DEFAULT_TIMEOUT
) {
    test(timeout = timeout) {
        // Skip intermediate emissions and get the final debounced value
        var lastEmission: T? = null
        while (true) {
            try {
                lastEmission = awaitItem()
            } catch (e: Exception) {
                break
            }
        }
        
        assert(lastEmission == expectedFinalValue) {
            "Expected final debounced value $expectedFinalValue but got $lastEmission"
        }
    }
}

/**
 * Test reactive repository behavior
 */
suspend fun <T> Flow<T>.testRepositoryFlow(
    operations: suspend () -> Unit,
    expectedEmissions: List<T>,
    timeout: Duration = FlowTestUtils.DEFAULT_TIMEOUT
) {
    test(timeout = timeout) {
        // Start operations that should trigger emissions
        operations()
        
        // Verify expected emissions
        expectedEmissions.forEach { expected ->
            val emission = awaitItem()
            assert(emission == expected) {
                "Expected $expected but got $emission"
            }
        }
        
        awaitComplete()
    }
}
