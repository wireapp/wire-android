package com.wire.android.feature.conversation.content.domain

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.usecase.SendMessageWorkerScheduler
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class SendMessageServiceTest : UnitTest() {

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var sendMessageWorkerScheduler: SendMessageWorkerScheduler

    @MockK
    private lateinit var messageSender: MessageSender

    private lateinit var sendMessageService: SendMessageService

    private lateinit var testMessage: Message

    @Before
    fun setUp() {
        testMessage = mockk<Message>().also { message ->
            every { message.senderUserId } returns "senderUserId"
            every { message.id } returns "messageId"
        }
        sendMessageService = SendMessageService(messageRepository, sendMessageWorkerScheduler, messageSender)
    }

    @Test
    fun `given a message, when sending or scheduling a new message, then the message should be stored`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest { sendMessageService.sendOrScheduleNewMessage(testMessage) }

        coVerify(exactly = 1) { messageRepository.storeOutgoingMessage(testMessage) }
    }

    @Test
    fun `given a message and successful storage, when sending or scheduling a new message, then right message info is passed to worker`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest { sendMessageService.sendOrScheduleNewMessage(testMessage) }

        val senderId = testMessage.senderUserId
        val messageId = testMessage.id
        coVerify(exactly = 1) {
            messageSender.trySendingOutgoingMessage(senderId, messageId)
        }
    }

    @Test
    fun `given a successful storage and sending, when sending or scheduling a new message, then success is propagated`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
                .shouldSucceed {}
        }
    }

    @Test
    fun `given a successful storage and sending, when sending or scheduling a new message, then no worker is scheduled`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
                .shouldSucceed {}
        }
        coVerify(inverse = true) { sendMessageWorkerScheduler.scheduleMessageSendingWorker(any(), any()) }
    }

    @Test
    fun `given a storage fails, when sending or scheduling a new message, then failure is propagated`() {
        val failure = mockk<Failure>()
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Left(failure)
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given a storage fails, when sending or scheduling a new message, then no worker is scheduled`() {
        val failure = mockk<Failure>()
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Left(failure)
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
        }

        coVerify(inverse = true) { sendMessageWorkerScheduler.scheduleMessageSendingWorker(any(), any()) }
    }

    @Test
    fun `given a storage succeeds and sending fails, when sending or scheduling a new message, then failure is propagated`() {
        val failure = mockk<Failure>()
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Left(failure)

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given sending fails due to network issues, when sending or scheduling a new message, then a worker is scheduled`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Left(NetworkConnection)

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
        }

        val senderUserId = testMessage.senderUserId
        val messageId = testMessage.id
        coVerify {
            sendMessageWorkerScheduler.scheduleMessageSendingWorker(senderUserId, messageId)
        }
    }

    @Test
    fun `given sending fails for another reason, when sending or scheduling a new message, then n worker is scheduled`() {
        coEvery { messageRepository.storeOutgoingMessage(any()) } returns Either.Right(mockk())
        coEvery { messageSender.trySendingOutgoingMessage(any(), any()) } returns Either.Left(NoEntityFound)

        runBlockingTest {
            sendMessageService.sendOrScheduleNewMessage(testMessage)
        }

        coVerify(inverse = true) {
            sendMessageWorkerScheduler.scheduleMessageSendingWorker(any(), any())
        }
    }

}
