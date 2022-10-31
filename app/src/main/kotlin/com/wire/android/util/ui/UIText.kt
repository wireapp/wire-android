package com.wire.android.util.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.kalium.logic.data.message.MessageMention

sealed class UIText {
    data class DynamicString(
        val value: String,
        val mentions: List<MessageMention> = listOf()
    ) : UIText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val formatArgs: Any
    ) : UIText()

    @Suppress("SpreadOperator")
    @Composable
    fun asString() = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id = resId, *formatArgs)
    }

    @Suppress("SpreadOperator")
    fun asString(resources: Resources) = when (this) {
        is DynamicString -> value
        is StringResource -> resources.getString(resId, *formatArgs)
    }
}

fun String.toUIText() = UIText.DynamicString(this)
