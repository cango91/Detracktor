package com.gologlu.detracktor.application.error

import com.gologlu.detracktor.domain.error.DomainError
import com.gologlu.detracktor.domain.error.DomainResult

/**
 * App-layer error model. Wraps domain errors and introduces app-specific errors.
 * Prefer throwing AppException with a specific AppError subtype.
 */
sealed interface AppError {
    val message: String
    val cause: Throwable? get() = null
}

/** Bridge for bubbling up DomainError through the app layer. */
data class DomainViolation(val domain: DomainError) : AppError {
    override val message: String = domain.message
    override val cause: Throwable? = domain.cause
}

/** Thrown for user/config validation issues on a single field. */
data class AppValidationError(
    override val message: String,
    val fieldPath: String,
    override val cause: Throwable? = null
) : AppError

/** A single rules validation violation. */
data class Violation(
    val path: String,   // JSONPointer-like e.g. "sites[0].when.host.domains[1]"
    val message: String
)

/** Aggregated validation error for a rules document. */
data class RulesValidationError(
    val violations: List<Violation>
) : AppError {
    override val message: String =
        if (violations.isEmpty()) "Rules validation failed with no details."
        else buildString {
            append("Rules validation failed:")
            violations.forEach { v -> append("\n - ").append(v.path).append(": ").append(v.message) }
        }
}

/** Config/JSON load/parse issues. */
sealed interface AppConfigError : AppError
data class ConfigParseError(
    override val message: String,
    override val cause: Throwable? = null
) : AppConfigError
data class ConfigInvalidFieldError(
    val path: String,
    override val message: String,
    override val cause: Throwable? = null
) : AppConfigError

/** Canonicalization/normalization problems (rare, mostly wrapped into validation). */
data class CanonicalizationError(
    override val message: String,
    override val cause: Throwable? = null
) : AppError

/** Base exception to throw across app layer. */
open class AppException(val error: AppError) : Exception(error.message, error.cause)

/** Specific exception types for convenience/catching granularity. */
class RulesValidationException(val err: RulesValidationError) : AppException(err)
class AppValidationException(val err: AppValidationError) : AppException(err)
class AppConfigException(val err: AppConfigError) : AppException(err)

/** Tiny helper to add violations conditionally. */
inline fun MutableList<Violation>.requireValid(condition: Boolean, path: String, lazyMsg: () -> String) {
    if (!condition) add(Violation(path, lazyMsg()))
}


/** App-level result wrapper mirroring DomainResult, but with AppError. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    companion object {
        fun <T> success(value: T): AppResult<T> = Success(value)
        fun <T> failure(error: AppError): AppResult<T> = Failure(error)
    }
}

val <T> AppResult<T>.isSuccess: Boolean get() = this is AppResult.Success
val <T> AppResult<T>.isFailure: Boolean get() = this is AppResult.Failure

inline fun <T, R> AppResult<T>.map(f: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.success(f(value))
        is AppResult.Failure -> this
    }

inline fun <T, R> AppResult<T>.flatMap(f: (T) -> AppResult<R>): AppResult<R> =
    when (this) {
        is AppResult.Success -> f(value)
        is AppResult.Failure -> this
    }

inline fun <T, R> AppResult<T>.fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R =
    when (this) {
        is AppResult.Success -> onSuccess(value)
        is AppResult.Failure -> onFailure(error)
    }

fun <T> AppResult<T>.getOrThrow(): T =
    when (this) {
        is AppResult.Success -> value
        is AppResult.Failure -> throw AppException(error)
    }

fun <T> AppResult<T>.getOrNull(): T? =
    when (this) {
        is AppResult.Success -> value
        is AppResult.Failure -> null
    }

fun <T> AppResult<T>.getOrElse(default: () -> T): T =
    when (this) {
        is AppResult.Success -> value
        is AppResult.Failure -> default()
    }

/** Interop: wrap a DomainResult as an AppResult. */
fun <T> DomainResult<T>.toAppResult(): AppResult<T> =
    when (this) {
        is DomainResult.Success -> AppResult.success(this.value)
        is DomainResult.Failure -> AppResult.failure(DomainViolation(this.error))
    }

/** Interop: convert DomainError -> AppError immediately. */
fun DomainError.toAppError(): AppError = DomainViolation(this)

/** Interop: throw AppException from a DomainResult failure, pass through success. */
fun <T> DomainResult<T>.getOrThrowApp(): T =
    when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> throw AppException(DomainViolation(error))
    }