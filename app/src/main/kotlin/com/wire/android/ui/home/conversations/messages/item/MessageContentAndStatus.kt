package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Column
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
import com.wire.android.ui.home.conversations.messages.QuotedStyle
import com.wire.android.ui.home.conversations.messages.QuotedUnavailable
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIMessageContent.PartialDeliverable
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.location.LocationMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsView
import com.wire.android.ui.home.conversations.model.messagetypes.video.VideoMessage
import com.wire.android.ui.theme.Accent
import com.wire.android.util.launchGeoIntent
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
internal fun UIMessage.Regular.MessageContentAndStatus(
    message: UIMessage.Regular,
    assetStatus: AssetTransferStatus?,
    searchQuery: String,
    messageStyle: MessageStyle,
    onAssetClicked: (String) -> Unit,
    onImageClicked: (UIMessage.Regular, Boolean, String?) -> Unit,
    onProfileClicked: (String) -> Unit,
    onLinkClicked: (String) -> Unit,
    onReplyClicked: (UIMessage.Regular) -> Unit,
    shouldDisplayMessageStatus: Boolean,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
    accent: Accent = Accent.Unknown,
) {
    val onAssetClickable = remember(message) {
        Clickable(enabled = isAvailable, onClick = {
            onAssetClicked(header.messageId)
        })
    }

    val onImageClickable = remember(message) {
        Clickable(enabled = isAvailable, onClick = {
            onImageClicked(message, source == MessageSource.Self, null)
        })
    }

    val onMultipartImageClickable: (String) -> Unit = remember(message) {
        {
            onImageClicked(message, source == MessageSource.Self, it)
        }
    }

    val onReplyClickable = remember(message) {
        Clickable {
            onReplyClicked(message)
        }
    }
    Column(
        modifier
    ) {
        MessageContent(
            message = message,
            messageContent = messageContent,
            searchQuery = searchQuery,
            assetStatus = assetStatus,
            onAssetClick = onAssetClickable,
            onImageClick = onImageClickable,
            onMultipartImageClick = onMultipartImageClickable,
                onOpenProfile = onProfileClicked,
                onLinkClick = onLinkClicked,
                onReplyClick = onReplyClickable,
                messageStyle = messageStyle,
                accent = accent
            )
            if (!messageStyle.isBubble()) {
                if (messageContent is PartialDeliverable && messageContent.deliveryStatus.hasAnyFailures) {
                    PartialDeliveryInformation(messageContent.deliveryStatus, messageStyle)
                }
            }
        }
        if (!messageStyle.isBubble()) {
            if (isMyMessage && shouldDisplayMessageStatus) {
                MessageStatusIndicator(
                    status = message.header.messageStatus.flowStatus,
                    isGroupConversation = conversationDetailsData is ConversationDetailsData.Group,
                    messageStyle = messageStyle,
                    modifier = Modifier.padding(
                        top = if (message.isTextContentWithoutQuote) dimensions().spacing2x else dimensions().spacing4x,
                        start = dimensions().spacing8x
                    )
                )
            } else {
                HorizontalSpace.x24()
            }
        } else {
            if (message.isTextContentWithoutQuote) {
                VerticalSpace.x2()
            } else {
                VerticalSpace.x4()
        }
    }
}

