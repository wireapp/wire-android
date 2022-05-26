package com.wire.android.ui.home.conversations.delete

data class MessageDeletion(val messageToDeleteId: String, val isSelfMessage: Boolean) {
    override fun toString(): String = "$messageToDeleteId:$isSelfMessage"
}

fun String.parseIntoMessageDeletion(): MessageDeletion? {
     return if (contains(":")) {
        val (messageId, isSelfMessage) = split(":")
        if (messageId.isNotEmpty() && isSelfMessage.isNotEmpty())
            MessageDeletion(messageToDeleteId = messageId, isSelfMessage = isSelfMessage.toBoolean())
        else null
    } else null
}
