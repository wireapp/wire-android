package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.messaging.datasource.remote.mapper.OtrClientIdMapper
import com.wire.messages.Otr
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class OtrClientIdMapperTest : UnitTest() {

    private val subject = OtrClientIdMapper()

    @Test
    fun `given toOtrClientId is being called, when valid clientIds are passed, then the correct mapping should be returned`() {
        subject.toOtrClientId("10") shouldBeEqualTo newOtrClientId(0x10.toLong())

        subject.toOtrClientId("f") shouldBeEqualTo newOtrClientId(0xf.toLong())
    }

    @Test
    fun `given fromOtrClientId is being called, when valid OtrClientIds are passed, then the correct mapping should be returned`() {
        subject.fromOtrClientId(newOtrClientId(0x10.toLong())) shouldBeEqualTo "10"

        subject.fromOtrClientId(newOtrClientId(0xf.toLong())) shouldBeEqualTo "f"
    }

    private fun newOtrClientId(id: Long) = Otr.ClientId.newBuilder().setClient(id).build()
}
