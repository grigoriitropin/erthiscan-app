import java.io.FileInputStream
import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // ANDROID APPLICATION: The core plugin required to build an Android application.
    alias(libs.plugins.android.application)
    // KOTLIN COMPOSE: Compiler plugin that enables Compose support. Kotlin support is now built-in to AGP 9.1.1
    alias(libs.plugins.kotlin.compose)
    // SERIALIZATION: Required for kotlinx.serialization.
    alias(libs.plugins.kotlin.serialization)
    // KSP: Kotlin Symbol Processing, used here for Hilt code generation.
    alias(libs.plugins.ksp)
    // HILT: Dependency injection framework for Android.
    alias(libs.plugins.hilt)
}

// LOCAL PROPERTIES: Loads configuration secrets from local.properties.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}

extensions.configure<ApplicationExtension> {
    namespace = "io.erthiscan"
    compileSdk = 36

    signingConfigs {
        // RELEASE SIGNING: Uses credentials from local.properties.
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_STORE_FILE", ""))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    defaultConfig {
        applicationId = "io.erthiscan"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BUILD CONFIG FIELDS: Injects compile-time constants.
        buildConfigField("String", "API_BASE_URL", "\"${localProps.getProperty("API_BASE_URL", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
    }

    buildTypes {
        release {
            // OPTIMIZATION: Enables R8 code shrinking and resource stripping.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        // COMPOSE & BUILD CONFIG: Enables Jetpack Compose and BuildConfig generation.
        compose = true
        buildConfig = true
    }
}

// Global Kotlin configuration for Built-in Kotlin (AGP 9.1+)
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Fix KT-73255: Opt-in to modern annotation targeting behavior for constructor properties
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

base { archivesName.set("erthiscan") }

dependencies {
    // --- UI LAYER ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // --- LIFECYCLE ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- NAVIGATION ---
    implementation(libs.androidx.navigation.compose)

    // --- DEPENDENCY INJECTION ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- NETWORKING & SERIALIZATION ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // --- SCANNING ---
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.compose)
    implementation(libs.camera.viewfinder.compose)
    implementation(libs.mlkit.barcode)

    // --- AUTHENTICATION ---
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- STORAGE & SECURITY ---
    implementation(libs.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.profileinstaller)

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
