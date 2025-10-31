/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.messages.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.RegularMessageItem
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
import com.wire.android.ui.home.conversations.mock.mockedVideo
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.util.ui.PreviewMultipleScreens
import com.wire.android.util.ui.WireScrollableTheme
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.QualifiedID

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesPhotoLandscape() {
    WireScrollableTheme {
        Column(Modifier.background(colorsScheme().surface)) {
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.Self,
                    content = mockedVideo(
                        width = 1920,
                        height = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.OtherUser,
                    content = mockedVideo(
                        width = 1920,
                        height = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesPhotoPortrait() {
    WireScrollableTheme {
        Column(Modifier.background(colorsScheme().surface)) {
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.Self,
                    content = mockedVideo(
                        height = 1920,
                        width = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.OtherUser,
                    content = mockedVideo(
                        height = 1920,
                        width = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesSquarePhoto() {
    WireScrollableTheme {
        Column(Modifier.background(colorsScheme().surface)) {
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.Self,
                    content = mockedVideo(
                        height = 1080,
                        width = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.OtherUser,
                    content = mockedVideo(
                        height = 1080,
                        width = 1080
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesMediumLandscape() {
    WireScrollableTheme {
        Column(Modifier.background(colorsScheme().surface)) {
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.Self,
                    content = mockedVideo(
                        height = 1200,
                        width = 1600
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.OtherUser,
                    content = mockedVideo(
                        height = 1200,
                        width = 1600
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesUltraWideBanner() {
    WireScrollableTheme {
        Column(Modifier.background(colorsScheme().surface)) {
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.Self,
                    content = mockedVideo(
                        height = 480,
                        width = 1900
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockedImageUIMessage(
                    "assetMessageId",
                    source = MessageSource.OtherUser,
                    content = mockedVideo(
                        height = 480,
                        width = 1900
                    )
                ),
                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleScreens
@Composable
fun PreviewVideoMessagesUltraTallScreenshot() {
    WireScrollableTheme {
        RegularMessageItem(
            message = mockedImageUIMessage(
                "assetMessageId",
                source = MessageSource.Self,
                content = mockedVideo(
                    height = 2400,
                    width = 600,
                    assetName = "very looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong name.mp4"
                )
            ),
            conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
            assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
            clickActions = MessageClickActions.Content(),
            isBubbleUiEnabled = true,
        )
        RegularMessageItem(
            message = mockedImageUIMessage(
                "assetMessageId",
                source = MessageSource.OtherUser,
                content = mockedVideo(
                    height = 2400,
                    width = 600
                )
            ),
            conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
            assetStatus = AssetTransferStatus.SAVED_INTERNALLY,
            clickActions = MessageClickActions.Content(),
            isBubbleUiEnabled = true
        )
    }
}
