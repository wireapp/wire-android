package com.wire.android.ui.home.conversations

sealed class ConversationErrors {
    object ERROR_PICKING_ATTACHMENT : ConversationErrors()
    object ERROR_MAX_IMAGE_SIZE : ConversationErrors()
    object ERROR_MAX_ASSET_SIZE : ConversationErrors()
    object ERROR_SENDING_IMAGE : ConversationErrors()
}
