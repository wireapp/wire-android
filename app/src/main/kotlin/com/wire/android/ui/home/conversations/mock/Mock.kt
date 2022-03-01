package com.wire.android.ui.home.conversations.mock

import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership

val mockMessageWithText = Message(
    user = User("", UserStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = "John Doe",
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
)

val mockMessageWithImage = Message(
    user = User("", UserStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = "John Doe",
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Deleted
    ),
    messageContent = MessageContent.ImageMessage("someUrl")
)

val mockMessages = listOf(
    Message(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
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
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted
        ),
        messageContent = MessageContent.ImageMessage("someUrl"),
    ),
    Message(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited
        ),
        messageContent = MessageContent.ImageMessage("someUrl"),
    ),
    Message(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited
        ),
        messageContent = MessageContent.ImageMessage("someUrl"),
    ),
    Message(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
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
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited
        ),
        messageContent = MessageContent.ImageMessage("someUrl"),
    ),
    Message(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
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
    )
)
