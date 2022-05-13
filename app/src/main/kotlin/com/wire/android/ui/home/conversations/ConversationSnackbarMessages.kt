package com.wire.android.ui.home.conversations

sealed class ConversationSnackbarMessages {
    object ErrorPickingAttachment : ConversationSnackbarMessages()
    object ErrorMaxImageSize : ConversationSnackbarMessages()
    object ErrorSendingAsset : ConversationSnackbarMessages()
    object ErrorSendingImage : ConversationSnackbarMessages()
    object ErrorOpeningAssetFile : ConversationSnackbarMessages()
    class ErrorMaxAssetSize(val maxLimitInMB: Int) : ConversationSnackbarMessages()
    class OnFileDownloaded(val assetName: String?) : ConversationSnackbarMessages()
}
