package com.wire.android.ui.home.conversations.model

import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserAssetId

data class MessageViewWrapper(
    val user: User,
    val messageSource: MessageSource,
    val messageHeader: MessageHeader,
    val messageContent: MessageContent?,
) {
    val isDeleted: Boolean = messageHeader.messageStatus == MessageStatus.Deleted
    val sendingFailed: Boolean = messageHeader.messageStatus == MessageStatus.SendFailure
    val receivingFailed: Boolean = messageHeader.messageStatus == MessageStatus.ReceiveFailure
}

data class MessageHeader(
    val username: UIText,
    val membership: Membership,
    val isLegalHold: Boolean,
    val time: String,
    val messageStatus: MessageStatus,
    val messageId: String
)

enum class MessageStatus(val stringResourceId: Int) {
    Untouched(-1),
    Deleted(R.string.label_message_status_deleted),
    Edited(R.string.label_message_status_edited),
    SendFailure(R.string.label_message_sent_failure),
    ReceiveFailure(R.string.label_message_receive_failure)
}

sealed class MessageContent {
    data class TextMessage(val messageBody: MessageBody) : MessageContent()
    object DeletedMessage : MessageContent()

    data class AssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: String,
        val assetSizeInBytes: Long,
        val downloadStatus: Message.DownloadStatus
    ) : MessageContent()

    data class ImageMessage(val assetId: UserAssetId, val rawImgData: ByteArray?, val width: Int, val height: Int) : MessageContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ImageMessage
            if (assetId != other.assetId) return false
            if (!rawImgData.contentEquals(other.rawImgData)) return false
            return true
        }

        override fun hashCode(): Int {
            return rawImgData.contentHashCode()
        }
    }
}

data class MessageBody(
    val message: UIText
)

data class User(
    val avatarAsset: UserAvatarAsset?,
    val availabilityStatus: UserAvailabilityStatus,
)

enum class MessageSource {
    Self, OtherUser
}
