/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations.mock

import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import com.wire.android.model.ImageAsset
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageEditStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val mockFooter = MessageFooter("", mapOf("👍" to 1), setOf("👍"))
val mockEmptyFooter = MessageFooter("", emptyMap(), emptySet())

val mockHeader = MessageHeader(
    username = UIText.DynamicString("John Doe"),
    membership = Membership.Guest,
    isLegalHold = true,
    messageTime = MessageTime("12.23pm"),
    messageStatus = MessageStatus(
        flowStatus = MessageFlowStatus.Sent,
        expirationStatus = ExpirationStatus.NotExpirable
    ),
    messageId = "",
    connectionState = ConnectionState.ACCEPTED,
    isSenderDeleted = false,
    isSenderUnavailable = false
)

val mockMessageWithText = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                "This is some test message that is very very" +
                        "very very very very" +
                        " very very very" +
                        "very very very very very long"
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithTextLoremIpsum = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus volutpat lorem tortor, " +
                        "nec porttitor sapien pulvinar eu. Nullam orci dolor, eleifend quis massa non, posuere bibendum risus. " +
                        "Praesent velit ipsum, hendrerit et ante in, placerat pretium nunc. Sed orci velit, venenatis non vulputate non, " +
                        "venenatis sit amet enim. Quisque vestibulum, ligula in interdum rhoncus, magna ante porta velit, " +
                        "ut dignissim augue est et leo. Vestibulum in nunc eu velit elementum porttitor vitae eu nunc. " +
                        "Aliquam consectetur orci sit amet turpis consectetur, ut tempus velit pulvinar. Pellentesque et lorem placerat, " +
                        "aliquet odio non, consequat metus. Maecenas ultricies mauris quis lorem cursus dignissim. " +
                        "Nullam lacinia, nisl et dapibus consequat, sapien dolor maximus erat, quis aliquet dolor elit tincidunt orci."
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithMarkdownTextAndLinks = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                """ 
**bold text**

_italic text_

**_bold and italic_**

~~Strikethrough~~

# header

# Code

Inline `code`

Indented code

// Some comments
line 1 of code
line 2 of code
line 3 of code


Block code "fences"

```
Sample text here...
```

# Links
[AR PR](https://github.com/wireapp/wire-android/pulls)

Autoconverted link https://github.com/wireapp/kalium/pulls
"""
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithMarkdownListAndImages = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                """
## Lists

Bullet List

+ Create a list by starting a line with `+`, `-`, or `*`
+ Sub-lists are made by indenting 2 spaces:
- Marker character change forces new list start:
* Ac tristique libero volutpat at
+ Facilisis in pretium nisl aliquet
- Nulla volutpat aliquam velit
+ Very easy!

Ordered

1. Lorem ipsum dolor sit amet
2. Consectetur adipiscing elit
3. Integer molestie lorem at massa


1. You can use sequential numbers...
1. ...or keep all the numbers as `1.`

Start numbering with offset:

57. foo
1. bar

# Images

Webp

![Wire](https://wire.com/wp-content/uploads/2022/02/Independently-Audited-_-Open-Source-2.webp)

Svg

![Wire](https://wire.com/wp-content/uploads/2021/08/wire-logo.svg)

Png

![Wire](https://avatars.githubusercontent.com/u/16047324?s=280&v=4)
"""
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithMarkdownTablesAndBlocks = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                """
# Tables
| Task | Person |
| ------ | ----------- |
| MLS   | John |
| Federation | Will |
| Navigation | Ashley |


## Thematic Break

___

---

***


# Blockquotes


> Blockquotes can also be nested...
>> ...by using additional greater-than signs right next to each other...
> > > ...or with spaces between arrows.


# Typographic replacements

Enable typographer option to see result.

(c) (C) (r) (R) (tm) (TM) (p) (P) +-"""
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithKnock = UIMessage.System(
    header = mockHeader,
    messageContent = UIMessageContent.SystemMessage.Knock(UIText.DynamicString("John Doe pinged"), true),
    source = MessageSource.Self,
)

