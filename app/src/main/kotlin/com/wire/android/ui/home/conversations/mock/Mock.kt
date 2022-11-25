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
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

val mockFooter = MessageFooter("", mapOf("👍" to 1), setOf("👍"))

val mockMessageWithText = UIMessage(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus.Untouched,
        messageId = "",
        connectionState = ConnectionState.ACCEPTED,
        isDeleted = false
    ),
    messageContent = UIMessageContent.TextMessage(
        MessageBody(UIText.DynamicString(
            "This is some test message that is very very" +
                    "very very very very" +
                    " very very very" +
                    "very very very very very long"
        ))
    ),
    messageSource = MessageSource.Self,
    messageFooter = mockFooter
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


fun mockAssetMessage(uploadStatus: Message.UploadStatus = Message.UploadStatus.UPLOADED) = UIMessage(
    userAvatarData = UserAvatarData(
        UserAvatarAsset(mockImageLoader, UserAssetId("a", "domain")),
        UserAvailabilityStatus.AVAILABLE
    ),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus.Untouched,
        messageId = "",
        connectionState = ConnectionState.ACCEPTED,
        isDeleted = false
    ),
    messageContent = UIMessageContent.AssetMessage(
        assetName = "This is some test asset message",
        assetExtension = "ZIP",
        assetId = UserAssetId("asset", "domain"),
        assetSizeInBytes = 21957335,
        uploadStatus = uploadStatus,
        downloadStatus = Message.DownloadStatus.NOT_DOWNLOADED
    ),
    messageFooter = mockFooter,
    messageSource = MessageSource.Self
)

@Suppress("MagicNumber")
fun mockedImg(
    uploadStatus: Message.UploadStatus = Message.UploadStatus.UPLOADED,
    downloadStatus: Message.DownloadStatus = Message.DownloadStatus.SAVED_INTERNALLY
) = UIMessageContent.ImageMessage(
    UserAssetId("a", "domain"), null, 0, 0, uploadStatus = uploadStatus, downloadStatus = downloadStatus
)

@Suppress("MagicNumber")
fun mockedImageUIMessage(uploadStatus: Message.UploadStatus = Message.UploadStatus.UPLOADED) = UIMessage(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    messageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.External,
        isLegalHold = false,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
        messageId = "4",
        connectionState = ConnectionState.ACCEPTED,
        isDeleted = false
    ),
    messageContent = mockedImg(uploadStatus),
    messageFooter = mockFooter,
    messageSource = MessageSource.Self
)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<UIMessage> = listOf(
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Untouched,
            messageId = "1",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = UIMessageContent.TextMessage(
            MessageBody(UIText.DynamicString(
                "This is some test message that is very very" +
                        "very very very very" +
                        " very very very" +
                        "very very very very very long"
            ))
        ),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter,
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Deleted,
            messageId = "2",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = mockedImg(),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter,
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "3",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = mockedImg(),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter,
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "4",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = mockedImg(),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter,
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Deleted,
            messageId = "5",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = UIMessageContent.TextMessage(
            MessageBody(UIText.DynamicString(
                "This is some test message that is very very" +
                        "very very very very" +
                        " very very very" +
                        "very very very very very long"
            ))
        ),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "6",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = mockedImg(),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter,
    ),
    UIMessage(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        messageHeader = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus.Edited("May 31, 2022 12.24pm"),
            messageId = "7",
            connectionState = ConnectionState.ACCEPTED,
            isDeleted = false
        ),
        messageContent = UIMessageContent.TextMessage(
            MessageBody(UIText.DynamicString(
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
            ))
        ),
        messageSource = MessageSource.Self,
        messageFooter = mockFooter
    )
)

