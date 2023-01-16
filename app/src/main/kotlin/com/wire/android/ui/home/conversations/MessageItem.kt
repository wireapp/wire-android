package com.wire.android.ui.home.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import com.google.accompanist.flowlayout.FlowRow
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.user.UserId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemTest(
    message: UIMessage,
    onOpenProfile: (String) -> Unit,
    onLongClicked: (UIMessage) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    messageContent: @Composable () -> Unit
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
            MessageAvatar(
                fullAvatarOuterPadding = fullAvatarOuterPadding,
                messageHeader = message.messageHeader,
                userAvatarData = message.userAvatarData,
                onOpenProfile = onOpenProfile
            )
            Column {
                Spacer(modifier = Modifier.height(fullAvatarOuterPadding))
                MessageHeader(messageHeader = messageHeader)

                if (!isDeleted) {
                    if (!decryptionFailed) {
                        messageContent()
                        MessageFooter(
                            messageFooter = messageFooter,
                            onReactionClicked = onReactionClicked
                        )
                    } else {
                        MessageDecryptionFailure(
                            messageHeader = messageHeader,
                            decryptionStatus = messageHeader.messageStatus as MessageStatus.DecryptionFailure,
                            onResetSessionClicked = onResetSessionClicked
                        )
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
private fun MessageAvatar(
    fullAvatarOuterPadding: Dp,
    messageHeader: MessageHeader,
    userAvatarData: UserAvatarData,
    onOpenProfile: (String) -> Unit
) {
    val isProfileRedirectEnabled =
        messageHeader.userId != null
                && !(messageHeader.isSenderDeleted || messageHeader.isSenderUnavailable)

    val avatarClickable = remember {
        Clickable(enabled = isProfileRedirectEnabled) {
            onOpenProfile(messageHeader.userId!!.toString())
        }
    }

    Spacer(Modifier.padding(start = dimensions().spacing8x - fullAvatarOuterPadding))
    UserProfileAvatar(
        avatarData = userAvatarData,
        clickable = avatarClickable
    )
    Spacer(Modifier.padding(start = dimensions().spacing16x - fullAvatarOuterPadding))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage,
    onLongClicked: (UIMessage) -> Unit,
    currentlyPlayedAudioMessage: String? = null,
    isAudioPlaying: Boolean = false,
    onAssetMessageClicked: (String) -> Unit,
    onImageMessageClicked: (String, Boolean) -> Unit,
    onAudioClick: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit
) {
    with(message) {
        val fullAvatarOuterPadding = dimensions().userAvatarClickablePadding + dimensions().userAvatarStatusBorderSize
        Column { }
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

            val isProfileRedirectEnabled =
                message.messageHeader.userId != null
                        && !(message.messageHeader.isSenderDeleted || message.messageHeader.isSenderUnavailable)

            val avatarClickable = remember {
                Clickable(enabled = isProfileRedirectEnabled) {
                    onOpenProfile(message.messageHeader.userId!!.toString())
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
                            messageId = message.messageHeader.messageId,
                            isAudioPlaying = isAudioPlaying,
                            currentlyPlayedAudioMessageId = currentlyPlayedAudioMessage,
                            messageContent = messageContent,
                            onAssetClick = currentOnAssetClicked,
                            onImageClick = currentOnImageClick,
                            onAudioClick = { onAudioClick(messageHeader.messageId) },
                            onLongClick = onLongClick,
                            onOpenProfile = onOpenProfile
                        )
                        MessageFooter(
                            messageFooter,
                            onReactionClicked
                        )
                    } else {
                        MessageDecryptionFailure(
                            messageHeader = messageHeader,
                            decryptionStatus = messageHeader.messageStatus as MessageStatus.DecryptionFailure,
                            onResetSessionClicked = onResetSessionClicked
                        )
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
private fun MessageHeader(
    messageHeader: MessageHeader,
) {
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
                        startPadding = dimensions().spacing6x,
                        isDeleted = isSenderDeleted
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
private fun MessageFooter(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit
) {
    FlowRow(
        mainAxisSpacing = dimensions().spacing4x,
        crossAxisSpacing = dimensions().spacing6x,
        modifier = Modifier.padding(vertical = dimensions().spacing4x)
    ) {
        messageFooter.reactions.entries
            .sortedBy { it.key }
            .forEach {
                val reaction = it.key
                val count = it.value
                ReactionPill(
                    emoji = reaction,
                    count = count,
                    isOwn = messageFooter.ownReactions.contains(reaction),
                    onTap = {
                        onReactionClicked(messageFooter.messageId, reaction)
                    }
                )
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
    messageId: String,
    currentlyPlayedAudioMessageId: String?,
    isAudioPlaying: Boolean = false,
    messageContent: UIMessageContent?,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onAudioClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> MessageImage(
            asset = messageContent.asset,
            imgParams = ImageMessageParams(messageContent.width, messageContent.height),
            uploadStatus = messageContent.uploadStatus,
            downloadStatus = messageContent.downloadStatus,
            onImageClick = onImageClick
        )

        is UIMessageContent.TextMessage -> {
            MessageText(
                messageBody = messageContent.messageBody,
                onLongClick = onLongClick,
                onOpenProfile = onOpenProfile
            )
        }

        is UIMessageContent.AssetMessage -> MessageGenericAsset(
            assetName = messageContent.assetName,
            assetExtension = messageContent.assetExtension,
            assetSizeInBytes = messageContent.assetSizeInBytes,
            assetUploadStatus = messageContent.uploadStatus,
            assetDownloadStatus = messageContent.downloadStatus,
            onAssetClick = onAssetClick
        )

        is UIMessageContent.RestrictedAsset -> {
            MessageRestrictedAsset(
                assetName = messageContent.assetName,
                assetSizeInBytes = messageContent.assetSizeInBytes,
                mimeType = messageContent.mimeType
            )
        }

        is UIMessageContent.AudioAssetMessage -> {
//            AudioMessage(
//                audioMessageDuration = AudioMessageDuration(durationMs = 0, currentPositionMs = 0),
//                audioMediaPlayingState = AudioMediaPlayingState.Paused,
//                onPlayAudioMessage = { onAudioClick() }) { }
        }

        is UIMessageContent.SystemMessage.MemberAdded -> {}
        is UIMessageContent.SystemMessage.MemberLeft -> {}
        is UIMessageContent.SystemMessage.MemberRemoved -> {}
        is UIMessageContent.SystemMessage.RenamedConversation -> {}
        is UIMessageContent.SystemMessage.TeamMemberRemoved -> {}
        is UIMessageContent.SystemMessage.CryptoSessionReset -> {}
        is UIMessageContent.PreviewAssetMessage -> {}
        is UIMessageContent.SystemMessage.MissedCall.YouCalled -> {}
        is UIMessageContent.SystemMessage.MissedCall.OtherCalled -> {}
        null -> {
            throw NullPointerException("messageContent is null")
        }
    }
}

@Composable
fun MessageText(
    messageBody: MessageBody,
    onLongClick: (() -> Unit)?,
    onOpenProfile: (String) -> Unit
) {
    messageBody.quotedMessage?.let {
        VerticalSpace.x4()
        QuotedMessage(it)
        VerticalSpace.x4()
    }
    MessageBody(
        messageBody = messageBody,
        onLongClick = onLongClick,
        onOpenProfile = onOpenProfile
    )
}

@Composable
fun MessageRestrictedAsset(
    assetName: String,
    assetSizeInBytes: Long,
    mimeType: String
) {
    when {
        mimeType.contains("image/") -> {
            RestrictedAssetMessage(R.drawable.ic_gallery, stringResource(id = R.string.prohibited_images_message))
        }

        mimeType.contains("video/") -> {
            RestrictedAssetMessage(R.drawable.ic_video, stringResource(id = R.string.prohibited_videos_message))
        }

        mimeType.contains("audio/") -> {
            RestrictedAssetMessage(R.drawable.ic_speaker_on, stringResource(id = R.string.prohibited_audio_message))
        }

        else -> {
            RestrictedGenericFileMessage(assetName, assetSizeInBytes)
        }
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    when (messageStatus) {
        MessageStatus.Deleted,
        is MessageStatus.Edited,
        MessageStatus.ReceiveFailure -> StatusBox(messageStatus.text.asString())

        is MessageStatus.DecryptionFailure,
        MessageStatus.SendFailure, MessageStatus.Untouched -> {
            /** Don't display anything **/
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
//      todo to uncomment this after we have the functionality of resend the message
//              Text(
//                modifier = Modifier.fillMaxWidth(),
//                style = LocalTextStyle.current.copy(
//                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
//                    textDecoration = TextDecoration.Underline
//                ),
//                text = stringResource(R.string.label_try_again),
//            )
        }
    }
}

@Composable
private fun MessageDecryptionFailure(
    messageHeader: MessageHeader,
    decryptionStatus: MessageStatus.DecryptionFailure,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_decryption_failure_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            VerticalSpace.x4()
            Text(
                text = decryptionStatus.text.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            Text(
                modifier = Modifier
                    .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                    textDecoration = TextDecoration.Underline
                ),
                text = stringResource(R.string.label_learn_more),
            )
            VerticalSpace.x4()
            Text(
                text = stringResource(R.string.label_message_decryption_failure_informative_message),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            if (!decryptionStatus.isDecryptionResolved) {
                Row {
                    WireSecondaryButton(
                        text = stringResource(R.string.label_reset_session),
                        onClick = { messageHeader.userId?.let { userId -> onResetSessionClicked(userId, messageHeader.clientId?.value) } },
                        minHeight = dimensions().spacing32x,
                        fillMaxWidth = false,
                    )
                }
            } else {
                VerticalSpace.x8()
            }
        }
    }
}
