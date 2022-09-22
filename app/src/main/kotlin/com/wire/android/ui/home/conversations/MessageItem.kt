package com.wire.android.ui.home.conversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.ImageMessageParams
import com.wire.android.ui.home.conversations.model.MessageAsset
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.RestrictedFileMessage
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.user.UserId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage,
    onLongClicked: (UIMessage) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onImageMessageClicked: (String, Boolean) -> Unit,
    onAvatarClicked: (MessageSource, UserId) -> Unit
) {
    with(message) {
        val fullAvatarOuterPadding = dimensions().userAvatarClickablePadding + dimensions().userAvatarStatusBorderSize
        Row(
            Modifier
                .customizeMessageBackground(message)
                .padding(
                    end = dimensions().spacing16x,
                    bottom = dimensions().messageItemBottomPadding - fullAvatarOuterPadding
                )
                .fillMaxWidth()
                .let {
                    if (!message.isDeleted) it.combinedClickable(
                        //TODO: implement some action onClick
                        onClick = { },
                        onLongClick = { onLongClicked(message) }
                    ) else it
                }
        ) {
            Spacer(Modifier.padding(start = dimensions().spacing8x - fullAvatarOuterPadding))
            val avatarClickable = remember {
                Clickable(enabled = message.messageHeader.userId != null) {
                    onAvatarClicked(message.messageSource, message.messageHeader.userId!!)
                }
            }
            UserProfileAvatar(
                avatarData = message.userAvatarData,
                clickable = avatarClickable
            )
            Spacer(Modifier.padding(start = dimensions().spacing16x - fullAvatarOuterPadding))
            Column {
                Spacer(modifier = Modifier.height(fullAvatarOuterPadding))
                MessageHeader(messageHeader)

                if (!isDeleted) {
                    if (!decryptionFailed) {
                        val currentOnAssetClicked = remember {
                            Clickable(enabled = true, onClick = {
                                onAssetMessageClicked(message.messageHeader.messageId)
                            }, onLongClick = {
                                onLongClicked(message)
                            })
                        }

                        val currentOnImageClick = remember {
                            Clickable(enabled = true, onClick = {
                                onImageMessageClicked(
                                    message.messageHeader.messageId,
                                    message.messageSource == MessageSource.Self
                                )
                            }, onLongClick = {
                                onLongClicked(message)
                            })
                        }
                        val onLongClick = remember { { onLongClicked(message) } }

                        MessageContent(
                            messageContent = messageContent,
                            onAssetClick = currentOnAssetClicked,
                            onImageClick = currentOnImageClick,
                            onLongClick = onLongClick
                        )
                    } else {
                        // Decryption failed for this message
                        MessageDecryptionFailure()
                    }
                }

                if (message.sendingFailed) {
                    MessageSendFailureWarning()
                }
            }
        }
    }
}

@Composable
private fun Modifier.customizeMessageBackground(
    message: UIMessage,
) = run {
    if (message.sendingFailed || message.receivingFailed) {
        background(MaterialTheme.wireColorScheme.messageErrorBackgroundColor)
    } else this
}

@Composable
private fun MessageHeader(messageHeader: MessageHeader) {
    with(messageHeader) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Username(username.asString(), modifier = Modifier.weight(weight = 1f, fill = false))

                    UserBadge(
                        membership = membership,
                        connectionState = connectionState,
                        startPadding = dimensions().spacing6x
                    )

                    if (isLegalHold) {
                        LegalHoldIndicator(modifier = Modifier.padding(start = dimensions().spacing6x))
                    }
                }
                MessageTimeLabel(
                    time = messageHeader.messageTime.formattedDate,
                    modifier = Modifier.padding(start = dimensions().spacing6x)
                )
            }
            MessageStatusLabel(messageStatus = messageStatus)
        }
    }
}

@Composable
private fun MessageTimeLabel(
    time: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun Username(username: String, modifier: Modifier = Modifier) {
    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MessageContent(
    messageContent: UIMessageContent?,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onLongClick: (() -> Unit)? = null
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> MessageImage(
            rawImgData = messageContent.imgData,
            imgParams = ImageMessageParams(messageContent.width, messageContent.height),
            assetUploadStatus = messageContent.uploadStatus,
            onImageClick = onImageClick
        )
        is UIMessageContent.TextMessage -> MessageBody(
            messageBody = messageContent.messageBody,
            onLongClick = onLongClick
        )
        is UIMessageContent.AssetMessage -> MessageAsset(
            assetName = messageContent.assetName,
            assetExtension = messageContent.assetExtension,
            assetSizeInBytes = messageContent.assetSizeInBytes,
            assetUploadStatus = messageContent.uploadStatus,
            assetDownloadStatus = messageContent.downloadStatus,
            onAssetClick = onAssetClick
        )
        is UIMessageContent.SystemMessage.MemberAdded -> {}
        is UIMessageContent.SystemMessage.MemberLeft -> {}
        is UIMessageContent.SystemMessage.MemberRemoved -> {}
        is UIMessageContent.RestrictedAsset -> {
            when {
                messageContent.mimeType.contains("image/") -> {
                    RestrictedAssetMessage(R.drawable.ic_gallery, stringResource(id = R.string.prohibited_images_message))
                }
                messageContent.mimeType.contains("video/") -> {
                    RestrictedAssetMessage(R.drawable.ic_video, stringResource(id = R.string.prohibited_videos_message))
                }
                messageContent.mimeType.contains("audio/") -> {
                    RestrictedAssetMessage(R.drawable.ic_speaker_on, stringResource(id = R.string.prohibited_audio_message))
                }
                else -> {
                    RestrictedFileMessage(messageContent.assetName, messageContent.assetSizeInBytes)
                }
            }
        }
        is UIMessageContent.SystemMessage.RenamedConversation -> {}
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        when (messageStatus) {
            MessageStatus.Deleted,
            is MessageStatus.Edited,
            MessageStatus.ReceiveFailure -> {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.wireColorScheme.divider
                            ),
                            shape = RoundedCornerShape(size = dimensions().spacing4x)
                        )
                        .padding(
                            horizontal = dimensions().spacing4x,
                            vertical = dimensions().spacing2x
                        )
                ) {
                    Text(
                        text = messageStatus.text.asString(),
                        style = LocalTextStyle.current.copy(color = MaterialTheme.wireColorScheme.labelText)
                    )
                }
            }
            MessageStatus.SendFailure, MessageStatus.Untouched, MessageStatus.DecryptionFailure -> {
            }
        }
    }
}

@Composable
private fun MessageSendFailureWarning() {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Row {
            Text(
                text = MessageStatus.SendFailure.text.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            Spacer(Modifier.width(dimensions().spacing4x))
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                    textDecoration = TextDecoration.Underline
                ),
                text = stringResource(R.string.label_try_again),
            )
        }
    }
}

@Composable
private fun MessageDecryptionFailure() {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_decryption_failure_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Row {
            Spacer(Modifier.height(dimensions().spacing4x))
            Text(
                text = MessageStatus.DecryptionFailure.text.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            Spacer(Modifier.width(dimensions().spacing4x))
            Text(
                modifier = Modifier
                    .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                    textDecoration = TextDecoration.Underline
                ),
                text = stringResource(R.string.label_learn_more),
            )
        }
    }
}
