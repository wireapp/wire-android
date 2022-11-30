package com.wire.android.util.extension

import com.wire.kalium.logic.data.conversation.ClientId

private const val REQUIRED_DISPLAY_LENGTH = 16

fun ClientId.formatAsString(): String {
    val actualLength = value.length

    val validatedValue = if (actualLength != REQUIRED_DISPLAY_LENGTH) {
        StringBuilder(value).insert(0, "0".repeat(REQUIRED_DISPLAY_LENGTH - actualLength)).toString()
    } else {
        value
    }

    return validatedValue.chunked(2).joinToString(separator = " ")
}
