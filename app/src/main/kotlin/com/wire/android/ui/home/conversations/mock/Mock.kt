package com.wire.android.ui.home.conversations.mock

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message

val mockMessageWithText = UIMessage(
    user = User(null, UserStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = ""
    ),
    messageContent = MessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                "This is some test message that is very very" +
                        "very very very very" +
                        " very very very" +
                        "very very very very very long"
            )
        )
    ),
    messageSource = MessageSource.Self
)

val mockAssetMessage = UIMessage(
    user = User(UserAvatarAsset(""), UserStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = ""
    ),
    messageContent = MessageContent.AssetMessage(
        assetName = "This is some test asset message",
        assetExtension = "ZIP",
        assetId = "asset-id",
        assetSizeInBytes = 21957335,
        downloadStatus = Message.DownloadStatus.NOT_DOWNLOADED
    ),
    messageSource = MessageSource.Self
)

@Suppress("MagicNumber")
val mockedImg = MessageContent.ImageMessage("asset-id", ByteArray(16), 0, 0)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<UIMessage> = listOf(
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Untouched,
            messageId = ""
        ),
        messageContent = MessageContent.TextMessage(
            messageBody = MessageBody(
                UIText.DynamicString(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            )
        ),
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = ""
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = ""
        ),
        messageContent = MessageContent.TextMessage(
            messageBody = MessageBody(
                UIText.DynamicString(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            )
        ),
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        user = User(null, UserStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited,
            messageId = ""
        ),
        messageContent = MessageContent.TextMessage(
            messageBody = MessageBody(
                UIText.DynamicString(
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
            )
        ),
        messageSource = MessageSource.Self
    )
)
