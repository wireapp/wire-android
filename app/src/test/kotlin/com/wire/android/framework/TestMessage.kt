package com.wire.android.framework

import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId

object TestMessage {
    val TEXT_MESSAGE = Message.Client(
        id = "messageID",
        content = MessageContent.Text("Some Text Message"),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        senderClientId = ClientId("client-id"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
    )
    val ASSET_REMOTE_DATA = AssetContent.RemoteData(
        otrKey = ByteArray(0),
        sha256 = ByteArray(16),
        assetId = "asset-id",
        assetToken = "==some-asset-token",
        assetDomain = "some-asset-domain.com",
        encryptionAlgorithm = AssetContent.RemoteData.EncryptionAlgorithm.AES_GCM
    )
    val ASSET_IMAGE_CONTENT = AssetContent(
        0L, "name", "image", null, ASSET_REMOTE_DATA, Message.DownloadStatus.NOT_DOWNLOADED
    )
    val MEMBER_REMOVED_MESSAGE = Message.Server(
        id = "messageID",
        content = MessageContent.MemberChange.Removed(listOf(Member(UserId("user-id", "domain")))),
        conversationId = ConversationId("convo-id", "convo.domain"),
        date = "some-date",
        senderUserId = UserId("user-id", "domain"),
        status = Message.Status.SENT
    )
    val UI_MESSAGE_HEADER = MessageHeader(
        username = UIText.DynamicString("username"),
        membership = Membership.Guest,
        isLegalHold = true,
        time = "12.23pm",
        messageStatus = MessageStatus.Untouched,
        messageId = "messageID"
    )
    val UI_TEXT_MESSAGE = MessageViewWrapper(
        user = User(avatarAsset = null, availabilityStatus = UserStatus.NONE),
        messageSource = MessageSource.OtherUser,
        messageHeader = UI_MESSAGE_HEADER,
        messageContent = TextMessage(MessageBody(UIText.DynamicString("Some Text Message")))
    )
}