val mockUsersUITexts = listOf(
    "Albert Lewis".toUIText(),
    "Bert Strunk".toUIText(),
    "Claudia Schiffer".toUIText(),
    "Dorothee Friedrich".toUIText(),
    "Erich Weinert".toUIText(),
    "Frieda Kahlo".toUIText(),
    "Gudrun Gut".toUIText()
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
},
    object : NetworkStateObserver {
        override fun observeNetworkState(): StateFlow<NetworkState> = MutableStateFlow(NetworkState.ConnectedWithInternet)
    }
)

fun mockAssetMessage() = UIMessage.Regular(
    userAvatarData = UserAvatarData(
        UserAvatarAsset(mockImageLoader, UserAssetId("a", "domain")),
        UserAvailabilityStatus.AVAILABLE
    ),
    header = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        isLegalHold = true,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus(
            flowStatus = MessageFlowStatus.Sent,
            expirationStatus = ExpirationStatus.NotExpirable
        ),
        messageId = "",
        connectionState = ConnectionState.ACCEPTED,
        isSenderDeleted = false,
        isSenderUnavailable = false
    ),
    messageContent = UIMessageContent.AssetMessage(
        assetName = "This is some test asset message that has a not so long title",
        assetExtension = "ZIP",
        assetId = UserAssetId("asset", "domain"),
        assetSizeInBytes = 21957335
    ),
    messageFooter = mockEmptyFooter,
    source = MessageSource.Self
)

@Suppress("MagicNumber")
fun mockedImg() = UIMessageContent.ImageMessage(
    UserAssetId("a", "domain"),
    ImageAsset.PrivateAsset(mockImageLoader, ConversationId("id", "domain"), "messageId", true),
    800, 600
)

@Suppress("MagicNumber")
fun mockedImageUIMessage(
    messageId: String = "messageId",
    messageStatus: MessageStatus = MessageStatus(
        flowStatus = MessageFlowStatus.Sent,
        expirationStatus = ExpirationStatus.NotExpirable
    )
) = UIMessage.Regular(
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.External,
        isLegalHold = false,
        messageTime = MessageTime("12.23pm"),
        messageStatus = messageStatus,
        messageId = messageId,
        connectionState = ConnectionState.ACCEPTED,
        isSenderDeleted = false,
        isSenderUnavailable = false
    ),
    messageContent = mockedImg(),
    messageFooter = mockEmptyFooter,
    source = MessageSource.Self
)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<UIMessage> = listOf(
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "1",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = UIMessageContent.TextMessage(
            messageBody = MessageBody(
                UIText.DynamicString(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            )
        ),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            isLegalHold = true,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Delivered, isDeleted = true,
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "2",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = mockedImg(),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                editStatus = MessageEditStatus.Edited("May 31, 2022 12.24pm"),
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "3",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = mockedImg(),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                editStatus = MessageEditStatus.Edited("May 31, 2022 12.24pm"),
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "4",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = mockedImg(),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Delivered,
                isDeleted = true,
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "5",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = UIMessageContent.TextMessage(
            messageBody = MessageBody(
                UIText.DynamicString(
                    "This is some test message that is very very" +
                            "very very very very" +
                            " very very very" +
                            "very very very very very long"
                )
            )
        ),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                editStatus = MessageEditStatus.Edited("May 31, 2022 12.24pm"),
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "6",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = mockedImg(),
        source = MessageSource.Self,
        messageFooter = mockFooter
    ),
    UIMessage.Regular(
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            isLegalHold = false,
            messageTime = MessageTime("12.23pm"),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                editStatus = MessageEditStatus.Edited("May 31, 2022 12.24pm"),
                expirationStatus = ExpirationStatus.NotExpirable
            ),
            messageId = "7",
            connectionState = ConnectionState.ACCEPTED,
            isSenderDeleted = false,
            isSenderUnavailable = false
        ),
        messageContent = UIMessageContent.TextMessage(
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
        source = MessageSource.Self,
        messageFooter = mockFooter
    )
)
