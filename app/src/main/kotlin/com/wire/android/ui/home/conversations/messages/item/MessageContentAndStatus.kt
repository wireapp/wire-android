/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.model.Clickable
import com.wire.android.ui.common.applyIf
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
import okio.Path.Companion.toPath

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
    accent: Accent = Accent.Unknown,
) {
    val conversationAssetPathsViewModel: ConversationAssetPathsViewModel = when {
        LocalInspectionMode.current -> ConversationAssetPathsViewModelPreview
        else -> hiltViewModel<ConversationAssetPathsViewModelImpl>(key = message.conversationId.toString())
    }

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
    Row {
        Column(Modifier.applyIf(!messageStyle.isBubble()) { weight(1F) }) {
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
                accent = accent,
                conversationAssetPathsViewModel = conversationAssetPathsViewModel
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
    accent: Accent,
    conversationAssetPathsViewModel: ConversationAssetPathsViewModel
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> {
            val messageId = message.header.messageId
            val localAssetPath = conversationAssetPathsViewModel.localAssetPath(
                conversationId = message.conversationId,
                messageId = messageId,
                assetStatus = assetStatus,
                downloadIfNeeded = true
            )

            MessageImage(
                imgParams = messageContent.params,
                transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
                onImageClick = onImageClick,
                messageStyle = messageStyle,
                assetPath = localAssetPath?.toPath(normalize = true)
            )
        }

        is UIMessageContent.VideoMessage -> {
            val messageId = message.header.messageId
            val localAssetPath = conversationAssetPathsViewModel.localAssetPath(
                conversationId = message.conversationId,
                messageId = messageId,
                assetStatus = assetStatus
            )

            VideoMessage(
                assetSize = messageContent.assetSizeInBytes,
                assetName = messageContent.assetName,
                assetExtension = messageContent.assetExtension,
                assetDataPath = localAssetPath,
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
            val messageId = message.header.messageId
            val localAssetPath = conversationAssetPathsViewModel.localAssetPath(
                conversationId = message.conversationId,
                messageId = messageId,
                assetStatus = assetStatus
            )

            MessageAsset(
                assetName = messageContent.assetName,
                assetExtension = messageContent.assetExtension,
                assetSizeInBytes = messageContent.assetSizeInBytes,
                assetDataPath = localAssetPath,
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
                    attachments = messageContent.attachments,
                    messageStyle = messageStyle,
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
