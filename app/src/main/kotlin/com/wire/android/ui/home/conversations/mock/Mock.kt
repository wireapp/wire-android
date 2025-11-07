@file:Suppress("TooManyFunctions", "MagicNumber")
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
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.VisualMediaParams
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath

private const val MOCK_TIME_IN_SECONDS: Long = 1729837498
val mockFooter = MessageFooter("", mapOf("👍" to 1), setOf("👍"))

val mockFooterWithMultipleReactions = MessageFooter(
    messageId = "messageId",
    reactions = mapOf(
        "👍" to 1,
        "👎" to 2,
        "👏" to 3,
        "🤔" to 4,
        "🤷" to 5,
        "🤦" to 6,
        "🤢" to 7
    ),
    ownReactions = setOf("👍"),
)
val mockEmptyFooter = MessageFooter("", emptyMap(), emptySet())
val mockMessageTime = MessageTime(Instant.fromEpochSeconds(MOCK_TIME_IN_SECONDS))

val mockHeader = MessageHeader(
    username = UIText.DynamicString("John Doe"),
    membership = Membership.Guest,
    showLegalHoldIndicator = true,
    messageTime = mockMessageTime,
    messageStatus = MessageStatus(
        flowStatus = MessageFlowStatus.Sent,
        expirationStatus = ExpirationStatus.NotExpirable
    ),
    messageId = "",
    connectionState = ConnectionState.ACCEPTED,
    isSenderDeleted = false,
    isSenderUnavailable = false
)

fun mockHeaderWithExpiration(expirable: ExpirationStatus.Expirable, isDeleted: Boolean = false) = mockHeader.copy(
    messageStatus = MessageStatus(
        flowStatus = MessageFlowStatus.Delivered,
        expirationStatus = expirable,
        isDeleted = isDeleted
    )
)

val mockMessageWithText = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
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

fun mockMessageWithTextContent(text: String) = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = mockHeader,
    messageContent = UIMessageContent.TextMessage(
        messageBody = MessageBody(
            UIText.DynamicString(
                text
            )
        )
    ),
    source = MessageSource.Self,
    messageFooter = mockEmptyFooter
)

val mockMessageWithTextLoremIpsum = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
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
    conversationId = ConversationId("value", "domain"),
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
    conversationId = ConversationId("value", "domain"),
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
    conversationId = ConversationId("value", "domain"),
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
    conversationId = ConversationId("value", "domain"),
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

fun mockAssetMessage(assetId: String = "asset1", messageId: String = "msg1") = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
    userAvatarData = UserAvatarData(
        UserAvatarAsset(UserAssetId("a", "domain")),
        UserAvailabilityStatus.AVAILABLE
    ),
    header = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        showLegalHoldIndicator = true,
        messageTime = mockMessageTime,
        messageStatus = MessageStatus(
            flowStatus = MessageFlowStatus.Sent,
            expirationStatus = ExpirationStatus.NotExpirable
        ),
        messageId = messageId,
        connectionState = ConnectionState.ACCEPTED,
        isSenderDeleted = false,
        isSenderUnavailable = false
    ),
    messageContent = UIMessageContent.AssetMessage(
        assetName = "This is some test asset message that has a not so long title",
        assetExtension = "ZIP",
        assetId = UserAssetId(assetId, "domain"),
        assetSizeInBytes = 21957335,
        assetDataPath = null,
    ),
    messageFooter = mockEmptyFooter,
    source = MessageSource.Self
)

fun mockAssetAudioMessage(assetId: String = "asset1", messageId: String = "msg1") = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
    userAvatarData = UserAvatarData(
        UserAvatarAsset(UserAssetId("a", "domain")),
        UserAvailabilityStatus.AVAILABLE
    ),
    header = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.Guest,
        showLegalHoldIndicator = true,
        messageTime = mockMessageTime,
        messageStatus = MessageStatus(
            flowStatus = MessageFlowStatus.Sent,
            expirationStatus = ExpirationStatus.NotExpirable
        ),
        messageId = messageId,
        connectionState = ConnectionState.ACCEPTED,
        isSenderDeleted = false,
        isSenderUnavailable = false
    ),
    messageContent = UIMessageContent.AudioAssetMessage(
        assetName = "Audio message",
        assetExtension = "WAV",
        assetId = UserAssetId(assetId, "domain"),
        audioMessageDurationInMs = 60_000,
        sizeInBytes = 10_000,
    ),
    messageFooter = mockEmptyFooter,
    source = MessageSource.Self
)

fun mockUIAssetMessage(assetId: String = "asset1", messageId: String = "msg1") = UIAssetMessage(
    assetId = assetId,
    time = Instant.DISTANT_PAST,
    username = UIText.DynamicString("Username 1"),
    messageId = messageId,
    conversationId = QualifiedID("value", "domain"),
    assetPath = "path".toPath(),
    isSelfAsset = false
)

@Suppress("MagicNumber")
fun mockedImg(width: Int = 800, height: Int = 600) = UIMessageContent.ImageMessage(
    assetId = UserAssetId("a", "domain"),
    asset = mockedPrivateAsset(),
    params = VisualMediaParams(width, height)
)

@Suppress("MagicNumber")
fun mockedVideo(width: Int = 800, height: Int = 600, assetName: String = "video.mp4") = UIMessageContent.VideoMessage(
    assetId = UserAssetId("a", "domain"),
    assetSizeInBytes = 123456,
    assetName = assetName,
    assetExtension = "mp4",
    assetDataPath = null,
    params = VisualMediaParams(width, height),
    duration = 12412412,
)

fun mockedPrivateAsset() = ImageAsset.PrivateAsset(
    conversationId = ConversationId("id", "domain"),
    messageId = "messageId",
    isSelfAsset = true
)

@Suppress("MagicNumber")
fun mockedImageUIMessage(
    messageId: String = "messageId",
    messageStatus: MessageStatus = MessageStatus(
        flowStatus = MessageFlowStatus.Sent,
        expirationStatus = ExpirationStatus.NotExpirable
    ),
    content: UIMessageContent.Regular = mockedImg(),
    header: MessageHeader = MessageHeader(
        username = UIText.DynamicString("John Doe"),
        membership = Membership.External,
        showLegalHoldIndicator = false,
        messageTime = mockMessageTime,
        messageStatus = messageStatus,
        messageId = messageId,
        connectionState = ConnectionState.ACCEPTED,
        isSenderDeleted = false,
        isSenderUnavailable = false
    ),
    source: MessageSource = MessageSource.Self
) = UIMessage.Regular(
    conversationId = ConversationId("value", "domain"),
    userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
    header = header,
    messageContent = content,
    messageFooter = mockEmptyFooter,
    source = source
)

@Suppress("LongMethod", "MagicNumber")
fun getMockedMessages(): List<UIMessage> = listOf(
    UIMessage.Regular(
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            showLegalHoldIndicator = true,
            messageTime = mockMessageTime,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.Guest,
            showLegalHoldIndicator = true,
            messageTime = mockMessageTime,
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Delivered,
                isDeleted = true,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            showLegalHoldIndicator = false,
            messageTime = mockMessageTime,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            showLegalHoldIndicator = false,
            messageTime = mockMessageTime,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            showLegalHoldIndicator = false,
            messageTime = mockMessageTime,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            showLegalHoldIndicator = false,
            messageTime = mockMessageTime,
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
        conversationId = ConversationId("value", "domain"),
        userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
        header = MessageHeader(
            username = UIText.DynamicString("John Doe"),
            membership = Membership.External,
            showLegalHoldIndicator = false,
            messageTime = mockMessageTime,
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
