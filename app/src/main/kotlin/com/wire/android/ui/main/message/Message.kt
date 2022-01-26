package com.wire.android.ui.main.message

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.main.conversation.model.AvailabilityStatus
import com.wire.android.ui.main.conversation.model.Membership

@Composable
fun Message(
    message: Message
) {
    Row {
        MessageHeader()
    }
}


@Composable
private fun MessageHeader() {

}

@Composable
private fun MessageBody() {

}

@Composable
private fun MentionLabel(userName: String) {
//    Label(label = userName)/
}

@Preview
@Composable
fun PreviewMessage() {
    Message(
        user = User("", AvailabilityStatus.Available, Membership.External, true),
        MessageContent("This is some test message"),
        "12:30 PM"
    )
}


data class Message(val user: User, val messageContent: MessageContent, val time: String)

data class MessageContent(
    val message: String
) {
    fun format() {

    }
}


data class User(
    val avatarUrl: String = "",
    val availabilityStatus: AvailabilityStatus,
    val membership: Membership,
    val isLegalHold: Boolean,
)





