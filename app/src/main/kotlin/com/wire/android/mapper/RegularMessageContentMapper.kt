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

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.DEFAULT_LOCATION_ZOOM
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageButton
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.theme.Accent
import com.wire.android.util.getVideoMetaData
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.SUPPORTED_VIDEO_ASSET_MIME_TYPES
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.DeliveryStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.hasValidData
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.util.fileExtension
import com.wire.kalium.logic.util.isGreaterThan
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import javax.inject.Inject

@Suppress("TooManyFunctions", "CyclomaticComplexMethod")
class RegularMessageMapper @Inject constructor(
    private val messageResourceProvider: MessageResourceProvider,
    private val isoFormatter: ISOFormatter,
) {

    fun mapMessage(
        message: Message.Regular,
        sender: User?,
        userList: List<User>
    ): UIMessageContent = when (val content = message.content) {
        is Asset -> {
            when (val metadata = content.value.metadata) {
                is AssetContent.AssetMetadata.Audio -> {
                    mapAudio(
                        assetContent = content.value,
                        metadata = metadata,
                        userList = userList,
                        deliveryStatus = message.deliveryStatus
                    )
                }

                is AssetContent.AssetMetadata.Image, is AssetContent.AssetMetadata.Video, null -> {
                    val assetMessageContentMetadata = AssetMessageContentMetadata(content.value)
                    toUIMessageContent(assetMessageContentMetadata, message, sender, userList, message.deliveryStatus)
                }
            }
        }
        // We are mapping regular knock message to system message, because it's UI is almost the same as system message
        is MessageContent.Knock -> UIMessageContent.SystemMessage.Knock(
            if (message.isSelfMessage) {
                UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            } else {
                sender?.name.orUnknownName()
            }, message.isSelfMessage
        )

        is MessageContent.RestrictedAsset -> toRestrictedAsset(
            content.mimeType,
            content.sizeInBytes,
            content.name,
            userList,
            message.deliveryStatus
        )

        is MessageContent.Composite -> {
            val text = content.textContent?.let { textContent ->
                val quotedMessage = textContent.quotedMessageDetails?.let { mapQuoteData(message.conversationId, it) }
                    ?: if (textContent.quotedMessageReference?.quotedMessageId != null) {
                        UIQuotedMessage.UnavailableData
                    } else {
                        null
                    }

                MessageBody(
                    message = UIText.DynamicString(textContent.value, content.textContent?.mentions.orEmpty()),
                    quotedMessage = quotedMessage
                )
            }

            UIMessageContent.Composite(
                messageBody = text,
                buttonList = content.buttonList.map {
                    MessageButton(
                        id = it.id,
                        text = it.text,
                        isSelected = it.isSelected
                    )
                }.toPersistentList()
            )
        }

        is MessageContent.Location -> toLocation(content, userList, message)

        is MessageContent.Multipart -> toMultipart(message.conversationId, content, userList, message.deliveryStatus)

        else -> toText(message.conversationId, content, userList, message.deliveryStatus)
    }

    private fun toLocation(
        content: MessageContent.Location,
        userList: List<User>,
        message: Message.Regular
    ) = UIMessageContent.Location(
        latitude = content.latitude,
        longitude = content.longitude,
        name = content.name.orEmpty(),
        zoom = content.zoom ?: DEFAULT_LOCATION_ZOOM,
        deliveryStatus = mapRecipientsFailure(userList, message.deliveryStatus)
    )

    private fun mapAudio(
        assetContent: AssetContent,
        metadata: AssetContent.AssetMetadata.Audio,
        userList: List<User>,
        deliveryStatus: DeliveryStatus,
    ): UIMessageContent {
        with(assetContent) {
            return UIMessageContent.AudioAssetMessage(
                assetName = name ?: "",
                assetExtension = name?.fileExtension() ?: mimeType,
                assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                audioMessageDurationInMs = metadata.durationMs ?: 0,
                deliveryStatus = mapRecipientsFailure(userList, deliveryStatus),
                sizeInBytes = sizeInBytes,
            )
        }
    }

    fun toText(
        conversationId: ConversationId,
        content: MessageContent,
        userList: List<User>,
        deliveryStatus: DeliveryStatus
    ): UIMessageContent.TextMessage {
        val messageTextContent = (content as? MessageContent.Text)

        val quotedMessage = messageTextContent?.quotedMessageDetails?.let { mapQuoteData(conversationId, it) }
            ?: if (messageTextContent?.quotedMessageReference?.quotedMessageId != null) {
                UIQuotedMessage.UnavailableData
            } else {
                null
            }

        return MessageBody(
            when (content) {
                is MessageContent.Text -> UIText.DynamicString(content.value, content.mentions)
                is MessageContent.Unknown -> content.typeName?.let {
                    UIText.StringResource(
                        messageResourceProvider.sentAMessageWithContent, it
                    )
                } ?: UIText.StringResource(R.string.sent_a_message_with_unknown_content)

                is MessageContent.FailedDecryption -> {
                    content.errorCode?.let {
                        UIText.StringResource(R.string.label_message_decryption_failure_message_with_error_code, it)
                    } ?: UIText.StringResource(R.string.label_message_decryption_failure_message)
                }

                else -> UIText.StringResource(R.string.sent_a_message_with_unknown_content)
            },
            quotedMessage = quotedMessage
        ).let { messageBody ->
            UIMessageContent.TextMessage(
                messageBody = messageBody,
                deliveryStatus = mapRecipientsFailure(userList, deliveryStatus)
            )
        }
    }

    private fun mapQuoteData(conversationId: ConversationId, details: MessageContent.QuotedMessageDetails) =
        UIQuotedMessage.UIQuotedData(
            messageId = details.messageId,
            senderId = details.senderId,
            senderName = details.senderName.orUnknownName(),
            senderAccent = Accent.fromAccentId(details.accentId),
            originalMessageDateDescription = UIText.StringResource(
                R.string.label_quote_original_message_date,
                isoFormatter.fromInstantToTimeFormatter(details.timeInstant)
            ),
            editedTimeDescription = details.editInstant?.let { instant ->
                UIText.StringResource(R.string.label_message_status_edited_with_date, isoFormatter.fromInstantToTimeFormatter(instant))
            },
            quotedContent = when (val quotedContent = details.quotedContent) {
                is MessageContent.QuotedMessageDetails.Asset -> when (AttachmentType.fromMimeTypeString(quotedContent.assetMimeType)) {
                    AttachmentType.IMAGE -> UIQuotedMessage.UIQuotedData.DisplayableImage(
                        ImageAsset.PrivateAsset(
                            conversationId,
                            details.messageId,
                            details.isQuotingSelfUser
                        )
                    )

                    AttachmentType.AUDIO -> UIQuotedMessage.UIQuotedData.AudioMessage
                    AttachmentType.VIDEO,
                    AttachmentType.GENERIC_FILE -> UIQuotedMessage.UIQuotedData.GenericAsset(
                        quotedContent.assetName,
                        quotedContent.assetMimeType
                    )
                }

                is MessageContent.QuotedMessageDetails.Text -> UIQuotedMessage.UIQuotedData.Text(UIText.DynamicString(quotedContent.value))
                is MessageContent.QuotedMessageDetails.Location -> UIQuotedMessage.UIQuotedData.Location(
                    quotedContent.locationName.orEmpty()
                )

                MessageContent.QuotedMessageDetails.Deleted -> UIQuotedMessage.UIQuotedData.Deleted
                MessageContent.QuotedMessageDetails.Invalid -> UIQuotedMessage.UIQuotedData.Invalid
            }
        )

    fun toUIMessageContent(
        assetMessageContentMetadata: AssetMessageContentMetadata,
        message: Message,
        sender: User?,
        userList: List<User>,
        deliveryStatus: DeliveryStatus
    ): UIMessageContent =
        with(assetMessageContentMetadata.assetMessageContent) {
            when {
                // If some of image data are still missing, we mark it as incomplete which won't be shown until we get missing data
                // But we also check if isnt our own message, if its our own, most likely the there was an error sending the image.
                assetMessageContentMetadata.isIncompleteImage() && sender !is SelfUser -> {
                    UIMessageContent.IncompleteAssetMessage
                }

                // If it's a displayable image with valid data, we delegate the download it right away to coil
                assetMessageContentMetadata.isDisplayableImage() -> {
                    UIMessageContent.ImageMessage(
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        asset = ImageAsset.PrivateAsset(
                            message.conversationId,
                            message.id,
                            sender is SelfUser
                        ),
                        width = assetMessageContentMetadata.imgWidth,
                        height = assetMessageContentMetadata.imgHeight,
                        deliveryStatus = mapRecipientsFailure(userList, deliveryStatus)
                    )
                }

                assetMessageContentMetadata.isVideo() -> {
                    val metaData = localData?.assetDataPath?.let { getVideoMetaData(it) }
                    UIMessageContent.VideoMessage(
                        assetName = name ?: "",
                        assetExtension = name?.split(".")?.last() ?: "",
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        assetDataPath = localData?.assetDataPath,
                        assetSizeInBytes = sizeInBytes,
                        deliveryStatus = mapRecipientsFailure(userList, deliveryStatus),
                        width = metaData?.width,
                        height = metaData?.height,
                        duration = metaData?.durationMs,
                    )
                }

                // It's a generic Asset Message so let's not download it yet
                else -> {
                    UIMessageContent.AssetMessage(
                        assetName = name ?: "",
                        assetExtension = name?.split(".")?.last() ?: "",
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        assetSizeInBytes = sizeInBytes,
                        assetDataPath = localData?.assetDataPath,
                        deliveryStatus = mapRecipientsFailure(userList, deliveryStatus)
                    )
                }
            }
        }

    private fun toRestrictedAsset(
        mimeType: String,
        assetSize: Long,
        assetName: String,
        userList: List<User>,
        deliveryStatus: DeliveryStatus
    ): UIMessageContent {
        return UIMessageContent.RestrictedAsset(
            mimeType = mimeType,
            assetSizeInBytes = assetSize,
            assetName = assetName,
            deliveryStatus = mapRecipientsFailure(userList, deliveryStatus)
        )
    }

    private fun toMultipart(
        conversationId: ConversationId,
        content: MessageContent.Multipart,
        userList: List<User>,
        deliveryStatus: DeliveryStatus
    ): UIMessageContent.Multipart {

        val quotedMessage = content.quotedMessageDetails?.let { mapQuoteData(conversationId = conversationId, details = it) }
            ?: if (content.quotedMessageReference?.quotedMessageId != null) {
                UIQuotedMessage.UnavailableData
            } else {
                null
            }

        return MessageBody(UIText.DynamicString(content.value ?: "", content.mentions), quotedMessage = quotedMessage).let { messageBody ->
            UIMessageContent.Multipart(
                messageBody = messageBody,
                attachments = content.attachments.toPersistentList(),
                deliveryStatus = mapRecipientsFailure(userList, deliveryStatus),
            )
        }
    }
}