@Suppress("ComplexMethod")
@Composable
private fun MessageContent(
    message: UIMessage.Regular,
    messageContent: UIMessageContent.Regular?,
    searchQuery: String,
    messageStyle: MessageStyle,
    assetStatus: AssetTransferStatus?,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onMultipartImageClick: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onReplyClick: Clickable,
    accent: Accent
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> {
            MessageImage(
                asset = messageContent.asset,
                imgParams = messageContent.params,
                transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                onImageClick = onImageClick,
                messageStyle = messageStyle
            )
        }

        is UIMessageContent.VideoMessage -> {
            VideoMessage(
                assetSize = messageContent.assetSizeInBytes,
                assetName = messageContent.assetName,
                assetExtension = messageContent.assetExtension,
                assetDataPath = messageContent.assetDataPath,
                params = messageContent.params,
                duration = messageContent.duration,
                transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                onVideoClick = onAssetClick,
                messageStyle = messageStyle

            )
        }

        is UIMessageContent.TextMessage -> {
            Column {
                messageContent.messageBody.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            conversationId = message.conversationId,
                            messageData = it,
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = it.senderAccent
                            ),
                            clickable = onReplyClick
                        )

                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = Accent.Unknown
                            )
                        )
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
                    messageStyle = messageStyle,
                    accent = accent
                )
            }
        }

        is UIMessageContent.Composite -> {
            Column {
                messageContent.messageBody?.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            conversationId = message.conversationId,
                            messageData = it,
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = it.senderAccent
                            ),
                            clickable = onReplyClick
                        )

                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = Accent.Unknown
                            )
                        )
                    }
                    VerticalSpace.x4()
                }
                MessageBody(
                    messageBody = messageContent.messageBody,
                    isAvailable = !message.isPending && message.isAvailable,
                    onOpenProfile = onOpenProfile,
                    buttonList = messageContent.buttonList,
                    messageId = message.header.messageId,
                    onLinkClick = onLinkClick,
                    messageStyle = messageStyle,
                    accent = accent
                )
            }
        }

        is UIMessageContent.AssetMessage -> {
            MessageAsset(
                assetName = messageContent.assetName,
                assetExtension = messageContent.assetExtension,
                assetSizeInBytes = messageContent.assetSizeInBytes,
                assetDataPath = messageContent.assetDataPath,
                assetTransferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                onAssetClick = onAssetClick,
                messageStyle = messageStyle
            )
        }

        is UIMessageContent.RestrictedAsset -> {
            when {
                messageContent.mimeType.contains("image/") -> {
                    RestrictedAssetMessage(
                        assetTypeIcon = R.drawable.ic_gallery,
                        restrictedAssetMessage = stringResource(id = R.string.prohibited_images_message),
                        messageStyle = messageStyle
                    )
                }

                messageContent.mimeType.contains("video/") -> {
                    RestrictedAssetMessage(
                        assetTypeIcon = R.drawable.ic_video,
                        restrictedAssetMessage = stringResource(id = R.string.prohibited_videos_message),
                        messageStyle = messageStyle
                    )
                }

                messageContent.mimeType.contains("audio/") -> {
                    RestrictedAssetMessage(
                        assetTypeIcon = R.drawable.ic_speaker_on,
                        restrictedAssetMessage = stringResource(id = R.string.prohibited_audio_message),
                        messageStyle = messageStyle
                    )
                }

                else -> {
                    RestrictedGenericFileMessage(
                        fileName = messageContent.assetName,
                        fileSize = messageContent.assetSizeInBytes,
                        messageStyle = messageStyle
                    )
                }
            }
        }

        is UIMessageContent.AudioAssetMessage -> {
            AudioMessage(
                audioMessageArgs = AudioMessageArgs(message.conversationId, message.header.messageId),
                audioMessageDurationInMs = messageContent.audioMessageDurationInMs,
                extension = messageContent.assetExtension,
                size = messageContent.sizeInBytes,
                assetTransferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                messageStyle = messageStyle
            )
        }

        is UIMessageContent.Location -> with(messageContent) {
            val context = LocalContext.current
            val locationUrl = stringResource(urlCoordinates, zoom, latitude, longitude)
            LocationMessageContent(
                locationName = name,
                locationUrl = locationUrl,
                onLocationClick = Clickable(
                    enabled = message.isAvailable,
                    onClick = { launchGeoIntent(latitude, longitude, name, locationUrl, context) },
                ),
                messageStyle = messageStyle
            )
        }

        is UIMessageContent.Multipart ->
            Column {
                messageContent.messageBody?.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            conversationId = message.conversationId,
                            messageData = it,
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = it.senderAccent
                            ),
                            clickable = onReplyClick
                        )

                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(
                            style = QuotedMessageStyle(
                                quotedStyle = QuotedStyle.COMPLETE,
                                messageStyle = messageStyle,
                                selfAccent = accent,
                                senderAccent = Accent.Unknown
                            )
                        )
                    }
                    VerticalSpace.x4()
                }
                if (messageContent.messageBody?.message?.asString()?.isNotEmpty() == true) {
                    MessageBody(
                        messageBody = messageContent.messageBody,
                        searchQuery = searchQuery,
                        isAvailable = !message.isPending && message.isAvailable,
                        onOpenProfile = onOpenProfile,
                        buttonList = null,
                        messageId = message.header.messageId,
                        onLinkClick = onLinkClick,
                        messageStyle = messageStyle,
                        accent = accent
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing8x))
                }
                MultipartAttachmentsView(
                    conversationId = message.conversationId,
                    attachments = messageContent.attachments,
                    messageStyle = messageStyle,
                    accent = accent,
                    onImageAttachmentClick = onMultipartImageClick
                )
            }

        UIMessageContent.Deleted -> {}

        null -> {
            throw NullPointerException("messageContent is null")
        }
    }
}

@Composable
fun PartialDeliveryInformation(deliveryStatus: DeliveryStatusContent?, messageStyle: MessageStyle) {
    (deliveryStatus as? DeliveryStatusContent.PartialDelivery)?.let { partialDelivery ->
        if (partialDelivery.hasFailures) {
            VerticalSpace.x4()
            MessageSentPartialDeliveryFailures(partialDelivery, messageStyle)
        }
    }
}
