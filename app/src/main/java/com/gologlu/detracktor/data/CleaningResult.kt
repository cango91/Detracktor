package com.gologlu.detracktor.data

/**
 * Result of URL cleaning operation
 */
enum class CleaningResult {
    CLIPBOARD_EMPTY,
    NOT_A_URL,
    NO_CHANGE,
    CLEANED_AND_COPIED
}
