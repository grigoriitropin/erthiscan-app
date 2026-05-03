// The plugins block defines the build-level plugins used by the project.
// These are identified by their aliases from the Version Catalog (libs.versions.toml).
plugins {
    // alias(libs.plugins.android.application): Provides the core Android build logic for the application module.
    // 'apply false' prevents the plugin from being applied to the root project itself, only declaring it for child modules.
    alias(libs.plugins.android.application) apply false
    
    // alias(libs.plugins.kotlin.android): Integrates Kotlin support into the Android build process.
    alias(libs.plugins.kotlin.android) apply false
    
    // alias(libs.plugins.kotlin.compose): Enables the Jetpack Compose compiler plugin for efficient UI rendering.
    alias(libs.plugins.kotlin.compose) apply false
    
    // alias(libs.plugins.kotlin.serialization): Adds support for Kotlinx Serialization (JSON/CBOR/etc).
    alias(libs.plugins.kotlin.serialization) apply false
    
    // alias(libs.plugins.ksp): Integrates Kotlin Symbol Processing (KSP), a fast alternative to kapt for code generation.
    alias(libs.plugins.ksp) apply false
    
    // alias(libs.plugins.hilt): Provides Hilt dependency injection support across the project.
    alias(libs.plugins.hilt) apply false
}
