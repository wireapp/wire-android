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

package com.wire.android.framework

import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageEncryptionAlgorithm
import com.wire.kalium.logic.data.message.MessagePreview
import com.wire.kalium.logic.data.message.MessagePreviewContent
import com.wire.kalium.logic.data.user.UserId

object TestMessage {

    val FAILED_DECRYPTION = Message.Regular(
        id = "messageID",
        content = MessageContent.FailedDecryption(
            null,
            senderUserId = UserId("user-id", "domain"),
            isDecryptionResolved = false
        ),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        isSelfMessage = false
    )

    val TEXT_MESSAGE = Message.Regular(
        id = "messageID",
        content = MessageContent.Text("Some Text Message"),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        isSelfMessage = false
    )
    val DUMMY_ASSET_REMOTE_DATA = AssetContent.RemoteData(
        otrKey = ByteArray(0),
        sha256 = ByteArray(16),
        assetId = "asset-id",
        assetToken = "==some-asset-token",
        assetDomain = "some-asset-domain.com",
        encryptionAlgorithm = MessageEncryptionAlgorithm.AES_GCM
    )
    val ASSET_IMAGE_CONTENT = AssetContent(
        0L,
        "name",
        "image/jpg",
        AssetContent.AssetMetadata.Image(100, 100),
        DUMMY_ASSET_REMOTE_DATA,
        Message.UploadStatus.NOT_UPLOADED,
        Message.DownloadStatus.NOT_DOWNLOADED
    )
    val GENERIC_ASSET_CONTENT = AssetContent(
        0L,
        "name",
        "application/zip",
        null,
        DUMMY_ASSET_REMOTE_DATA,
        Message.UploadStatus.NOT_UPLOADED,
        Message.DownloadStatus.NOT_DOWNLOADED
    )
    val ASSET_MESSAGE = Message.Regular(
        id = "messageID",
        content = MessageContent.Asset(ASSET_IMAGE_CONTENT),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        isSelfMessage = false
    )
    val UNKNOWN_MESSAGE = Message.Regular(
        id = "messageID",
        content = MessageContent.Unknown("some-unhandled-message"),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        isSelfMessage = false
    )

    fun buildAssetMessage(assetContent: AssetContent) = Message.Regular(
        id = "messageID",
        content = MessageContent.Asset(assetContent),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.Sent,
        editStatus = Message.EditStatus.NotEdited,
        isSelfMessage = false
    )

    val SYSTEM_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.MissedCall,
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.Sent,
        expirationData = null
    )

    val MEMBER_REMOVED_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.MemberChange.Removed(listOf(UserId("user-id", "domain"))),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.Sent,
        expirationData = null
    )
    val UI_MESSAGE_HEADER = MessageHeader(
        username = UIText.DynamicString("username"),
        membership = Membership.Guest,
        isLegalHold = true,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus(
            flowStatus = MessageFlowStatus.Sent,
            expirationStatus = ExpirationStatus.NotExpirable
        ),
        messageId = "messageID",
        connectionState = null,
        isSenderDeleted = false,
        isSenderUnavailable = false
    )
    val MISSED_CALL_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.MissedCall,
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.Sent,
        expirationData = null
    )

    val CONVERSATION_CREATED_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.ConversationCreated,
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.Sent,
        expirationData = null
    )

    val PREVIEW = MessagePreview(
        id = "messageId",
        conversationId = ConversationId("value", "domain"),
        content = MessagePreviewContent.WithUser.MissedCall(TestUser.OTHER_USER.name),
        isSelfMessage = false,
        date = "2022-03-30T15:36:00.000Z",
        visibility = Message.Visibility.VISIBLE,
        senderUserId = TestUser.USER_ID
    )
}
