package com.wire.android.feature.messaging.datasource.mapper

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.messaging.MessageContent

interface ContentMapper<Content : MessageContent, ProtoData> {

    fun toProtoBuf(content: Content): Either<Failure, ProtoData>

    fun fromProtoBuf(protoMessage: ProtoData): Either<Failure, Content>
}
