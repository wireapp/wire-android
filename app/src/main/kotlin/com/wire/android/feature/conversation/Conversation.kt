package com.wire.android.feature.conversation

import android.os.Parcelable
import com.wire.android.core.extension.EMPTY
import kotlinx.android.parcel.Parcelize

data class Conversation(
    val id: ConversationID,
    val name: String? = null,
    val type: ConversationType
)

@Parcelize
data class ConversationID(val value: String, val domain: String) : Parcelable {
    companion object {
        fun blankID() = ConversationID(String.EMPTY, String.EMPTY)
    }
}

