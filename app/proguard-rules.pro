# PROGUARD & R8 RULES
# This file defines rules for code shrinking, optimization, and obfuscation.

# --- KOTLINX SERIALIZATION ---
# These rules ensure that serializers generated at compile time are not removed or renamed.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Pre-fills the R8 mapping for companion objects that handle serialization.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve serializers specifically for our API models to ensure JSON parsing works in Release.
-keep,includedescriptorclasses class io.erthiscan.api.**$$serializer { *; }
-keepclassmembers class io.erthiscan.api.** {
    *** Companion;
}
-keepclasseswithmembers class io.erthiscan.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- RETROFIT ---
# Retrofit uses reflection to scan interface methods and build HTTP requests.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepattributes Signature, Exceptions
# Keep methods with HTTP annotations (GET, POST, etc.) so Retrofit can find them.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Suppress warnings for missing optional dependencies.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# --- OKHTTP ---
# Suppress warnings for optional platform-specific security providers.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- HILT (DEPENDENCY INJECTION) ---
# Hilt generates and accesses classes by name; aggressive shrinking will break DI.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel
-keep class io.erthiscan.ErthiscanApp

# --- DATASTORE ---
# Ensures Preference keys are not renamed to prevent data loss.
-keep class androidx.datastore.** { *; }
-keepclassmembers class * {
    @androidx.datastore.preferences.core.Preference ***();
}

# --- CAMERAX ---
# Hardware abstraction layer classes must remain intact for native driver communication.
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- MLKIT (BARCODE SCANNING) ---
# MLKit relies on native libraries and Play Services (GMS).
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

# --- TINK (CRYPTOGRAPHY) ---
# CRITICAL: Crypto algorithms and keyset managers must NOT be obfuscated.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- GOOGLE ID & CREDENTIALS ---
# Required for Google OAuth2 integration.
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }
