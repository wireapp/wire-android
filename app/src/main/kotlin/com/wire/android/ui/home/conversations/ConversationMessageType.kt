package com.wire.android.ui.home.conversations

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class ConversationSnackbarMessages(override val uiText: UIText) : SnackBarMessage {
    object ErrorPickingAttachment : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_generic))
    object ErrorMaxImageSize : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_max_image_size_limit))
    object ErrorSendingAsset : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_sending_asset))
    object ErrorSendingImage : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_sending_image))
    object ErrorDownloadingAsset : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_downloading_asset))
    object ErrorOpeningAssetFile : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_opening_asset_file))
    object ErrorDeletingMessage : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_deleting_message))
    data class ErrorMaxAssetSize(val maxLimitInMB: Int) :
        ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_max_asset_size_limit, maxLimitInMB))

    data class OnFileDownloaded(val assetName: String?) :
        ConversationSnackbarMessages(UIText.StringResource(R.string.conversation_on_file_downloaded, assetName ?: ""))

    data class OnResetSession(val text: UIText) : ConversationSnackbarMessages(text)
}

sealed class MediaGallerySnackbarMessages(override val uiText: UIText) : SnackBarMessage {
    class OnImageDownloaded(val assetName: String? = null) :
        MediaGallerySnackbarMessages(UIText.StringResource(R.string.media_gallery_on_image_downloaded, assetName ?: ""))

    object OnImageDownloadError : MediaGallerySnackbarMessages(UIText.StringResource(R.string.media_gallery_on_image_download_error))
    object DeletingMessageError : MediaGallerySnackbarMessages(UIText.StringResource(R.string.error_conversation_deleting_message))
}
