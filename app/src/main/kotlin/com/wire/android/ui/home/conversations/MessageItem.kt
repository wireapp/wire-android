package com.wire.android.ui.home.conversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.DeletedMessage
import com.wire.android.ui.home.conversations.model.ImageMessageParams
import com.wire.android.ui.home.conversations.model.MessageAsset
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage,
    onLongClicked: (UIMessage) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onImageMessageClicked: (String, Boolean) -> Unit
) {
    with(message) {
        Row(
            Modifier
                .padding(
                    end = dimensions().spacing16x,
                    bottom = dimensions().messageItemBottomPadding - dimensions().userAvatarClickablePadding
                )
                .fillMaxWidth()
                .combinedClickable(
                    //TODO: implement some action onClick
                    onClick = { },
                    onLongClick = { onLongClicked(message) }
                )
        ) {
            Spacer(Modifier.padding(start = dimensions().spacing8x - dimensions().userAvatarClickablePadding))
            UserProfileAvatar(
                avatarData = UserAvatarData(message.userAvatarData.asset, message.userAvatarData.availabilityStatus)
            )
            Spacer(Modifier.padding(start = dimensions().spacing16x - dimensions().userAvatarClickablePadding))
            Column {
                Spacer(modifier = Modifier.height(dimensions().userAvatarClickablePadding))
                MessageHeader(messageHeader)
                if (!isDeleted) {
                    MessageContent(messageContent,
                        onAssetClick = { onAssetMessageClicked(message.messageHeader.messageId) },
                        onImageClick = {
                            onImageMessageClicked(
                                message.messageHeader.messageId,
                                message.messageSource == MessageSource.Self
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageHeader(messageHeader: MessageHeader) {
    with(messageHeader) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Username(username.asString(), modifier = Modifier.weight(weight = 1F))

                if (membership != Membership.None) {
                    Spacer(modifier = Modifier.width(dimensions().spacing6x))
                    MembershipQualifierLabel(membership)
                }

                if (isLegalHold) {
                    Spacer(modifier = Modifier.width(dimensions().spacing6x))
                    LegalHoldIndicator()
                }

                MessageTimeLabel(messageHeader.time)
            }
        }
        if (messageStatus != MessageStatus.Untouched) {
            MessageStatusLabel(messageStatus = messageStatus)
        }
    }
}

@Composable
private fun MessageTimeLabel(time: String) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
    )
}

@Composable
private fun Username(username: String, modifier: Modifier) {
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
    messageContent: MessageContent?,
    onAssetClick: () -> Unit,
    onImageClick: () -> Unit = {}
) {
    when (messageContent) {
        is MessageContent.ImageMessage -> MessageImage(
            rawImgData = messageContent.rawImgData,
            imgParams = ImageMessageParams(messageContent.width, messageContent.height),
            onImageClick = { onImageClick() }
        )
        is MessageContent.TextMessage -> MessageBody(
            messageBody = messageContent.messageBody
        )
        is MessageContent.DeletedMessage -> DeletedMessage()
        is MessageContent.AssetMessage -> MessageAsset(
            assetName = messageContent.assetName.split(".").dropLast(1).joinToString("."),
            assetExtension = messageContent.assetExtension,
            assetSizeInBytes = messageContent.assetSizeInBytes,
            assetDownloadStatus = messageContent.downloadStatus,
            onAssetClick = { onAssetClick() }
        )
        is MessageContent.EditedMessage -> MessageBody(
            messageBody = messageContent.messageBody,
            editTime = messageContent.editTimeStamp
        )
        else -> {}
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        if (messageStatus != MessageStatus.SendFailure) {
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
                    text = stringResource(id = messageStatus.stringResourceId),
                    style = LocalTextStyle.current.copy(color = MaterialTheme.wireColorScheme.labelText)
                )
            }
        } else {
            Row {
                Text(
                    text = stringResource(id = messageStatus.stringResourceId),
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
}
