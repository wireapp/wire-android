package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.messages.Otr
import java.nio.ByteBuffer
import java.util.UUID

class OtrUserIdMapper {

    fun toOtrUserId(userId: String): Otr.UserId {
        val bytes = ByteArray(USER_UID_BYTE_COUNT)
        val byteBuffer = ByteBuffer.wrap(bytes).asLongBuffer()
        val uuid = UUID.fromString(userId)
        byteBuffer.put(uuid.mostSignificantBits)
        byteBuffer.put(uuid.leastSignificantBits)
        return Otr.UserId.newBuilder()
            .setUuid(ByteString.copyFrom(bytes))
            .build()
    }

    fun fromOtrUserId(otrUserId: Otr.UserId): String {
        val bytes = otrUserId.uuid.toByteArray()
        val byteBuffer = ByteBuffer.wrap(bytes)
        return UUID(byteBuffer.long, byteBuffer.long).toString()
    }

    companion object {
        private const val USER_UID_BYTE_COUNT = 16
    }
}
