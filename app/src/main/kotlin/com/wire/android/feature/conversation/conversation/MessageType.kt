package com.wire.android.feature.conversation.conversation

//Check com.waz.api.Message class in Scala code base to get all message types
sealed class MessageType

object Text: MessageType()
object Unknown: MessageType()
