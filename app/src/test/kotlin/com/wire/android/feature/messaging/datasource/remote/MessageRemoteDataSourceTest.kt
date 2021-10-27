package com.wire.android.feature.messaging.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.core.network.either.EitherResponse
import com.wire.android.feature.messaging.datasource.remote.api.MessageApi
import com.wire.android.feature.messaging.datasource.remote.api.MessageSendingErrorBody
import com.wire.android.feature.messaging.datasource.remote.mapper.OtrNewMessageMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkEitherErrorBodyFailure
import com.wire.android.framework.network.mockNetworkEitherResponse
import com.wire.android.framework.network.mockNetworkEitherThrowableFailure
import com.wire.messages.Otr
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class MessageRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var otrNewMessageMapper: OtrNewMessageMapper

    @MockK
    private lateinit var messageApi: MessageApi

    private lateinit var subject: MessageRemoteDataSource

    @Before
    fun setup() {
        subject = MessageRemoteDataSource(connectedNetworkHandler, otrNewMessageMapper, messageApi)
    }

    @Test
    fun `given the mapper result, when calling sendMessage, then the mapper result should be passed as parameter`() {
        val mappedValue: Otr.NewOtrMessage = mockk()
        every { otrNewMessageMapper.fromMessageEnvelope(any()) } returns mappedValue
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkEitherResponse()

        runBlocking { subject.sendMessage(any(), any()) }

        coVerify(exactly = 1) { messageApi.sendMessage(any(), mappedValue) }
    }

    @Test
    fun `given a conversationId, when calling sendMessage, then conversationId should be forwarded as parameter`() {
        val conversationId = "Conversation-ID"
        every { otrNewMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkEitherResponse()

        runBlocking { subject.sendMessage(conversationId, any()) }

        coVerify(exactly = 1) { messageApi.sendMessage(conversationId, any()) }
    }

    @Test
    fun `given api sendMessage succeeds, when calling sendMessage, then the success is returned`() {
        every { otrNewMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkEitherResponse()

        runBlocking { subject.sendMessage(any(), any()) }
            .shouldSucceed { }
    }

    @Test
    fun `given api sendMessage fails, when calling sendMessage, then the failure is returned`() {
        every { otrNewMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkEitherThrowableFailure()

        runBlocking { subject.sendMessage(any(), any()) }
            .shouldFail {
                it shouldBeInstanceOf EitherResponse.Failure.Exception::class
            }
    }

    @Test
    fun `given api sendMessage returns client mismatch, when calling sendMessage, then the client info is returned`() {
        val sendingErrorBody = mockk<MessageSendingErrorBody>()
        every { otrNewMessageMapper.fromMessageEnvelope(any()) } returns mockk()
        coEvery { messageApi.sendMessage(any(), any()) } returns mockNetworkEitherErrorBodyFailure(sendingErrorBody)

        runBlocking { subject.sendMessage(any(), any()) }
            .shouldFail {
                it shouldBeInstanceOf EitherResponse.Failure.ErrorBody::class
                val failure = it as EitherResponse.Failure.ErrorBody<MessageSendingErrorBody>
                failure.errorBody shouldBeEqualTo sendingErrorBody
            }
    }
}
