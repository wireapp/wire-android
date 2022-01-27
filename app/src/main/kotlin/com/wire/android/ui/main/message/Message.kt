package com.wire.android.ui.main.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.wire.android.ui.main.conversation.model.AvailabilityStatus
import com.wire.android.ui.main.conversation.model.Membership
import com.wire.android.ui.theme.body02
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun MessageItem(
    messages: List<Message>
) {
    LazyColumn(contentPadding = PaddingValues(8.dp)) {
        items(messages) { message ->
            Row {
                UserProfileAvatar()
                Column {
                    MessageHeader(message.messageHeader)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!message.isDeleted) {
                        MessageContent(message.messageContent)
                    }
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
                UserName(userName)

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
        text = userName,
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
    val mockMessages = listOf(
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.Guest,
                isLegalHold = true,
                time = "12.23pm",
                messageStatus = MessageStatus.Untouched
            ),
            messageContent = MessageContent.TextMessage(
                messageBody = MessageBody(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            ),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.Guest,
                isLegalHold = true,
                time = "12.23pm",
                messageStatus = MessageStatus.Deleted
            ),
            messageContent = MessageContent.ImageMessage("someUrl"),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.External,
                isLegalHold = false,
                time = "12.23pm",
                messageStatus = MessageStatus.Edited
            ),
            messageContent = MessageContent.ImageMessage("someUrl"),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.External,
                isLegalHold = false,
                time = "12.23pm",
                messageStatus = MessageStatus.Edited
            ),
            messageContent = MessageContent.ImageMessage("someUrl"),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.External,
                isLegalHold = false,
                time = "12.23pm",
                messageStatus = MessageStatus.Deleted
            ),
            messageContent = MessageContent.TextMessage(
                messageBody = MessageBody(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            ),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.External,
                isLegalHold = false,
                time = "12.23pm",
                messageStatus = MessageStatus.Edited
            ),
            messageContent = MessageContent.ImageMessage("someUrl"),
        ),
        Message(
            user = User("", AvailabilityStatus.Available),
            messageHeader = MessageHeader(
                userName = "Mateusz Pachulski",
                membership = Membership.External,
                isLegalHold = false,
                time = "12.23pm",
                messageStatus = MessageStatus.Edited
            ),
            messageContent = MessageContent.TextMessage(
                messageBody = MessageBody(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long" +
                            "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long" +
                            "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long" +
                            "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long" +
                            "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            ),
        ),
    )

    Message(
        mockMessages
    )
}

data class MessageHeader(
    val userName: String,
    val membership: Membership,
    val isLegalHold: Boolean,
    val time: String,
    val messageStatus: MessageStatus
)

enum class MessageStatus(val stringResourceId: Int) {
    Untouched(-1), Deleted(R.string.label_message_status_deleted), Edited(R.string.label_message_status_edited)
}

data class Message(
    val user: User,
    val messageHeader: MessageHeader,
    val messageContent: MessageContent,
) {
    val isDeleted = messageHeader.messageStatus == MessageStatus.Deleted
}

sealed class MessageContent {
    data class TextMessage(val messageBody: MessageBody) : MessageContent()
    data class ImageMessage(val imageUrl: String) : MessageContent()
}

data class MessageBody(
    val message: String
)

data class User(
    val avatarUrl: String = "",
    val availabilityStatus: AvailabilityStatus,
)





