package com.wire.android.feature.conversation.content.domain

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.SendMessageFailure
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MessageSenderTest : UnitTest() {

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var messageSendFailureHandler: MessageSendFailureHandler

    @MockK
    private lateinit var outgoingMessageRecipientsRetriever: OutgoingMessageRecipientsRetriever

    @MockK
    private lateinit var messageEnvelopeCreator: MessageEnvelopeCreator

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var messageSender: MessageSender

    @Before
    fun setUp() {
        messageSender = MessageSender(
            messageRepository,
            sessionRepository,
            messageSendFailureHandler,
            outgoingMessageRecipientsRetriever,
            messageEnvelopeCreator,
            coroutinesTestRule.dispatcherProvider
        )
    }

    @Test
    fun `given sessionRepository fails to fetch a session for user, when sending a message, then Unauthorized should be returned`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Left(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo Unauthorized
                }
        }
    }

    @Test
    fun `given a session with no clientId set, when sending a message, then Unauthorized should be returned`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION.copy(clientId = null))

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo Unauthorized
                }
        }
    }

    @Test
    fun `given a userId, when sending a message, then a session should be fetched with that ID`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION.copy(clientId = null))

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerify(exactly = 1) { sessionRepository.userSession(TEST_SENDER_ID) }
    }

    @Test
    fun `given a messageId, when sending a message, then the messageRepository should fetch a message by this Id`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Left(failure)

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Test
    fun `given repository fails to fetch stored message, when sending a message, then the failure should be forwarded`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Left(failure)

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Test
    fun `given message was retrieved, when sending a message, then should prepare recipients for new message using right parameters`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any()) } returns Either.Left(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerify(exactly = 1) {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_SENDER_ID, TEST_CONVERSATION_ID)
        }
    }

    @Test
    fun `given a failure when preparing recipients, when sending a message, then failure should be propagated`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any()) } returns Either.Left(failure)

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given recipients were prepared, when sending a message, then envelope creator should receive the correct parameters`() {
        val message = mockMessage()
        val detailedContacts = mockk<List<DetailedContact>>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(message)
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Left(mockk())
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(detailedContacts)

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerify(exactly = 1) {
            messageEnvelopeCreator.createOutgoingEnvelope(detailedContacts, TEST_CLIENT_ID, TEST_SENDER_ID, message)
        }
    }

    @Test
    fun `given a failure when creating envelope, when sending a message, then failure should be propagated`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Left(failure)
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Test
    fun `given envelope was created, when sending a message, then envelope should be sent using right parameters`() {
        val chatMessageEnvelope = mockk<ChatMessageEnvelope>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(chatMessageEnvelope)
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returns Either.Right(mockk())
        coEvery { messageRepository.markMessageAsSent(any()) } returns Either.Right(mockk())
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerify(exactly = 1) { messageRepository.sendMessageEnvelope(TEST_CONVERSATION_ID, chatMessageEnvelope) }
    }

    @Test
    fun `given a network failure, when sending a message, then the failure should be propagated`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(mockk())
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returns Either.Left(SendMessageFailure.NetworkFailure)
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeInstanceOf NetworkConnection::class
                }
        }
    }

    @Test
    fun `given a clients have changed failure, when sending a message, then the failure handler should receive the failure`() {
        val clientsHaveChangedFailure = mockk<SendMessageFailure.ClientsHaveChanged>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(mockk())
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returns Either.Left(clientsHaveChangedFailure)
        coEvery { messageSendFailureHandler.handleClientsHaveChangedFailure(any()) } returns Either.Left(mockk())
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerify(exactly = 1) { messageSendFailureHandler.handleClientsHaveChangedFailure(clientsHaveChangedFailure) }
    }

    @Test
    fun `given clientsHaveChanged failure is handled, when sending a message, then the a new attempt at sending should be made`() {
        val clientsHaveChangedFailure = mockk<SendMessageFailure.ClientsHaveChanged>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(mockk())
        coEvery { messageSendFailureHandler.handleClientsHaveChangedFailure(any()) } returns Either.Right(mockk())
        coEvery { messageRepository.markMessageAsSent(any()) } returns Either.Right(mockk())
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returnsMany listOf(
            Either.Left(clientsHaveChangedFailure), // Fails
            Either.Right(mockk())                   // and succeeds on the second try
        )

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerifyOrder {
            messageRepository.sendMessageEnvelope(any(), any())
            messageSendFailureHandler.handleClientsHaveChangedFailure(any())
            messageRepository.sendMessageEnvelope(any(), any())
        }
    }

    @Test
    fun `given clientsHaveChanged failure handling fails, when sending a message, then the failure should be propagated`() {
        val failure = mockk<Failure>()
        val clientsHaveChangedFailure = mockk<SendMessageFailure.ClientsHaveChanged>()
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(mockk())
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returns Either.Left(clientsHaveChangedFailure)
        coEvery { messageSendFailureHandler.handleClientsHaveChangedFailure(any()) } returns Either.Left(failure)
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }


    @Test
    fun `given envelope was sent, when sending a message, then the message should be marked as sent`() {
        coEvery { sessionRepository.userSession(any()) } returns Either.Right(TEST_SESSION)
        coEvery { messageRepository.messageById(any()) } returns Either.Right(mockMessage())
        coEvery { messageEnvelopeCreator.createOutgoingEnvelope(any(), any(), any(), any()) } returns Either.Right(mockk())
        coEvery { messageSendFailureHandler.handleClientsHaveChangedFailure(any()) } returns Either.Right(mockk())
        coEvery { messageRepository.markMessageAsSent(any()) } returns Either.Right(mockk())
        coEvery {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(any(), any())
        } returns Either.Right(mockk())
        coEvery { messageRepository.sendMessageEnvelope(any(), any()) } returns Either.Right(mockk())

        coroutinesTestRule.runTest {
            messageSender.trySendingOutgoingMessage(TEST_SENDER_ID, TEST_MESSAGE_ID)
        }

        coVerifyOrder {
            messageRepository.markMessageAsSent(TEST_MESSAGE_ID)
        }
    }

    private fun mockMessage(): Message {
        val message = mockk<Message>().also {
            every { it.id } returns TEST_MESSAGE_ID
            every { it.senderUserId } returns TEST_SENDER_ID
            every { it.conversationId } returns TEST_CONVERSATION_ID
        }
        return message
    }

    companion object {
        private const val TEST_MESSAGE_ID = "messageId"
        private const val TEST_CONVERSATION_ID = "conversationId"
        private const val TEST_SENDER_ID = "senderId"
        private const val TEST_CLIENT_ID = "clientId"
        private val TEST_SESSION = Session(TEST_SENDER_ID, TEST_CLIENT_ID, "", "", "")
    }
}
