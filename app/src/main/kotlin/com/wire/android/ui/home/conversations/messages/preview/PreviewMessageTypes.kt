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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.messages.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.RegularMessageItem
import com.wire.android.ui.home.conversations.messages.item.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetAudioMessage
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockFooter
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.mock.mockMessageWithKnock
import com.wire.android.ui.home.conversations.mock.mockMessageWithMarkdownListAndImages
import com.wire.android.ui.home.conversations.mock.mockMessageWithMarkdownTablesAndBlocks
import com.wire.android.ui.home.conversations.mock.mockMessageWithMarkdownTextAndLinks
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockMessageWithTextLoremIpsum
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.user.UserId

private val previewUserId = UserId("value", "domain")

@PreviewMultipleThemes
@Composable
fun PreviewMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.copy(
                header = mockMessageWithText.header.copy(
                    username = UIText.DynamicString(
                        "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                                "Ruiz y Picasso"
                    )
                )
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithReactions() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.copy(
                header = mockMessageWithText.header.copy(
                    username = UIText.DynamicString(
                        "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                                "Ruiz y Picasso"
                    )
                ),
                messageFooter = mockFooter
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithReply() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.copy(
                header = mockMessageWithText.header.copy(
                    username = UIText.DynamicString(
                        "Don Joe"
                    )
                ),
                messageContent = UIMessageContent.TextMessage(
                    MessageBody(
                        message = UIText.DynamicString("Sure, go ahead!"),
                        quotedMessage = UIQuotedMessage.UIQuotedData(
                            messageId = "asdoij",
                            senderId = previewUserId,
                            senderName = UIText.DynamicString("John Doe"),
                            originalMessageDateDescription = UIText.StringResource(R.string.label_quote_original_message_date, "10:30"),
                            editedTimeDescription = UIText.StringResource(R.string.label_message_status_edited_with_date, "10:32"),
                            quotedContent = UIQuotedMessage.UIQuotedData.Text(UIText.DynamicString("Hey, can I call right now?")),
                            senderAccent = Accent.Blue
                        )
                    )
                )
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewDeletedMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.let {
                it.copy(
                    header = it.header.copy(
                        messageStatus = MessageStatus(
                            flowStatus = MessageFlowStatus.Delivered,
                            isDeleted = true,
                            expirationStatus = ExpirationStatus.NotExpirable
                        )
                    )
                )
            },
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFailedSendMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.let {
                it.copy(
                    header = it.header.copy(
                        messageStatus = MessageStatus(
                            flowStatus = MessageFlowStatus.Failure.Send.Locally(false),
                            expirationStatus = ExpirationStatus.NotExpirable
                        )
                    ),
                    messageFooter = mockFooter.copy(reactions = emptyMap(), ownReactions = emptySet())
                )
            },
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFailedDecryptionMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.let {
                it.copy(
                    header = it.header.copy(
                        messageStatus = MessageStatus(
                            flowStatus = MessageFlowStatus.Failure.Decryption(false, 222),
                            expirationStatus = ExpirationStatus.NotExpirable
                        )
                    ),
                    messageFooter = mockFooter.copy(reactions = emptyMap(), ownReactions = emptySet())
                )
            },
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAssetMessageWithReactions() {
    WireTheme {
        RegularMessageItem(
            message = mockAssetMessage().copy(messageFooter = mockFooter),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportedMediaAssetMessageContent() {
    WireTheme {
        MessageAsset(
            assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
            assetExtension = "rar.tgz",
            assetSizeInBytes = 99201224L,
            onAssetClick = Clickable(enabled = false),
            assetTransferStatus = AssetTransferStatus.NOT_DOWNLOADED,
            assetDataPath = null,
            messageStyle = MessageStyle.NORMAL
            )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLoadingAssetMessage() {
    WireTheme {
        MessageAsset(
            assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
            assetExtension = "rar.tgz",
            messageStyle = MessageStyle.NORMAL,
            assetSizeInBytes = 99201224L,
            onAssetClick = Clickable(enabled = false),
            assetTransferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
                    assetDataPath = null,
            )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFailedDownloadAssetMessage() {
    WireTheme {
        MessageAsset(
            assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
            assetExtension = "rar.tgz",
            assetSizeInBytes = 99201224L,
            onAssetClick = Clickable(enabled = false),
            assetTransferStatus = AssetTransferStatus.FAILED_DOWNLOAD,
            assetDataPath = null,
            messageStyle = MessageStyle.NORMAL
            )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImageMessageUploaded() {
    WireTheme {
        RegularMessageItem(
            message = mockedImageUIMessage(messageId = "assetMessageId"),
            conversationDetailsData = ConversationDetailsData.None(null),
            assetStatus = AssetTransferStatus.UPLOADED,
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImageMessageUploading() {
    WireTheme {
        RegularMessageItem(
            message = mockedImageUIMessage("assetMessageId"),
            conversationDetailsData = ConversationDetailsData.None(null),
            assetStatus = AssetTransferStatus.UPLOAD_IN_PROGRESS,
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImageMessageFailedUpload() {
    WireTheme {
        RegularMessageItem(
            message = mockedImageUIMessage(
                messageId = "assetMessageId",
                messageStatus = MessageStatus(
                    flowStatus = MessageFlowStatus.Failure.Send.Locally(false),
                    expirationStatus = ExpirationStatus.NotExpirable
                )
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            assetStatus = AssetTransferStatus.FAILED_UPLOAD,
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAudioMessage() = WireTheme {
    RegularMessageItem(
        message = mockAssetAudioMessage(),
        conversationDetailsData = ConversationDetailsData.None(null),
        assetStatus = AssetTransferStatus.UPLOADED,
        clickActions = MessageClickActions.Content(),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithSystemMessage() {
    WireTheme {
        Column {
            RegularMessageItem(
                message = mockMessageWithText,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            SystemMessageItem(
                mockMessageWithKnock.copy(
                    messageContent = UIMessageContent.SystemMessage.MissedCall.YouCalled(
                        UIText.DynamicString("ME")
                    )
                )
            )
            SystemMessageItem(
                mockMessageWithKnock.copy(
                    messageContent = UIMessageContent.SystemMessage.MemberAdded(
                        UIText.DynamicString("ME"),
                        listOf(UIText.DynamicString("John Smith"))
                    )
                )
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessagesWithUnavailableQuotedMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.copy(
                messageContent = UIMessageContent.TextMessage(
                    MessageBody(
                        message = UIText.DynamicString("Confirmed"),
                        quotedMessage = UIQuotedMessage.UnavailableData
                    )
                )
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAggregatedMessagesWithErrorMessage() {
    WireTheme {
        Column {
            RegularMessageItem(
                message = mockMessageWithText,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithText.copy(
                    messageContent = UIMessageContent.TextMessage(
                        MessageBody(
                            message = UIText.DynamicString("Confirmed"),
                            quotedMessage = UIQuotedMessage.UnavailableData
                        )
                    )
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                showAuthor = false,
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithText.copy(
                    header = mockHeader.copy(
                        messageStatus = MessageStatus(
                            flowStatus = MessageFlowStatus.Failure.Send.Locally(false),
                            expirationStatus = ExpirationStatus.NotExpirable
                        )
                    )
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                showAuthor = true,
                clickActions = MessageClickActions.Content(),
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithMarkdownTextAndLinks() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithMarkdownTextAndLinks,
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithMarkdownListAndImages() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithMarkdownListAndImages,
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithMarkdownTablesAndBlocks() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithMarkdownTablesAndBlocks,
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithMarkdownQuery() {
    WireTheme {
        Column {
            RegularMessageItem(
                message = mockMessageWithTextLoremIpsum,
                searchQuery = "ed",
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithMarkdownTextAndLinks,
                searchQuery = "code",
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithMarkdownTextAndLinks,
                searchQuery = ".com",
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithMarkdownListAndImages,
                searchQuery = "can",
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
            RegularMessageItem(
                message = mockMessageWithMarkdownTablesAndBlocks,
                searchQuery = "Joh",
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageWithAccents() = WireTheme {
    Column {
        Accent.entries.forEach {
            RegularMessageItem(
                message = mockMessageWithText.copy(
                    header = mockHeader.copy(username = UIText.DynamicString(it.name), accent = it),
                    messageContent = UIMessageContent.TextMessage(MessageBody(UIText.DynamicString("Text")))
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
            )
        }
    }
}
