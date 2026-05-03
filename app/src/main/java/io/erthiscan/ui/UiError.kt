package io.erthiscan.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * UI ERROR: A sealed representation of transient application failures.
 * 
 * ARCHITECTURAL ROLE:
 * Decouples low-level exceptions (IOException, HttpException) from the UI layer. 
 * By using a sealed class, we ensure exhaustive handling and allow for both 
 * hardcoded messages and localized resource-based errors.
 * 
 * DESIGN PATTERNS:
 * 1. SEALED CLASS: Limits the hierarchy to 'Message' and 'Resource' types.
 * 2. UNIQUE ID: Uses a timestamp-based ID to ensure that consecutive identical 
 *    errors (e.g., repeated network timeouts) trigger UI updates (like Snackbars).
 */
sealed class UiError {
    abstract val id: Long

    /**
     * MESSAGE: Used for dynamic or unlocalized error strings (e.g., debug messages).
     */
    data class Message(
        val message: String,
        override val id: Long = System.currentTimeMillis()
    ) : UiError()

    /**
     * RESOURCE: The preferred type for production, using localized strings.
     */
    data class Resource(
        @StringRes val resId: Int,
        override val id: Long = System.currentTimeMillis()
    ) : UiError()

    /**
     * AS STRING: Standard context-based extraction for non-Compose callers (e.g., snackbars).
     */
    fun asString(context: Context): String = when (this) {
        is Message -> message
        is Resource -> context.getString(resId)
    }

    /**
     * AS COMPOSABLE STRING: Leverages stringResource() for reactive UI nodes.
     */
    @Composable
    fun asComposableString(): String = when (this) {
        is Message -> message
        is Resource -> stringResource(resId)
    }

    companion object {
        /**
         * FROM THROWABLE: The central mapping logic for the application.
         * 
         * Logic:
         * 1. UnknownHostException -> No Internet connection.
         * 2. HttpException -> Maps HTTP status codes to specific meanings (401, 404, etc.).
         * 3. Default -> Generic "Something went wrong" message.
         */
        fun from(t: Throwable): UiError = when (t) {
            is java.net.UnknownHostException -> Resource(io.erthiscan.R.string.error_no_internet)
            is retrofit2.HttpException -> {
                when (t.code()) {
                    401 -> Resource(io.erthiscan.R.string.error_unauthorized)
                    404 -> Resource(io.erthiscan.R.string.error_not_found)
                    else -> Resource(io.erthiscan.R.string.error_server)
                }
            }
            else -> Resource(io.erthiscan.R.string.error_unknown)
        }
    }
}