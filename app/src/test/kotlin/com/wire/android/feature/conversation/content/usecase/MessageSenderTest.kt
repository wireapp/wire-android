package com.wire.android.feature.conversation.content.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import com.wire.android.core.network.NetworkHandler
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class MessageSenderTest : UnitTest() {

    @MockK
    private lateinit var networkHandler: NetworkHandler

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var messageSendFailureHandler: MessageSendFailureHandler

    @MockK
    private lateinit var outgoingMessageRecipientsRetriever: OutgoingMessageRecipientsRetriever

    private lateinit var messageSender: MessageSender

    @Before
    fun setUp() {
        messageSender = MessageSender(
            networkHandler, messageRepository, sessionRepository,
            messageSendFailureHandler, outgoingMessageRecipientsRetriever
        )
    }

    @Test
    fun `given there is no internet connection, when attempting to send a message, then it should fail with NetworkConnection Failure`() {
        every { networkHandler.isConnected() } returns false

        runBlockingTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_USER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeInstanceOf NetworkConnection::class
                }
        }
    }

    @Test
    fun `given there is internet and no running session, when attempting to send a message, then it should fail with NetworkConnection Failure`() {
        every { networkHandler.isConnected() } returns true
        coEvery { sessionRepository.userSession(any()) } returns Either.Left(mockk())

        runBlockingTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_USER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeInstanceOf Unauthorized::class
                }
        }
    }

    @Test
    fun `given failure when retrieving stored message, when attempting to send a message, then it should propagate Failure`() {
        val failure = mockk<Failure>()
        every { networkHandler.isConnected() } returns true
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Left(failure)

        runBlockingTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_USER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Ignore // Failing due to internal Coroutine Context for single-threaded processing.
    @Test
    fun `given contact retriever fails to prepare recipients, when attempting to send a message, then it should propagate Failure`() {
        val failure = mockk<Failure>()
        val message = mockk<Message>().also {
            every { it.conversationId } returns TEST_CONVERSATION_ID
        }
        every { networkHandler.isConnected() } returns true
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(message)
        coEvery { outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any()) } returns Either.Left(failure)

        runBlockingTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_USER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    companion object {
        private const val TEST_SENDER_USER_ID = "senderID"
        private const val TEST_MESSAGE_ID = "messageID"
        private const val TEST_CLIENT_ID = "clientID"
        private const val TEST_CONVERSATION_ID = "conversationID"
        private val TEST_SESSION = Session(TEST_SENDER_USER_ID, TEST_CLIENT_ID, "", "", "")
    }
}
