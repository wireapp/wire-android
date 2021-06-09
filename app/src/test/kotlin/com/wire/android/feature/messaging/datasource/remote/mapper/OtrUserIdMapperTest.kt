package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.android.UnitTest
import com.wire.messages.Otr
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class OtrUserIdMapperTest : UnitTest() {

    private val subject = OtrUserIdMapper()

    @Test
    fun `given toOtrUserId is being called, when valid userIds are passed, then the correct mapping should be returned`() {
        subject.toOtrUserId(USER_ID) shouldBeEqualTo OTR_UUID
    }

    @Test
    fun `given fromOtrUserId is being called, when valid OtrUserIds are passed, then the correct mapping should be returned`() {
        subject.fromOtrUserId(OTR_UUID) shouldBeEqualTo USER_ID
    }

    @Test
    fun `given toOtrUserId is being called, when reverting fromOtrUserId, then the original value should be returned`() {
        subject.toOtrUserId(USER_ID).let(subject::fromOtrUserId) shouldBeEqualTo USER_ID
    }

    @Test
    fun `given fromOtrUserId is being called, when reverting toOtrUserId, then the original value should be returned`() {
        subject.fromOtrUserId(OTR_UUID).let(subject::toOtrUserId) shouldBeEqualTo OTR_UUID
    }

    companion object {
        private const val USER_ID = "76ebeb16-a849-4be4-84a7-157654b492cf"
        private val OTR_UUID = Otr.UserId.newBuilder()
            .setUuid(
                ByteString.copyFrom(byteArrayOf(118, -21, -21, 22, -88, 73, 75, -28, -124, -89, 21, 118, 84, -76, -110, -49))
            )
            .build()
    }
}
