package com.wire.android.feature.messaging.datasource.mapper

import com.waz.model.Messages
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.messaging.MessageContent

class TextMessageMapper : ContentMapper<MessageContent.Text, Messages.Text> {

    override fun toProtoBuf(content: MessageContent.Text): Either<Failure, Messages.Text> {
        return Either.Right(
            Messages.Text.newBuilder()
                .setContent(content.text)
                //TODO Add other fields, like mentions, link previews, etc.
                .build()
        )
    }

    override fun fromProtoBuf(protoMessage: Messages.Text): Either<Failure, MessageContent.Text> =
        Either.Right(MessageContent.Text(protoMessage.content))

}
