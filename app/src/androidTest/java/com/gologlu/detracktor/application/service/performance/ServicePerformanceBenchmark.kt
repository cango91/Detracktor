package com.gologlu.detracktor.application.service.performance

import com.gologlu.detracktor.application.service.globby.Globby
import com.gologlu.detracktor.runtime.android.service.net.HostCanonicalizer
import com.gologlu.detracktor.runtime.android.service.net.UrlParserImpl
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WhenBlock
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.domain.error.isSuccess
import com.gologlu.detracktor.domain.error.getOrThrow
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

class ServicePerformanceBenchmark {

    private val urlParser = UrlParserImpl()

    @Test
    fun `benchmark_globby_pattern_matching_performance`() {
        // Test Globby performance with various pattern complexities
        val patterns = listOf(
            "utm_*" to "Simple prefix pattern",
            "*_id" to "Simple suffix pattern", 
            "utm_*_source" to "Middle wildcard pattern",
            "*a*b*c*d*e*" to "Multiple wildcards",
            "*tracking*analytics*pixel*conversion*" to "Complex backtracking",
            "?" to "Single character wildcard",
            "param?" to "Single char at end",
            "\\*literal\\*" to "Escaped literals"
        )
        
        val testStrings = listOf(
            "utm_source", "utm_campaign", "utm_medium",
            "user_id", "session_id", "tracking_id",
            "abcde", "tracking_analytics_pixel_conversion_data",
            "a", "param1", "*literal*", "no_match_here"
        )
        
        println("=== Globby Pattern Matching Performance ===")
        
        for ((pattern, description) in patterns) {
            val duration = measureTimeMillis {
                repeat(10000) {
                    for (testString in testStrings) {
                        Globby.matches(pattern, testString)
                    }
                }
            }
            
            val opsPerSecond = (10000 * testStrings.size * 1000) / duration
            println("$description: ${duration}ms (${opsPerSecond} ops/sec)")
            
            // Performance assertion - should complete within reasonable time
            assertTrue("$description should complete quickly", duration < 5000)
        }
    }

    @Test
    fun `benchmark_globby_validation_performance`() {
        // Test Globby validation performance
        val patterns = listOf(
            "utm_*",
            "*_id", 
            "utm_*_source",
            "*a*b*c*d*e*",
            "param\\*name\\?test",
            "a".repeat(256), // Max length
            "complex_pattern_with_many_literals_and_*_wildcards_?_mixed"
        )
        
        println("\n=== Globby Validation Performance ===")
        
        val duration = measureTimeMillis {
            repeat(10000) {
                for (pattern in patterns) {
                    try {
                        Globby.requireValid(pattern, "benchmark.test")
                    } catch (e: Exception) {
                        // Expected for some patterns
                    }
                }
            }
        }
        
        val opsPerSecond = (10000 * patterns.size * 1000) / duration
        println("Validation: ${duration}ms (${opsPerSecond} ops/sec)")
        assertTrue("Validation should be fast", duration < 2000)
    }

    @Test
    fun `benchmark_host_canonicalizer_performance`() {
        // Test HostCanonicalizer performance with various domain types
        val domains = listOf(
            "example.com",
            "EXAMPLE.COM",
            "sub.domain.example.com",
            "bücher.example.com", // IDN
            "例え.テスト", // Japanese IDN
            "مثال.اختبار", // Arabic IDN
            "пример.испытание", // Cyrillic IDN
            "very-long-subdomain-name.with-many-parts.example.com",
            "192.168.1.1", // IPv4
            "localhost"
        )
        
        println("\n=== Host Canonicalizer Performance ===")
        
        val duration = measureTimeMillis {
            repeat(10000) {
                for (domain in domains) {
                    HostCanonicalizer.toAscii(domain)
                }
            }
        }
        
        val opsPerSecond = (10000 * domains.size * 1000) / duration
        println("Host canonicalization: ${duration}ms (${opsPerSecond} ops/sec)")
        assertTrue("Host canonicalization should be fast", duration < 3000)
    }

    @Test
    fun `benchmark_url_parser_performance`() {
        // Test UrlParserImpl performance with various URL types
        val urls = listOf(
            "https://example.com",
            "https://example.com/path",
            "https://example.com/path?query=value",
            "https://example.com/path?query=value#fragment",
            "https://user:pass@example.com:8443/path?query=value#fragment",
            "http://[::1]:8080/api",
            "https://例え.テスト/path?param=value",
            "https://example.com/very/long/path/with/many/segments?param1=value1&param2=value2&param3=value3",
            "ftp://files.example.com/file.txt",
            "mailto:user@example.com"
        )
        
        println("\n=== URL Parser Performance ===")
        
        val duration = measureTimeMillis {
            repeat(10000) {
                for (url in urls) {
                    urlParser.parse(url)
                }
            }
        }
        
        val opsPerSecond = (10000 * urls.size * 1000) / duration
        println("URL parsing: ${duration}ms (${opsPerSecond} ops/sec)")
        assertTrue("URL parsing should be fast", duration < 5000)
    }

