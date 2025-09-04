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
                // No external dependencies - pure business logic
            }
        }
        
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        
        androidMain {
            dependencies {
                // Android-specific implementations if needed
            }
        }
        
        jsMain {
            dependencies {
                // JS-specific implementations if needed
            }
        }
    }
}

android {
    namespace = "com.gologlu.detracktor.domain"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 29
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}