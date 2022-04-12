package com.wire.android.ui.home.conversations.mock

import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership

val mockMessageWithText = MessageViewWrapper(
    user = User("", UserStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = "John Doe",
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = ""
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

@Suppress("MagicNumber")
val mockedImg = MessageContent.ImageMessage(ByteArray(16), 0, 0)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<MessageViewWrapper> = listOf(
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Untouched,
            messageId = ""
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
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = ""
        ),
        messageContent = mockedImg,
    ),
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
    ),
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
    ),
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = ""
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
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
    ),
    MessageViewWrapper(
        user = User("", UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = "John Doe",
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
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
