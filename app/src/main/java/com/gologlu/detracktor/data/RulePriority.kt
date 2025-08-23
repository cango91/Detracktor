package com.gologlu.detracktor.data

/**
 * Rule priority levels for hierarchical matching.
 * Lower level numbers indicate higher priority (more specific rules).
 */
enum class RulePriority(val level: Int) {
    EXACT_HOST(1),           // example.com
    SUBDOMAIN_WILDCARD(2),   // *.example.com  
    PATH_SPECIFIC(3),        // example.com/path/*
    GLOBAL_WILDCARD(4)       // *
}
