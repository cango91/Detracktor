package com.gologlu.detracktor.application.types

import com.gologlu.detracktor.application.service.globby.Globby

@JvmInline
value class Pattern(val pattern: String) {
    init {
        Globby.requireValid(pattern, "pattern")
    }
}