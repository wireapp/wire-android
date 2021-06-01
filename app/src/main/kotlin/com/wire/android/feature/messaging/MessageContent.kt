package com.wire.android.feature.messaging

sealed class MessageContent {

    data class Text(
        val text: String
    ) : MessageContent()

}
