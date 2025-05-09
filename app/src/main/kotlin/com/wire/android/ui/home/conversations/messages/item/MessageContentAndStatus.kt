package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.model.Clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle
import com.wire.android.ui.home.conversations.messages.QuotedUnavailable
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.location.LocationMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsView
import com.wire.android.ui.home.conversations.model.messagetypes.video.VideoMessage
import com.wire.android.util.launchGeoIntent
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
internal fun UIMessage.Regular.MessageContentAndStatus(
    message: UIMessage.Regular,
    assetStatus: AssetTransferStatus?,
    searchQuery: String,
    onAssetClicked: (String) -> Unit,
    onImageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onProfileClicked: (String) -> Unit,
    onLinkClicked: (String) -> Unit,
    onReplyClicked: (UIMessage.Regular) -> Unit,
    shouldDisplayMessageStatus: Boolean,
    conversationDetailsData: ConversationDetailsData,
) {
    val onAssetClickable = remember(message) {
        Clickable(enabled = isAvailable, onClick = {
            onAssetClicked(header.messageId)
        })
    }
    val onImageClickable = remember(message) {
        Clickable(enabled = isAvailable, onClick = {
            onImageClicked(message, source == MessageSource.Self)
        })
    }
    val onReplyClickable = remember(message) {
        Clickable {
            onReplyClicked(message)
        }
    }
    Row {
        Box(modifier = Modifier.weight(1F)) {
            MessageContent(
                message = message,
                messageContent = messageContent,
                searchQuery = searchQuery,
                assetStatus = assetStatus,
                onAssetClick = onAssetClickable,
                onImageClick = onImageClickable,
                onOpenProfile = onProfileClicked,
                onLinkClick = onLinkClicked,
                onReplyClick = onReplyClickable,
            )
        }
        if (isMyMessage && shouldDisplayMessageStatus) {
            MessageStatusIndicator(
                status = message.header.messageStatus.flowStatus,
                isGroupConversation = conversationDetailsData is ConversationDetailsData.Group,
                modifier = Modifier.padding(
                    top = if (message.isTextContentWithoutQuote) dimensions().spacing2x else dimensions().spacing4x,
                    start = dimensions().spacing8x
                )
            )
        } else {
            HorizontalSpace.x24()
        }
    }
}

@Suppress("ComplexMethod")
@Composable
private fun MessageContent(
    message: UIMessage.Regular,
    messageContent: UIMessageContent.Regular?,
    searchQuery: String,
    assetStatus: AssetTransferStatus?,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onReplyClick: Clickable,
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> {
            Column {
                MessageImage(
                    asset = messageContent.asset,
                    imgParams = ImageMessageParams(messageContent.width, messageContent.height),
                    transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                    onImageClick = onImageClick
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.VideoMessage -> {
            Column {
                VideoMessage(
                    assetSize = messageContent.assetSizeInBytes,
                    assetName = messageContent.assetName,
                    assetExtension = messageContent.assetExtension,
                    assetDataPath = messageContent.assetDataPath,
                    width = messageContent.width,
                    height = messageContent.height,
                    duration = messageContent.duration,
                    transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                    onVideoClick = onAssetClick,
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.TextMessage -> {
            Column {
                messageContent.messageBody.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            messageData = it,
                            clickable = onReplyClick
                        )

                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(style = QuotedMessageStyle.COMPLETE)
                    }
                    VerticalSpace.x4()
                }
                MessageBody(
                    messageBody = messageContent.messageBody,
                    searchQuery = searchQuery,
                    isAvailable = !message.isPending && message.isAvailable,
                    onOpenProfile = onOpenProfile,
                    buttonList = null,
                    messageId = message.header.messageId,
                    onLinkClick = onLinkClick,
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.Composite -> {
            Column {
                messageContent.messageBody?.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            messageData = it,
                            clickable = onReplyClick
                        )

                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(style = QuotedMessageStyle.COMPLETE)
                    }
                    VerticalSpace.x4()
                }
                MessageBody(
                    messageBody = messageContent.messageBody,
                    isAvailable = !message.isPending && message.isAvailable,
                    onOpenProfile = onOpenProfile,
                    buttonList = messageContent.buttonList,
                    messageId = message.header.messageId,
                    onLinkClick = onLinkClick
                )
            }
        }

        is UIMessageContent.AssetMessage -> {
            Column {
                MessageAsset(
                    assetName = messageContent.assetName,
                    assetExtension = messageContent.assetExtension,
                    assetSizeInBytes = messageContent.assetSizeInBytes,
                    assetDataPath = messageContent.assetDataPath,
                    assetTransferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                    onAssetClick = onAssetClick
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.RestrictedAsset -> {
            Column {
                when {
                    messageContent.mimeType.contains("image/") -> {
                        RestrictedAssetMessage(
                            R.drawable.ic_gallery,
                            stringResource(id = R.string.prohibited_images_message)
                        )
                    }

                    messageContent.mimeType.contains("video/") -> {
                        RestrictedAssetMessage(R.drawable.ic_video, stringResource(id = R.string.prohibited_videos_message))
                    }

                    messageContent.mimeType.contains("audio/") -> {
                        RestrictedAssetMessage(
                            R.drawable.ic_speaker_on,
                            stringResource(id = R.string.prohibited_audio_message)
                        )
                    }

                    else -> {
                        RestrictedGenericFileMessage(messageContent.assetName, messageContent.assetSizeInBytes)
                    }
                }
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.AudioAssetMessage -> {
            Column {
                AudioMessage(
                    audioMessageArgs = AudioMessageArgs(message.conversationId, message.header.messageId),
                    audioMessageDurationInMs = messageContent.audioMessageDurationInMs,
                    extension = messageContent.assetExtension,
                    size = messageContent.sizeInBytes,
                    assetTransferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.Location -> with(messageContent) {
            val context = LocalContext.current
            val locationUrl = stringResource(urlCoordinates, zoom, latitude, longitude)
            Column {
                LocationMessageContent(
                    locationName = name,
                    locationUrl = locationUrl,
                    onLocationClick = Clickable(
                        enabled = message.isAvailable,
                        onClick = { launchGeoIntent(latitude, longitude, name, locationUrl, context) },
                    )
                )
                PartialDeliveryInformation(deliveryStatus)
            }
        }

        is UIMessageContent.Multipart ->
            Column {
                if (messageContent.messageBody?.message?.asString()?.isNotEmpty() == true) {
                    MessageBody(
                        messageBody = messageContent.messageBody,
                        searchQuery = searchQuery,
                        isAvailable = !message.isPending && message.isAvailable,
                        onOpenProfile = onOpenProfile,
                        buttonList = null,
                        messageId = message.header.messageId,
                        onLinkClick = onLinkClick,
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing8x))
                }
                MultipartAttachmentsView(
                    message.conversationId,
                    messageContent.attachments
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }

        UIMessageContent.Deleted -> {}

        null -> {
            throw NullPointerException("messageContent is null")
        }
    }
}

@Composable
private fun PartialDeliveryInformation(deliveryStatus: DeliveryStatusContent) {
    (deliveryStatus as? DeliveryStatusContent.PartialDelivery)?.let { partialDelivery ->
        if (partialDelivery.hasFailures) {
            VerticalSpace.x4()
            MessageSentPartialDeliveryFailures(partialDelivery)
        }
    }
}
