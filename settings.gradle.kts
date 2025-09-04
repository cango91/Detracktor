pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Required for Kotlin/JS Node.js setup
        exclusiveContent {
            forRepository {
                ivy {
                    name = "Node Distributions at ${"$"}{ivy.gradleUserHomeDir}/nodejs"
                    url = uri("https://nodejs.org/dist/")
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources { artifact() }
                }
            }
            filter { includeModuleByRegex("org\\.nodejs", ".*") }
        }
    }
}

rootProject.name = "Detracktor"
include(":app")
include(":core:domain")
include(":core:application")
 