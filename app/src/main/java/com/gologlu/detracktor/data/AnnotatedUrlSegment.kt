package com.gologlu.detracktor.data

/**
 * Smart string representation for partial blur rendering.
 * Allows the UI to selectively blur sensitive parts of URLs.
 */
data class AnnotatedUrlSegment(
    val text: String,
    val type: SegmentType,
    val shouldBlur: Boolean = false
)

enum class SegmentType {
    PROTOCOL,
    CREDENTIALS,
    HOST,
    PATH,
    PARAM_NAME,
    PARAM_VALUE,
    SEPARATOR
}
