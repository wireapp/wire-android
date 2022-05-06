package com.wire.android.ui.home.conversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.ImageMessageParams
import com.wire.android.ui.home.conversations.model.MessageAsset
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageViewWrapper,
    onLongClicked: () -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onImageMessageClicked: (String) -> Unit
) {
    with(message) {
        Row(
            Modifier
                .padding(
                    end = MaterialTheme.wireDimensions.spacing16x,
                    bottom = MaterialTheme.wireDimensions.messageItemBottomPadding
                )
                .fillMaxWidth()
                .combinedClickable(
                    //TODO: implement some action onClick
                    onClick = { },
                    onLongClick = onLongClicked
                )
        ) {
            Spacer(Modifier.padding(start = dimensions().spacing2x))
            UserProfileAvatar(
                userAvatarAsset = message.user.avatarAsset,
                status = message.user.availabilityStatus
            )
            Spacer(Modifier.padding(start = dimensions().spacing12x))
            Column {
                MessageHeader(messageHeader)
                Spacer(modifier = Modifier.height(dimensions().spacing6x))
                if (!isDeleted) {
                    MessageContent(messageContent,
                        onAssetClick = { assetId ->
                            onAssetMessageClicked(message.messageHeader.messageId, assetId)
                        },
                        onImageClick = {
                            onImageMessageClicked(message.messageHeader.messageId)
                        })
                }
            }
        }
    }
}

@Composable
private fun MessageHeader(messageHeader: MessageHeader) {
    with(messageHeader) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Username(username.asString())

                if (membership != Membership.None) {
                    Spacer(modifier = Modifier.width(dimensions().spacing6x))
                    MembershipQualifierLabel(membership)
                }

                if (isLegalHold) {
                    Spacer(modifier = Modifier.width(dimensions().spacing6x))
                    LegalHoldIndicator()
                }
/*
for now this feature is disabled as Wolfgang suggested
Box(Modifier.fillMaxWidth()) {
MessageTimeLabel(
time, modifier = Modifier
.align(Alignment.CenterEnd)
.padding(end = 8.dp)
)
}
*/
            }
        }
        if (messageStatus != MessageStatus.Untouched) {
            MessageStatusLabel(messageStatus = messageStatus)
        }
    }
}

//TODO: just a mock label, later when back-end is ready we are going to format it correctly, probably not as a String?
@Composable
private fun MessageTimeLabel(time: String, modifier: Modifier) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
        modifier = modifier
    )
}

@Composable
private fun Username(username: String) {
    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02
    )
}

@Composable
private fun MessageContent(messageContent: MessageContent?, onAssetClick: (String) -> Unit, onImageClick: () -> Unit = {}) {
    when (messageContent) {
        is MessageContent.ImageMessage -> MessageImage(
            rawImgData = messageContent.rawImgData,
            imgParams = ImageMessageParams(messageContent.width, messageContent.height),
            onImageClick = { onImageClick() }
        )
        is MessageContent.TextMessage -> MessageBody(messageBody = messageContent.messageBody)
        is MessageContent.AssetMessage -> MessageAsset(
            assetName = messageContent.assetName.split(".").dropLast(1).joinToString("."),
            assetExtension = messageContent.assetExtension,
            assetSizeInBytes = messageContent.assetSizeInBytes,
            onAssetClick = { onAssetClick(messageContent.assetId) }
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
