package com.wire.android.feature.messaging

data class ChatMessage<Content : MessageContent>(val uid: String, val content: Content)
