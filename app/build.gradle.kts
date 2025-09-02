import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load version from version.properties file
val versionPropsFile = file("../version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

android {
    namespace = "com.gologlu.detracktor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gologlu.detracktor"
        minSdk = 29
        targetSdk = 36
        versionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
        versionName = versionProps.getProperty("VERSION_NAME", "1.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true // Enable coverage for debug builds
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enable desugaring for newer Java features
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
    testOptions {
        animationsDisabled = true
        unitTests {
            isReturnDefaultValues = true
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // URL cleaning app dependencies
    implementation(libs.gson)  // JSON parsing
    implementation(libs.androidx.preference.ktx)  // Settings
    
    // Enhanced URL cleaning dependencies
    implementation("com.ibm.icu:icu4j:77.1")  // IDN and internationalization support
    implementation("org.apache.commons:commons-lang3:3.18.0")  // Enhanced regex and pattern matching
    // Using Android's built-in LruCache instead of Caffeine for Android compatibility
    
    // Core library desugaring for compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation("org.mockito:mockito-inline:5.2.0")  // Enhanced mocking for new functionality
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Jacoco configuration
jacoco {
    toolVersion = "0.8.13"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/databinding/**/*.*",
        "**/generated/**/*.*",
        "**/compose/**/*.*" // Exclude Compose generated classes
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    val kotlinSrc = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc, kotlinSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}

// Optional: Task to combine unit and instrumented test coverage
tasks.register<JacocoReport>("jacocoFullReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/databinding/**/*.*",
        "**/generated/**/*.*",
        "**/compose/**/*.*"
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    val kotlinSrc = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc, kotlinSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            "outputs/code_coverage/debugAndroidTest/connected/coverage.ec"
        )
    })
}

// Task to verify version loading
tasks.register("printVersion") {
    doLast {
        println("Version Name: ${android.defaultConfig.versionName}")
        println("Version Code: ${android.defaultConfig.versionCode}")
    }
}
