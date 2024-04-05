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
package com.wire.android.ui.home.conversations.model

import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.UserId

sealed class UIQuotedMessage {

    object UnavailableData : UIQuotedMessage()

    data class UIQuotedData(
        val messageId: String,
        val senderId: UserId,
        val senderName: UIText,
        val originalMessageDateDescription: UIText,
        val editedTimeDescription: UIText?,
        val quotedContent: Content
    ) : UIQuotedMessage() {

        sealed interface Content

        data class Text(val value: String) : Content

        data class GenericAsset(
            val assetName: String?,
            val assetMimeType: String
        ) : Content

        data class DisplayableImage(
            val displayable: ImageAsset.PrivateAsset
        ) : Content

        data class Location(val locationName: String) : Content

        object AudioMessage : Content

        object Deleted : Content
        object Invalid : Content
    }
}

fun UIMessage.Regular.mapToQuotedContent(): UIQuotedMessage.UIQuotedData.Content? =
    when (val messageContent = messageContent) {
        is UIMessageContent.AssetMessage -> UIQuotedMessage.UIQuotedData.GenericAsset(
            assetName = messageContent.assetName,
            assetMimeType = messageContent.assetExtension
        )

        is UIMessageContent.RestrictedAsset -> UIQuotedMessage.UIQuotedData.GenericAsset(
            assetName = messageContent.assetName,
            assetMimeType = messageContent.mimeType
        )

        is UIMessageContent.TextMessage -> UIQuotedMessage.UIQuotedData.Text(
            value = messageContent.messageBody.message.asString(null)
        )

        is UIMessageContent.AudioAssetMessage -> UIQuotedMessage.UIQuotedData.AudioMessage

        is UIMessageContent.ImageMessage -> messageContent.asset?.let {
            UIQuotedMessage.UIQuotedData.DisplayableImage(
                displayable = messageContent.asset
            )
        }

        is UIMessageContent.Location -> with(messageContent) {
            UIQuotedMessage.UIQuotedData.Location(locationName = name)
        }

        else -> {
            appLogger.w("Attempting to reply to an unsupported message type of content = $messageContent")
            null
        }
    }
