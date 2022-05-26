package com.wire.android.ui.home.conversations.delete

data class MessageDeletion(val messageToDeleteId: String, val isSelfMessage: Boolean) {
    override fun toString(): String = "$messageToDeleteId#$isSelfMessage"
}

fun String.parseIntoMessageDeletion(): MessageDeletion? {
    val (messageId, isSelfMessage) = split("#")
    return if (messageId.isNotEmpty() && isSelfMessage.isNotEmpty() && messageId != "null")
        MessageDeletion(messageToDeleteId = messageId, isSelfMessage = isSelfMessage.toBoolean())
    else null
}
