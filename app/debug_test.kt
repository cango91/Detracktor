import com.gologlu.detracktor.domain.model.QueryPairs

fun main() {
    val complexQuery = "🚀=rocket&café=naïve&&empty=&=value&flag&𝕏=math"
    println("Original: $complexQuery")
    
    val queryPairs = QueryPairs.from(complexQuery)
    val roundTrip = queryPairs.asString()
    println("Round-trip: $roundTrip")
    println("Equal: ${complexQuery == roundTrip}")
    
    // Check individual tokens
    val tokens = queryPairs.getTokens()
    tokens.forEachIndexed { index, token ->
        println("Token $index: rawKey='${token.rawKey}', rawValue='${token.rawValue}', hasEquals=${token.hasEquals}")
    }
}
