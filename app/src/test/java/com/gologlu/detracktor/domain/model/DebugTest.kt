package com.gologlu.detracktor.domain.model

fun main() {
    val complexQuery = "🚀=rocket&café=naïve&&empty=&=value&flag&𝕏=math"
    println("Original: \"$complexQuery\"")
    println("Original length: ${complexQuery.length}")
    
    val queryPairs = QueryPairs.from(complexQuery)
    val roundTrip = queryPairs.asString()
    println("Round-trip: \"$roundTrip\"")
    println("Round-trip length: ${roundTrip.length}")
    println("Equal: ${complexQuery == roundTrip}")
    
    // Check character by character
    if (complexQuery != roundTrip) {
        val minLen = minOf(complexQuery.length, roundTrip.length)
        for (i in 0 until minLen) {
            if (complexQuery[i] != roundTrip[i]) {
                println("Diff at index $i:")
                println("  Original: '${complexQuery[i]}' (code=${complexQuery[i].code})")
                println("  RoundTrip: '${roundTrip[i]}' (code=${roundTrip[i].code})")
                break
            }
        }
        if (complexQuery.length != roundTrip.length) {
            println("Length difference: original=${complexQuery.length}, roundTrip=${roundTrip.length}")
        }
    }
    
    // Check tokens
    val tokens = queryPairs.getTokens()
    println("\nTokens:")
    tokens.forEachIndexed { index, token ->
        println("  $index: rawKey='${token.rawKey}', rawValue='${token.rawValue}', hasEquals=${token.hasEquals}")
    }
}
