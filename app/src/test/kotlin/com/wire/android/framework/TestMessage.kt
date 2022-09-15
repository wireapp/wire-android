package com.wire.android.framework

import com.wire.android.mapper.AssetMessageData
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageEncryptionAlgorithm
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

object TestMessage {
    val TEXT_MESSAGE = Message.Regular(
        id = "messageID",
        content = MessageContent.Text("Some Text Message"),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
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
    val ASSET_MESSAGE = Message.Regular(
        id = "messageID",
        content = MessageContent.Asset(ASSET_IMAGE_CONTENT),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
    )
    fun buildAssetMessage(assetContent: AssetContent) = Message.Regular(
        id = "messageID",
        content = MessageContent.Asset(assetContent),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
    )

    val MEMBER_REMOVED_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.MemberChange.Removed(listOf(UserId("user-id", "domain"))),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.SENT
    )
    val IMAGE_ASSET_MESSAGE_DATA_TEST = AssetMessageData(
        AssetContent(
            100L,
            "dummy_data.tiff",
            "image/tiff",
            AssetContent.AssetMetadata.Image(50, 50),
            AssetContent.RemoteData(ByteArray(16), ByteArray(16), "asset-id", "token", "domain.com", MessageEncryptionAlgorithm.AES_CBC),
            Message.UploadStatus.NOT_UPLOADED,
            Message.DownloadStatus.NOT_DOWNLOADED
        )
    )
    val UI_MESSAGE_HEADER = MessageHeader(
        username = UIText.DynamicString("username"),
        membership = Membership.Guest,
        isLegalHold = true,
        messageTime = MessageTime("12.23pm"),
        messageStatus = MessageStatus.Untouched,
        messageId = "messageID",
        connectionState = null
    )
    val UI_TEXT_MESSAGE = UIMessage(
        userAvatarData = UserAvatarData(asset = null, availabilityStatus = UserAvailabilityStatus.NONE),
        messageSource = MessageSource.OtherUser,
        messageHeader = UI_MESSAGE_HEADER,
        messageContent = TextMessage(MessageBody(UIText.DynamicString("Some Text Message")))
    )

    val MISSED_CALL_MESSAGE = Message.System(
        id = "messageID",
        content = MessageContent.MissedCall,
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.SENT
    )
}
