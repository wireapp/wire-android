package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.messaging.ClientPayload
import com.wire.messages.Otr
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class OtrClientEntryMapperTest : UnitTest() {

    @MockK
    private lateinit var clientIdMapper: OtrClientIdMapper

    private lateinit var subject: OtrClientEntryMapper

    @Before
    fun setup() {
        subject = OtrClientEntryMapper(clientIdMapper)
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping clientId, then client id mapper should be called`() {
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID

        subject.toOtrClientEntry(CLIENT_PAYLOAD)

        verify(exactly = 1) { clientIdMapper.toOtrClientId(any()) }
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping clientId, then client id mapper should be called with the right client id`() {
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID

        subject.toOtrClientEntry(CLIENT_PAYLOAD)

        verify(exactly = 1) { clientIdMapper.toOtrClientId(CLIENT_ID) }
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping clientId, then the result contains the result from mapper`() {
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID

        subject.toOtrClientEntry(CLIENT_PAYLOAD).client shouldBeEqualTo OTR_CLIENT_ID
    }

    @Test
    fun `given fromClientPayLoad is called, when calling toOtrClientId, then the correct userId is passed`() {
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID

        subject.toOtrClientEntry(CLIENT_PAYLOAD)

        verify(exactly = 1) { clientIdMapper.toOtrClientId(CLIENT_ID) }
    }

    companion object {
        private const val CLIENT_ID = "client-ID"
        private val OTR_CLIENT_ID = Otr.ClientId.newBuilder().setClient(42L).build()
        private val BYTE_PAYLOAD = ByteArray(42) { 0x42 }
        private val CLIENT_PAYLOAD = ClientPayload(CLIENT_ID, BYTE_PAYLOAD)
    }
}
