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

sealed class ConversationSnackbarMessages(override val uiText: UIText, override val actionLabel: UIText? = null) : SnackBarMessage {
    data object ErrorPickingAttachment : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_generic))
    data object ErrorSendingAsset : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_sending_asset))
    data object ErrorSendingImage : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_sending_image))
    data object ErrorDownloadingAsset : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_downloading_asset))
    data object ErrorOpeningAssetFile : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_opening_asset_file))
    data object ErrorDeletingMessage : ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_deleting_message))
    data object ErrorAssetRestriction : ConversationSnackbarMessages(UIText.StringResource(R.string.restricted_asset_error_toast_message))
    data class ErrorMaxAssetSize(val maxLimitInMB: Int) :
        ConversationSnackbarMessages(UIText.StringResource(R.string.error_conversation_max_asset_size_limit, maxLimitInMB))

    data class OnFileDownloaded(val assetName: String?) :
        ConversationSnackbarMessages(
            uiText = UIText.StringResource(R.string.conversation_on_file_downloaded, assetName ?: ""),
            actionLabel = UIText.StringResource(R.string.label_show)
        )

    data class OnResetSession(val text: UIText) : ConversationSnackbarMessages(text)
}

sealed class MediaGallerySnackbarMessages(override val uiText: UIText, override val actionLabel: UIText? = null) : SnackBarMessage {
    class OnImageDownloaded(val assetName: String? = null) : MediaGallerySnackbarMessages(
        uiText = UIText.StringResource(R.string.media_gallery_on_image_downloaded, assetName ?: ""),
        actionLabel = UIText.StringResource(R.string.label_show)
    )

    data object OnImageDownloadError : MediaGallerySnackbarMessages(UIText.StringResource(R.string.media_gallery_on_image_download_error))
    data object DeletingMessageError : MediaGallerySnackbarMessages(UIText.StringResource(R.string.error_conversation_deleting_message))
}
