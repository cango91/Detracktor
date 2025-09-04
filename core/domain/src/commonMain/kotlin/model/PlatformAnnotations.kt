package com.gologlu.detracktor.domain.model

/**
 * Platform-specific annotation for value classes.
 * On JVM/Android: @JvmInline, On JS: no-op
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class PlatformInline()