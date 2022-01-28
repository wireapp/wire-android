package com.wire.android.ui.home.message.mock

import com.wire.android.ui.home.conversations.all.model.AvailabilityStatus
import com.wire.android.ui.home.conversations.all.model.Membership
import com.wire.android.ui.main.message.model.Message
import com.wire.android.ui.main.message.model.MessageBody
import com.wire.android.ui.main.message.model.MessageContent
import com.wire.android.ui.main.message.model.MessageHeader
import com.wire.android.ui.main.message.model.MessageStatus
import com.wire.android.ui.main.message.model.User

val mockMessageWithText = Message(
    user = User("", AvailabilityStatus.Available),
    messageHeader = MessageHeader(
        username = "Mateusz Pachulski",
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
    user = User("", AvailabilityStatus.Available),
    messageHeader = MessageHeader(
        username = "Mateusz Pachulski",
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Deleted
    ),
    messageContent = MessageContent.ImageMessage("someUrl")
)

val mockMessages = listOf(
    Message(
        user = User("", AvailabilityStatus.Available),
        messageHeader = MessageHeader(
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
            username = "Mateusz Pachulski",
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
