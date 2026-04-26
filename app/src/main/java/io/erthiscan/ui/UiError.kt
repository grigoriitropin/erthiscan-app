package io.erthiscan.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiError {
    abstract val id: Long

    data class Message(
        val message: String,
        override val id: Long = System.currentTimeMillis()
    ) : UiError()

    data class Resource(
        @StringRes val resId: Int,
        override val id: Long = System.currentTimeMillis()
    ) : UiError()

    fun asString(context: Context): String = when (this) {
        is Message -> message
        is Resource -> context.getString(resId)
    }

    @Composable
    fun asComposableString(): String = when (this) {
        is Message -> message
        is Resource -> stringResource(resId)
    }

    companion object {
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