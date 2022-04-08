package com.wire.android.ui.home.conversations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getUriFromDrawable
import com.wire.android.util.toBitmap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    onLongClicked: () -> Unit,
) {
    with(message) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    //TODO: implement some action onClick
                    onClick = { },
                    onLongClick = onLongClicked
                )
        ) {
            UserProfileAvatar(status = message.user.availabilityStatus)
            Column {
                MessageHeader(messageHeader)
                Spacer(modifier = Modifier.height(6.dp))
                if (!isDeleted) {
                    MessageContent(messageContent)
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
                Username(username)

                if (membership != Membership.None) {
                    Spacer(modifier = Modifier.width(6.dp))
                    MembershipQualifierLabel(membership)
                }

                if (isLegalHold) {
                    Spacer(modifier = Modifier.width(6.dp))
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
private fun MessageContent(messageContent: MessageContent) {
    when (messageContent) {
        is MessageContent.ImageMessage -> MessageImage(rawImgData = messageContent.rawImgData, messageContent.width, messageContent.height)
        is MessageContent.TextMessage -> MessageBody(messageBody = messageContent.messageBody)
    }
}

//TODO: replace with actual imageUrl loading probably with: https://coil-kt.github.io/coil/compose/
@Composable
fun MessageImage(rawImgData: ByteArray, realImgWidth: Int, realImgHeight: Int) {
    val width = MaterialTheme.wireDimensions.messageImageMaxWidth
    val height = width.value * realImgHeight.toFloat() / realImgWidth
    Image(
        painter = rememberAsyncImagePainter(
            rawImgData.toBitmap() ?: getUriFromDrawable(
                LocalContext.current,
                R.drawable.ic_gallery
            )
        ),
        alignment = Alignment.CenterStart,
        contentDescription = stringResource(R.string.content_description_image_message),
        modifier = Modifier.width(width).height(height.dp),
        contentScale = ContentScale.Crop
    )
}

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
// waiting for the backend to implement mapping logic for the MessageBody
@Composable
private fun MessageBody(messageBody: MessageBody) {
    Text(
        buildAnnotatedString {
            appendBody(messageBody = messageBody)
        }
    )
}

// TODO:we should provide the SpanStyle by LocalProvider to our Theme, later on
@Composable
private fun AnnotatedString.Builder.appendMentionLabel(label: String) {
    withStyle(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        append("$label ")
    }
}

@Composable
private fun AnnotatedString.Builder.appendBody(messageBody: MessageBody) {
    append(messageBody.message)
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.wireColorScheme.divider
                ),
                shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(
                horizontal = 4.dp,
                vertical = 2.dp
            )
    ) {
        Text(
            text = stringResource(id = messageStatus.stringResourceId),
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.labelText)
        )
    }
}

@Preview
@Composable
fun PreviewMessage() {
    MessageItem(
        mockMessageWithText
    ) {}
}
