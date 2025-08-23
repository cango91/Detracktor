package com.gologlu.detracktor.data

/**
 * Configuration for host normalization behavior.
 */
data class HostNormalizationConfig(
    val enableCaseNormalization: Boolean = true,
    val enablePortNormalization: Boolean = true,
    val enableIDNConversion: Boolean = true,
    val defaultPorts: Map<String, Int> = mapOf("http" to 80, "https" to 443)
)
