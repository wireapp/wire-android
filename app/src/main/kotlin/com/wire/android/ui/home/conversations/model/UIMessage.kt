package com.wire.android.ui.home.conversations.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.AssetId

data class UIMessage(
    val userAvatarData: UserAvatarData,
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

sealed class MessageStatus(val text: UIText) {
    object Untouched : MessageStatus(UIText.DynamicString(""))
    object Deleted : MessageStatus(UIText.StringResource(R.string.deleted_message_text))
    data class Edited(val formattedEditTimeStamp: String) :
        MessageStatus(UIText.StringResource(R.string.label_message_status_edited_with_date, formattedEditTimeStamp))
    object SendFailure : MessageStatus(UIText.StringResource(R.string.label_message_sent_failure))
    object ReceiveFailure : MessageStatus(UIText.StringResource(R.string.label_message_receive_failure))
}

sealed class MessageContent {

    sealed class ClientMessage : MessageContent()

    data class TextMessage(val messageBody: MessageBody) : ClientMessage()

    data class RestrictedAsset(val mimeType: String) : ClientMessage()
    data class AssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val assetSizeInBytes: Long,
        val downloadStatus: Message.DownloadStatus
    ) : ClientMessage()

    data class ImageMessage(val assetId: AssetId, val rawImgData: ByteArray?, val width: Int, val height: Int) : MessageContent() {
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

    sealed class SystemMessage(@DrawableRes val iconResId: Int?, @StringRes val stringResId: Int) : MessageContent() {
        data class MemberAdded(
            val author: UIText,
            val memberNames: List<UIText>
        ) : SystemMessage(R.drawable.ic_add, R.string.label_system_message_added)

        data class MemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>
        ) : SystemMessage(R.drawable.ic_minus, R.string.label_system_message_removed)

        data class MemberLeft(
            val author: UIText
        ) : SystemMessage(R.drawable.ic_minus, R.string.label_system_message_left_the_conversation)
    }
}

data class MessageBody(
    val message: UIText
)

enum class MessageSource {
    Self, OtherUser
}
