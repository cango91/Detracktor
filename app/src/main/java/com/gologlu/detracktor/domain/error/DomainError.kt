package com.gologlu.detracktor.domain.error

/**
 * Base interface for all domain-specific errors in the application.
 * Represents errors that can occur during domain operations, providing
 * a common structure with error message and optional cause.
 */
sealed interface DomainError {
    /** Human-readable error message describing what went wrong */
    val message: String
    /** Optional underlying throwable that caused this error */
    val cause: Throwable?
}

/**
 * Exception wrapper for domain errors to integrate with exception-based code.
 * Converts a [DomainError] into a throwable exception for compatibility.
 *
 * @param error The domain error to wrap
 */
class DomainException(val error: DomainError) : Exception(error.message, error.cause)

/**
 * Represents the result of a domain operation that can either succeed with a value or fail with an error.
 * This type provides a functional approach to error handling without exceptions.
 *
 * @param T The type of the success value
 */
sealed interface DomainResult<out T> {
    /**
     * Represents a successful operation with a value.
     *
     * @param value The successful result value
     */
    data class Success<T>(val value: T) : DomainResult<T>
    
    /**
     * Represents a failed operation with a domain error.
     *
     * @param error The domain error that occurred
     */
    data class Failure(val error: DomainError) : DomainResult<Nothing>

    companion object {
        /**
         * Creates a successful result with the given value.
         *
         * @param value The success value
         * @return A success result containing the value
         */
        fun <T> success(value: T): DomainResult<T> = Success(value)
        
        /**
         * Creates a failure result with the given error.
         *
         * @param error The domain error
         * @return A failure result containing the error
         */
        fun <T> failure(error: DomainError): DomainResult<T> = Failure(error)
    }
}

/** * Returns true if this result represents a success. */
val <T> DomainResult<T>.isSuccess: Boolean
    get() = this is DomainResult.Success

/** * Returns true if this result represents a failure. */
val <T> DomainResult<T>.isFailure: Boolean
    get() = this is DomainResult.Failure

/** * Transforms the success value using the given function. * If this is a failure, returns the failure unchanged. */
inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> {
    return when (this) {
        is DomainResult.Success -> DomainResult.success(transform(value))
        is DomainResult.Failure -> this }
}

/** * Transforms the success value using a function that returns a DomainResult. * If this is a failure, returns the failure unchanged. * This enables chaining of operations that can fail. */
inline fun <T, R> DomainResult<T>.flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> {
    return when (this) {
        is DomainResult.Success -> transform(value)
        is DomainResult.Failure -> this
    }
}

/** * Handles both success and failure cases by applying the appropriate function. */
inline fun <T, R> DomainResult<T>.fold( onSuccess: (T) -> R, onFailure: (DomainError) -> R ): R {
    return when (this) {
        is DomainResult.Success -> onSuccess(value)
        is DomainResult.Failure -> onFailure(error)
    }
}

/** * Returns the success value or throws an exception based on the error. * Use this for compatibility with exception-based code. */
fun <T> DomainResult<T>.getOrThrow(): T {
    return when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> throw DomainException(error)
    }
}
/** * Returns the success value or the provided default value if this is a failure. */
fun <T> DomainResult<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> defaultValue
    }
}
/** * Returns the success value or null if this is a failure. */
fun <T> DomainResult<T>.getOrNull(): T? {
    return when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> null
    }
}

/**
 * Returns the success value or the result of calling the default function if this is a failure.
 * This is more flexible than getOrDefault as it allows lazy evaluation of the default value.
 */
inline fun <T> DomainResult<T>.getOrElse(default: () -> T): T {
    return when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> default()
    }
}

/**
 * Returns the success value or transforms the error into a success value using the transform function.
 * This allows recovery from errors by converting them into valid values.
 */
inline fun <T> DomainResult<T>.recover(transform: (DomainError) -> T): T {
    return when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> transform(error)
    }
}
/** * Executes the given function on the success value and returns the original result. * This enables side effects (like logging) without changing the result type. */
inline fun <T> DomainResult<T>.onSuccess(f: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) {
        f(value)
    }
    return this
}
/** * Executes the given function on the failure error and returns the original result. * This enables side effects (like logging) without changing the result type. */
inline fun <T> DomainResult<T>.onFailure(f: (DomainError) -> Unit): DomainResult<T> {
    if (this is DomainResult.Failure) {
        f(error)
    }
    return this
}

/**
 * Returns the exception that would be thrown by getOrThrow() or null if this is a success.
 * This provides access to the underlying error without throwing an exception.
 */
fun <T> DomainResult<T>.exceptionOrNull(): DomainException? {
    return when (this) {
        is DomainResult.Success -> null
        is DomainResult.Failure -> DomainException(error)
    }
}
