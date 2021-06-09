package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.messaging.ClientPayload
import com.wire.messages.Otr
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
        val data: ClientPayload = mockk()
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID
        every { data.payload } returns PAYLOAD
        every { data.clientId } returns CLIENT_ID

        subject.toOtrClientEntry(data)

        verify(exactly = 1) { clientIdMapper.toOtrClientId(any()) }
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping clientId, then client id mapper should be called with the right client id`() {
        val data: ClientPayload = mockk()
        val expectedClientId = CLIENT_ID
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID
        every { data.payload } returns PAYLOAD
        every { data.clientId } returns expectedClientId

        subject.toOtrClientEntry(data)

        verify(exactly = 1) { clientIdMapper.toOtrClientId(expectedClientId) }
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping clientId, then the result contains the result from mapper`() {
        val data: ClientPayload = mockk()
        val expectedOtrClientId = OTR_CLIENT_ID

        every { data.payload } returns PAYLOAD
        every { data.clientId } returns CLIENT_ID
        every { clientIdMapper.toOtrClientId(any()) } returns expectedOtrClientId

        subject.toOtrClientEntry(data).client shouldBeEqualTo expectedOtrClientId
    }

    @Test
    fun `given fromClientPayLoad is called, when mapping payload, then `() {
        val data: ClientPayload = mockk()
        every { clientIdMapper.toOtrClientId(any()) } returns OTR_CLIENT_ID
        every { data.payload } returns PAYLOAD
        every { data.clientId } returns CLIENT_ID

        subject.toOtrClientEntry(data)
    }


    companion object {
        private const val CLIENT_ID = "client-ID"
        private val OTR_CLIENT_ID = Otr.ClientId.newBuilder().setClient(42L).build()
        private val PAYLOAD = ByteArray(42) { 0x42 }
    }
}