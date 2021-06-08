package com.wire.android.feature.messaging.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.feature.messaging.datasource.remote.api.MessageApi
import com.wire.android.feature.messaging.datasource.remote.mapper.OtrMessageMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import com.wire.messages.Otr
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.any
import org.junit.Before
import org.junit.Test

class MessageRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var otrMessageMapper: OtrMessageMapper

    @MockK
    private lateinit var messageApi: MessageApi

    private lateinit var subject: MessageRemoteDataSource

    @Before
    fun setup() {
        subject = MessageRemoteDataSource(connectedNetworkHandler, otrMessageMapper, messageApi)
    }

    @Test
    fun `given sendMessage was called, when calling api sendMessage, then the mapper result should be passed as parameter`() {
        val mappedValue: Otr.NewOtrMessage = mockk()
        every { otrMessageMapper.fromMessageEnvelope(any()) } returns mappedValue
        coEvery { messageApi.sendMessage(any(), any()) } returns mockk()

        runBlocking { subject.sendMessage(any(), any()) }

        coVerify(exactly = 1) { messageApi.sendMessage(any(), mappedValue) }
    }

    @Test
    fun `given sendMessage was called, when calling api sendMessage, then conversationId should be forwarded as parameter`() {
        val conversationId = "Conversation-ID"
        every { otrMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockk()

        runBlocking { subject.sendMessage(conversationId, any()) }

        coVerify(exactly = 1) { messageApi.sendMessage(conversationId, any()) }
    }

    @Test
    fun `given sendMessage was called, when api sendMessage succeeds, then the success is returned`() {
        every { otrMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkResponse()

        runBlocking { subject.sendMessage(any(), any()) }
            .shouldSucceed { }
    }

    @Test
    fun `given sendMessage was called, when api sendMessage fails, then the failure is returned`() {
        every { otrMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkError()

        runBlocking { subject.sendMessage(any(), any()) }
            .shouldFail()
    }

}
