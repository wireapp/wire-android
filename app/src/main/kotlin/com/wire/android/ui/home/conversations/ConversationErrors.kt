package com.wire.android.ui.home.conversations

sealed class ConversationErrors {
    object ErrorPickingAttachment : ConversationErrors()
    object ErrorMaxImageSize : ConversationErrors()
    object ErrorMaxAssetSize : ConversationErrors()
    object ErrorSendingSize : ConversationErrors()
}
