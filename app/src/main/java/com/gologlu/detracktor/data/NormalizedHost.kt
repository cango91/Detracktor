package com.gologlu.detracktor.data

/**
 * Host normalization result containing original and normalized forms.
 * Supports IDN/Punycode conversion and port normalization.
 */
data class NormalizedHost(
    val original: String,
    val normalized: String,
    val port: Int?,
    val isIDN: Boolean,
    val punycode: String?
)
