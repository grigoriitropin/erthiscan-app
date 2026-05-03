package io.erthiscan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * ERTHISCAN THEME: The central styling configuration for the application.
 * 
 * ARCHITECTURAL ROLE:
 * This component implements Material Design 3 (M3) and integrates with the Android 
 * "Dynamic Color" system (Material You). It ensures that the app's palette 
 * harmonizes with the user's system wallpaper on Android 12+.
 * 
 * KEY FEATURES:
 * 1. DYNAMIC COLOR: Leverages [dynamicLightColorScheme] and [dynamicDarkColorScheme] 
 *    to fetch user-specific accent colors.
 * 2. DARK MODE SUPPORT: Automatically reacts to the [isSystemInDarkTheme] hook.
 * 3. COMPOSABLE WRAPPER: Provides the [MaterialTheme] context to all child nodes 
 *    in the UI tree.
 */
@Composable
fun ErthiScanTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    
    // COLOR SELECTION: 
    // Uses Material You dynamic colors if available (API 31+). 
    // Falls back to system defaults otherwise.
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    // THEME PROVIDER: Applies the colorScheme to the Material design system.
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