    @Test
    fun `benchmark_rules_json_parsing_performance`() {
        // Test RulesJson parsing performance
        println("\n=== Rules Build Performance (DTO->App via constructors) ===")
        val smallDuration = measureTimeMillis {
            repeat(10000) {
                AppSettings(
                    sites = listOf(
                        UrlRule(
                            when_ = WhenBlock(
                                host = HostCond(
                                    domains = com.gologlu.detracktor.application.types.Domains.ListOf(listOf("example.com")),
                                    subdomains = com.gologlu.detracktor.application.types.Subdomains.Any
                                ),
                                schemes = listOf("http","https")
                            ),
                            then = ThenBlock(remove = listOf(Pattern("utm_*")), warn = null),
                            metadata = null
                        )
                    ),
                    warnings = com.gologlu.detracktor.application.types.WarningSettings(),
                    version = AppSettings.VERSION
                )
            }
        }
        println("Small build (1 site): ${smallDuration}ms")
    }

    @Test
    fun `benchmark_integrated_workflow_performance`() {
        // Test the complete workflow: URL parsing -> Host canonicalization -> Rules matching -> Pattern matching
        val testUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=google&gclid=abc123",
            "https://example.com/path?utm_campaign=test&fbclid=def456&normal=keep",
            "https://測試.例え/search?追跡_source=test&param=value",
            "https://api.github.com/repos/user/repo?access_token=secret&utm_medium=email"
        )
        
        val rules = AppSettings(
            sites = listOf(
                UrlRule(
                    when_ = WhenBlock(
                        host = HostCond(domains = com.gologlu.detracktor.application.types.Domains.Any, subdomains = null),
                        schemes = null
                    ),
                    then = ThenBlock(remove = listOf(Pattern("utm_*"), Pattern("gclid"), Pattern("fbclid"), Pattern("追跡_*"), Pattern("access_token")), warn = null),
                    metadata = null
                )
            ),
            warnings = com.gologlu.detracktor.application.types.WarningSettings(),
            version = AppSettings.VERSION
        )
        val removePatterns = rules.sites[0].then.remove.map { it.pattern }
        
        println("\n=== Integrated Workflow Performance ===")
        
        val duration = measureTimeMillis {
            repeat(1000) {
                for (url in testUrls) {
                    // Step 1: Parse URL
                    val parseResult = urlParser.parse(url)
                    if (parseResult.isSuccess) {
                        val urlParts = parseResult.getOrThrow()
                        
                        // Step 2: Canonicalize host
                        val canonicalHost = HostCanonicalizer.toAscii(urlParts.host)
                        
                        // Step 3: Extract query parameters (simplified)
                        val queryParams = urlParts.rawQuery.split("&")
                            .mapNotNull { param ->
                                val parts = param.split("=", limit = 2)
                                if (parts.isNotEmpty()) parts[0] else null
                            }
                        
                        // Step 4: Apply pattern matching
                        val paramsToRemove = queryParams.filter { param ->
                            removePatterns.any { pattern -> Globby.matches(pattern, param) }
                        }
                        
                        // Simulate parameter removal (just count for benchmark)
                        val remainingParams = queryParams.size - paramsToRemove.size
                    }
                }
            }
        }
        
        val opsPerSecond = (1000 * testUrls.size * 1000) / duration
        println("Complete workflow: ${duration}ms (${opsPerSecond} ops/sec)")
        assertTrue("Complete workflow should be performant", duration < 10000)
    }

    @Test
    fun `benchmark_memory_usage_patterns`() {
        // Test memory usage patterns (basic test - would need profiling tools for detailed analysis)
        println("\n=== Memory Usage Patterns ===")
        
        val runtime = Runtime.getRuntime()
        
        // Baseline memory
        System.gc()
        Thread.sleep(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create many pattern matching operations
        val patterns = listOf("utm_*", "*_id", "tracking_*", "?", "*a*b*c*")
        val testStrings = (1..1000).map { "test_param_$it" }
        
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform operations
        for (pattern in patterns) {
            for (testString in testStrings) {
                Globby.matches(pattern, testString)
            }
        }
        
        System.gc()
        Thread.sleep(100)
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val memoryUsed = afterMemory - beforeMemory
        println("Memory used for pattern matching: ${memoryUsed / 1024}KB")
        
        // Test URL parsing memory usage
        val beforeUrlMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val urls = (1..1000).map { "https://example$it.com/path?param$it=value$it" }
        val parsedUrls = urls.map { urlParser.parse(it) }
        
        System.gc()
        Thread.sleep(100)
        val afterUrlMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val urlMemoryUsed = afterUrlMemory - beforeUrlMemory
        println("Memory used for URL parsing: ${urlMemoryUsed / 1024}KB")
        
        // Basic assertion - memory usage should be reasonable
        assertTrue("Pattern matching memory usage should be reasonable", memoryUsed < 10 * 1024 * 1024) // 10MB
        assertTrue("URL parsing memory usage should be reasonable", urlMemoryUsed < 50 * 1024 * 1024) // 50MB
    }

    @Test
    fun `benchmark_worst_case_scenarios`() {
        // Test worst-case performance scenarios
        println("\n=== Worst Case Scenarios ===")
        
        // Worst case for Globby: deeply nested backtracking
        val worstCasePattern = "*a*b*c*d*e*f*g*h*i*j*k*l*m*n*o*p*"
        val worstCaseString = "abcdefghijklmnop" + "x".repeat(100) // Forces backtracking
        
        val globbyWorstCase = measureTimeMillis {
            repeat(100) {
                Globby.matches(worstCasePattern, worstCaseString)
            }
        }
        
        println("Globby worst case (backtracking): ${globbyWorstCase}ms")
        assertTrue("Globby worst case should complete", globbyWorstCase < 5000)
        
        // Worst case for HostCanonicalizer: very long IDN domain
        val longIdnDomain = "测试".repeat(20) + ".example.com"
        
        val hostWorstCase = measureTimeMillis {
            repeat(1000) {
                HostCanonicalizer.toAscii(longIdnDomain)
            }
        }
        
        println("Host canonicalizer worst case (long IDN): ${hostWorstCase}ms")
        assertTrue("Host canonicalizer worst case should complete", hostWorstCase < 3000)
        
        // Worst case for URL parser: very long URL with many parameters
        val longUrl = "https://example.com/very/long/path/" + "segment/".repeat(100) + 
                     "?" + (1..100).joinToString("&") { "param$it=value$it" }
        
        val urlWorstCase = measureTimeMillis {
            repeat(1000) {
                urlParser.parse(longUrl)
            }
        }
        
        println("URL parser worst case (long URL): ${urlWorstCase}ms")
        assertTrue("URL parser worst case should complete", urlWorstCase < 5000)
    }

    @Test
    fun `benchmark_concurrent_access_simulation`() {
        // Simulate concurrent access patterns (single-threaded simulation)
        println("\n=== Concurrent Access Simulation ===")
        
        val patterns = listOf("utm_*", "gclid", "fbclid", "*_id", "tracking_*")
        val testStrings = listOf("utm_source", "gclid", "user_id", "tracking_pixel", "normal_param")
        
        // Simulate multiple "threads" accessing services simultaneously
        val duration = measureTimeMillis {
            repeat(1000) { iteration ->
                // Simulate different operations happening concurrently
                when (iteration % 4) {
                    0 -> {
                        // Pattern matching
                        for (pattern in patterns) {
                            for (testString in testStrings) {
                                Globby.matches(pattern, testString)
                            }
                        }
                    }
                    1 -> {
                        // Host canonicalization
                        val hosts = listOf("Example.COM", "测试.example.com", "sub.domain.org")
                        for (host in hosts) {
                            HostCanonicalizer.toAscii(host)
                        }
                    }
                    2 -> {
                        // URL parsing
                        val urls = listOf(
                            "https://example.com/path?param=value",
                            "https://测试.example.com/path",
                            "http://localhost:8080/api"
                        )
                        for (url in urls) {
                            urlParser.parse(url)
                        }
                    }
                    3 -> {
                        // Rules building (would be cached in real app)
                        AppSettings(
                            sites = listOf(
                                UrlRule(
                                    when_ = WhenBlock(
                                        host = HostCond(domains = com.gologlu.detracktor.application.types.Domains.Any, subdomains = null),
                                        schemes = null
                                    ),
                                    then = ThenBlock(remove = listOf(Pattern("utm_*")), warn = null),
                                    metadata = null
                                )
                            ),
                            warnings = com.gologlu.detracktor.application.types.WarningSettings(),
                            version = AppSettings.VERSION
                        )
                    }
                }
            }
        }
        
        val opsPerSecond = (1000 * 1000) / duration
        println("Concurrent simulation: ${duration}ms (${opsPerSecond} ops/sec)")
        assertTrue("Concurrent access simulation should be performant", duration < 15000)
    }
}
