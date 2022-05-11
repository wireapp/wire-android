package com.wire.android.ui.home.conversations

sealed class ConversationErrors {
    object ErrorPickingAttachment : ConversationErrors()
    object ErrorMaxImageSize : ConversationErrors()
    object ErrorSendingAsset : ConversationErrors()
    object ErrorSendingImage : ConversationErrors()
    class ErrorMaxAssetSize(val maxLimitInMB: Int) : ConversationErrors()
}
