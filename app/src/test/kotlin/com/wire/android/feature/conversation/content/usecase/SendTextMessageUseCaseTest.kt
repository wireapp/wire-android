package com.wire.android.feature.conversation.content.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Content
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.Pending
import com.wire.android.feature.conversation.content.domain.SendMessageService
import com.wire.android.framework.functional.shouldFail
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class SendTextMessageUseCaseTest : UnitTest() {

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var sendMessageService: SendMessageService

    private lateinit var sendTextMessageUseCase: SendTextMessageUseCase

    @Before
    fun setUp() {
        sendTextMessageUseCase = SendTextMessageUseCase(sessionRepository, sendMessageService)
    }

    @Test
    fun `given a current session and send parameters, when sending a text message, then service should get correct parameters`() {
        val currentSession = mockk<Session>().also {
            every { it.userId } returns TEST_USER_ID
            every { it.clientId } returns TEST_CLIENT_ID
        }
        val messageSlot = slot<Message>()
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        coEvery { sendMessageService.sendOrScheduleNewMessage(capture(messageSlot)) } returns Either.Right(mockk())

        runBlockingTest {
            sendTextMessageUseCase.run(TEST_SEND_PARAMETERS).collect()
        }

        coVerify(exactly = 1) { sendMessageService.sendOrScheduleNewMessage(any()) }

        messageSlot.captured.let {
            it.senderUserId shouldBeEqualTo currentSession.userId
            it.clientId shouldBeEqualTo currentSession.clientId
            it.conversationId shouldBeEqualTo TEST_SEND_PARAMETERS.conversationId
            it.content shouldBeEqualTo Content.Text(TEST_SEND_PARAMETERS.text)
            it.state shouldBeEqualTo Pending
        }
    }

    @Test
    fun `given failure when getting current session, when sending a text message, then failure should be propagated`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.currentSession() } returns Either.Left(failure)

        runBlockingTest {
            sendTextMessageUseCase.run(TEST_SEND_PARAMETERS).collect {
                it.shouldFail { failure ->
                    failure shouldBeEqualTo failure
                }
            }
        }
    }

    @Test
    fun `given failure when sending message, when sending a text message, then failure should be propagated`() {
        val currentSession = mockk<Session>().also {
            every { it.userId } returns TEST_USER_ID
            every { it.clientId } returns TEST_CLIENT_ID
        }
        coEvery { sessionRepository.currentSession() } returns Either.Right(currentSession)
        val failure = mockk<Failure>()
        coEvery { sendMessageService.sendOrScheduleNewMessage(any()) } returns Either.Left(failure)

        runBlockingTest {
            sendTextMessageUseCase.run(TEST_SEND_PARAMETERS).collect {
                it.shouldFail { failure ->
                    failure shouldBeEqualTo failure
                }
            }
        }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "convId"
        private const val TEST_MESSAGE_TEXT = "Servus"
        private const val TEST_USER_ID = "testUserId"
        private const val TEST_CLIENT_ID = "testClientId"
        private val TEST_SEND_PARAMETERS = SendTextMessageUseCaseParams(TEST_CONVERSATION_ID, TEST_MESSAGE_TEXT)
    }
}
