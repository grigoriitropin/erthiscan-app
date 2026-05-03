// pluginManagement: Configures how Gradle resolves plugins.
pluginManagement {
    // repositories: Defines where Gradle looks for plugin artifacts.
    repositories {
        // google(): Priority repository for Android-specific plugins.
        google {
            // content block: Optimization to limit lookups to specific groups.
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // mavenCentral(): Fallback repository for standard Java/Kotlin plugins.
        mavenCentral()
        // gradlePluginPortal(): Repository for community-contributed Gradle plugins.
        gradlePluginPortal()
    }
}

// plugins block in settings: Allows applying plugins to the build settings themselves.
plugins {
    // id("org.gradle.toolchains.foojay-resolver-convention"): Automatically resolves JDK toolchains
    // via the Foojay Discovery API, simplifying cross-platform JDK management.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// dependencyResolutionManagement: Configures how project dependencies are resolved.
dependencyResolutionManagement {
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS): 
    // Enforces that all repositories MUST be defined here, not in individual build.gradle files.
    // This provides a single source of truth for dependency resolution.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    // repositories: Defines the global list of repositories for all modules.
    repositories {
        // google(): Essential for Android and Google libraries.
        google()
        // mavenCentral(): Standard repository for the vast majority of open-source libraries.
        mavenCentral()
    }
}

// rootProject.name: The formal name of the project in the Gradle system.
rootProject.name = "Erthiscan"

// include(":app"): Declares the ':app' directory as a Gradle module.
include(":app")
