package com.gologlu.detracktor.domain.model

fun main() {
    // Test the specific failing case
    val complexQuery = "🚀=rocket&café=naïve&&empty=&=value&flag&𝕏=math"
    println("Testing complex query: $complexQuery")
    
    val queryPairs = QueryPairs.from(complexQuery)
    val tokens = queryPairs.getTokens()
    
    println("\nAll tokens:")
    tokens.forEachIndexed { index, token ->
        println("  $index: rawKey='${token.rawKey}', rawValue='${token.rawValue}', hasEquals=${token.hasEquals}")
        println("      decodedKey='${token.decodedKey}', decodedValue='${token.decodedValue}'")
    }
    
    // Test getFirst with empty key
    val emptyKeyResult = queryPairs.getFirst("")
    println("\nqueryPairs.getFirst(\"\") = '$emptyKeyResult'")
    
    // Test simple =value case
    println("\n--- Testing simple =value case ---")
    val simpleQuery = "=value"
    val simpleQueryPairs = QueryPairs.from(simpleQuery)
    val simpleTokens = simpleQueryPairs.getTokens()
    
    println("Simple query: $simpleQuery")
    simpleTokens.forEachIndexed { index, token ->
        println("  $index: rawKey='${token.rawKey}', rawValue='${token.rawValue}', hasEquals=${token.hasEquals}")
        println("      decodedKey='${token.decodedKey}', decodedValue='${token.decodedValue}'")
    }
    
    val simpleResult = simpleQueryPairs.getFirst("")
    println("simpleQueryPairs.getFirst(\"\") = '$simpleResult'")
}