class AssetMessageContentMetadata(val assetMessageContent: AssetContent) {
    val imgWidth
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.width
            else -> 0
        }

    val imgHeight
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.height
            else -> 0
        }

    fun isDisplayableImage(): Boolean = isDisplayableImageMimeType(assetMessageContent.mimeType) &&
            imgWidth.isGreaterThan(0) && imgHeight.isGreaterThan(0)

    fun isVideo(): Boolean = assetMessageContent.mimeType in SUPPORTED_VIDEO_ASSET_MIME_TYPES

    // Sometimes client receives two events for the same asset, first one with only part of the data ("preview" type from web),
    // so such asset shouldn't be shown until all the required data is received.
    fun isIncompleteImage(): Boolean = isDisplayableImage() && !assetMessageContent.remoteData.hasValidData()
}

private fun String?.orUnknownName(): UIText = when {
    this != null -> UIText.DynamicString(this)
    else -> UIText.StringResource(R.string.username_unavailable_label)
}

private fun mapRecipientsFailure(userList: List<User>, deliveryStatus: DeliveryStatus?): DeliveryStatusContent {
    return when (deliveryStatus) {
        is DeliveryStatus.PartialDelivery -> DeliveryStatusContent.PartialDelivery(
            failedRecipients = deliveryStatus.recipientsFailedDelivery
                .map { userId -> UIText.DynamicString(userList.findUser(userId = userId)?.name.orEmpty()) }
                .toPersistentList(),
            noClients = deliveryStatus.recipientsFailedWithNoClients
                .groupBy { it.domain }
                .mapValues { (_, userIds) -> userIds.map { UIText.DynamicString(userList.findUser(userId = it)?.name.orEmpty()) } }
                .toPersistentMap()
        )

        is DeliveryStatus.CompleteDelivery, null -> DeliveryStatusContent.CompleteDelivery
    }
}
