plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                    freeCompilerArgs.add("-Xcontext-parameters")
                }
            }
        }
    }
    
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
        
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xcontext-parameters")
                }
            }
        }
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core:domain"))
                // Common application layer dependencies
            }
        }
        
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        androidMain {
            dependencies {
                // Android-specific dependencies will go here
                // e.g., for HostCanonicalizer using ICU4J
            }
        }
        
        androidUnitTest {
            dependencies {
                implementation(libs.mockk.android)
            }
        }
        
        jsMain {
            dependencies {
                // JS-specific implementations
                // Browser APIs, etc.
            }
        }
    }
}

android {
    namespace = "com.gologlu.detracktor.application"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 29
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}