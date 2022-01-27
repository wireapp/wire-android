package com.wire.android.ui.main.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversation.model.Membership
import com.wire.android.ui.main.message.mock.mockMessageWithText
import com.wire.android.ui.main.message.model.Message
import com.wire.android.ui.main.message.model.MessageBody
import com.wire.android.ui.main.message.model.MessageContent
import com.wire.android.ui.main.message.model.MessageHeader
import com.wire.android.ui.main.message.model.MessageStatus
import com.wire.android.ui.theme.body02
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun MessageItem(
    message: Message
) {
    with(message) {
        Row {
            UserProfileAvatar()
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

                Box(Modifier.fillMaxWidth()) {
                    MessageTimeLabel(
                        time, modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                }
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
        style = MaterialTheme.typography.body02
    )
}

@Composable
private fun MessageContent(messageContent: MessageContent) {
    when (messageContent) {
        is MessageContent.ImageMessage -> MessageImage(messageContent.imageUrl)
        is MessageContent.TextMessage -> MessageBody(messageBody = messageContent.messageBody)
    }
}

//TODO: replace with actual imageUrl loading probably with: https://coil-kt.github.io/coil/compose/
@Composable
fun MessageImage(imageUrl: String = "") {
    Image(
        painter = painterResource(R.drawable.mock_message_image), "",
        alignment = Alignment.CenterStart,
        modifier = Modifier
            .width(200.dp)
    )
}

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
// waiting for the backend to implement mapping logic for the MessageBody
@Composable
private fun MessageBody(messageBody: MessageBody) {
    Text(
        buildAnnotatedString {
            appendMentionLabel("@Mateusz Pachulski")
            appendBody(messageBody)
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
            .border(BorderStroke(1.dp, MaterialTheme.wireColorScheme.tertiaryButtonFocus), shape = RoundedCornerShape(4.dp))
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
    )
}
