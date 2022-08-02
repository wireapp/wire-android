package com.wire.android.ui.home.conversations.mock

import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

val mockMessageWithText = UIMessage(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = "",
        connectionState = ConnectionState.ACCEPTED
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

val mockImageLoader = WireSessionImageLoader(object : ImageLoader {
    override val components: ComponentRegistry get() = TODO("Not yet implemented")
    override val defaults: DefaultRequestOptions get() = TODO("Not yet implemented")
    override val diskCache: DiskCache get() = TODO("Not yet implemented")
    override val memoryCache: MemoryCache get() = TODO("Not yet implemented")
    override fun enqueue(request: ImageRequest): Disposable = TODO("Not yet implemented")
    override suspend fun execute(request: ImageRequest): ImageResult = TODO("Not yet implemented")
    override fun newBuilder(): ImageLoader.Builder = TODO("Not yet implemented")
    override fun shutdown() = TODO("Not yet implemented")
})

val mockAssetMessage = UIMessage(
    userAvatarData = UserAvatarData(
        UserAvatarAsset(mockImageLoader, UserAssetId("a", "domain")),
        UserAvailabilityStatus.AVAILABLE
    ),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = "",
        connectionState = ConnectionState.ACCEPTED
    ),
    messageContent = MessageContent.AssetMessage(
        assetName = "This is some test asset message",
        assetExtension = "ZIP",
        assetId = UserAssetId("asset", "domain"),
        assetSizeInBytes = 21957335,
        downloadStatus = Message.DownloadStatus.NOT_DOWNLOADED
    ),
    messageSource = MessageSource.Self
)

@Suppress("MagicNumber")
val mockedImg = MessageContent.ImageMessage(UserAssetId("a", "domain"), ByteArray(16), 0, 0)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<UIMessage> = listOf(
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Untouched,
            messageId = "1",
            connectionState = ConnectionState.ACCEPTED
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
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = "2",
            connectionState = ConnectionState.ACCEPTED
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "3",
            connectionState = ConnectionState.ACCEPTED
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "4",
            connectionState = ConnectionState.ACCEPTED
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Deleted,
            messageId = "5",
            connectionState = ConnectionState.ACCEPTED
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
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "6",
            connectionState = ConnectionState.ACCEPTED
        ),
        messageContent = mockedImg,
        messageSource = MessageSource.Self
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            time = "12.23pm",
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "7",
            connectionState = ConnectionState.ACCEPTED
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
