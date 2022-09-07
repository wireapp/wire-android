package com.wire.android.ui.home.conversations

// TODO(refactor): Split into smaller viewmodel-specific messages
//                 For example, some for sending assets, some for downloadnig assets, etc.
sealed class ConversationSnackbarMessages {
    object ErrorPickingAttachment : ConversationSnackbarMessages()
    object ErrorMaxImageSize : ConversationSnackbarMessages()
    object ErrorSendingAsset : ConversationSnackbarMessages()
    object ErrorSendingImage : ConversationSnackbarMessages()
    object ErrorDownloadingAsset : ConversationSnackbarMessages()
    object ErrorOpeningAssetFile : ConversationSnackbarMessages()
    object ErrorDeletingMessage: ConversationSnackbarMessages()
    data class ErrorMaxAssetSize(val maxLimitInMB: Int) : ConversationSnackbarMessages()
    data class OnFileDownloaded(val assetName: String?) : ConversationSnackbarMessages()
}

sealed class MediaGallerySnackbarMessages {
    class OnImageDownloaded(val assetName: String? = null) : MediaGallerySnackbarMessages()
    object OnImageDownloadError : MediaGallerySnackbarMessages()
    object DeletingMessageError: MediaGallerySnackbarMessages()
}
