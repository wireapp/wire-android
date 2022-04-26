package com.wire.android.util.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UIText {
    data class DynamicString(val value: String) : UIText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UIText()

    @Composable
    fun asString() = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id = resId, args)
    }

    fun asString(context: Context) = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, args)
    }
}
