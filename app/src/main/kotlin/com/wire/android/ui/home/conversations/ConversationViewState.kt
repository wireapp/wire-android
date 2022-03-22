package com.wire.android.ui.home.conversations

import android.os.Parcelable
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.Message
import kotlinx.parcelize.Parcelize

data class ConversationViewState(
    val conversationName: String = "",
    val messages: List<Message> = emptyList(),
    val messageText: TextFieldValue = TextFieldValue("")
)

/**
 * Represents an attachment message to be sent
 */
@Parcelize
data class AttachmentPart(val mimeType: String, val rawContent: ByteArray) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentPart

        if (mimeType != other.mimeType) return false
        if (!rawContent.contentEquals(other.rawContent)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mimeType.hashCode()
        result = 31 * result + rawContent.contentHashCode()
        return result
    }
}
