/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class ConversationSnackbarMessages(override val uiText: UIText) : SnackBarMessage {
    object ErrorPickingAttachment : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_generic))
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
