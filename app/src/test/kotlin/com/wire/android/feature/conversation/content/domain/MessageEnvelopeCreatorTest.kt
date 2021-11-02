package com.wire.android.feature.conversation.content.domain

import com.wire.android.UnitTest
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.Content
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldMatchAllWith
import org.junit.Before
import org.junit.Test

class MessageEnvelopeCreatorTest : UnitTest() {

    @MockK
    private lateinit var messageRepository: MessageRepository

    private lateinit var messageEnvelopeCreator: MessageEnvelopeCreator

    @Before
    fun setUp() {
        messageEnvelopeCreator = MessageEnvelopeCreator(messageRepository)
    }

    @Test
    fun `given a list of detailed contacts, when creating envelope, then the repository should be called to encrypt for each client`() {
        val encryptedMessage = EncryptedMessage(byteArrayOf())
        coEvery { messageRepository.encryptMessageContent(any(), any(), any(), any(), any()) } returns Either.Right(encryptedMessage)

        runBlockingTest {
            messageEnvelopeCreator.createOutgoingEnvelope(
                TEST_DETAILED_CONTACTS,
                TEST_SENDER_CLIENT_ID,
                TEST_SENDER_USER_ID,
                mockMessage()
            )
        }

        coVerifySequence {
            TEST_DETAILED_CONTACTS.forEach { detailedContact ->
                val contactId = detailedContact.contact.id
                detailedContact.clients.forEach { client ->
                    val receiverClientId = client.id
                    messageRepository.encryptMessageContent(TEST_SENDER_USER_ID, contactId, receiverClientId, TEST_MESSAGE_ID, TEST_CONTENT)
                }
            }
        }
    }

    @Test
    fun `given detailed contacts and repository succeeds, when creating envelope, then should succeed returning all recipient entries`() {
        val encryptedMessage = EncryptedMessage(byteArrayOf())
        coEvery { messageRepository.encryptMessageContent(any(), any(), any(), any(), any()) } returns Either.Right(encryptedMessage)

        runBlockingTest {
            messageEnvelopeCreator.createOutgoingEnvelope(
                TEST_DETAILED_CONTACTS,
                TEST_SENDER_CLIENT_ID,
                TEST_SENDER_USER_ID,
                mockMessage()
            ).shouldSucceed { envelope ->
                // Should get a corresponding contact for the envelope entry
                envelope.senderClientId shouldBeEqualTo TEST_SENDER_CLIENT_ID
                // For each contact
                TEST_DETAILED_CONTACTS.forEach { detailedContact ->
                    // Should get a matching recipient entry in the created envelope
                    val matchingRecipientEntry = envelope.recipients.first { recipientEntry ->
                        detailedContact.contact.id == recipientEntry.userId
                    }
                    // All clients of this contact should have a matching payload in the entry
                    detailedContact.clients.shouldMatchAllWith { contactClient ->
                        matchingRecipientEntry.clientPayloads.any { clientPayload ->
                            clientPayload.clientId == contactClient.id
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `given the repository fails to encrypt, when creating envelope, then the failure should be propagated`() {
        val failure = mockk<Failure>()
        coEvery { messageRepository.encryptMessageContent(any(), any(), any(), any(), any()) } returns Either.Left(failure)

        runBlockingTest {
            messageEnvelopeCreator.createOutgoingEnvelope(
                TEST_DETAILED_CONTACTS,
                TEST_SENDER_CLIENT_ID,
                TEST_SENDER_USER_ID,
                mockMessage()
            ).shouldFail {
                it shouldBeEqualTo failure
            }
        }
    }

    @Test
    fun `given the repository fails to encrypt for a client, when creating envelope, then no more encryptions should be done`() {
        val failure = mockk<Failure>()
        coEvery { messageRepository.encryptMessageContent(any(), any(), any(), any(), any()) } returns Either.Left(failure)

        runBlockingTest {
            messageEnvelopeCreator.createOutgoingEnvelope(
                TEST_DETAILED_CONTACTS,
                TEST_SENDER_CLIENT_ID,
                TEST_SENDER_USER_ID,
                mockMessage()
            )
        }

        coVerify(exactly = 1) { messageRepository.encryptMessageContent(any(), any(), any(), any(), any()) }
    }

    private fun mockMessage(): Message = mockk<Message>().also {
        every { it.id } returns TEST_MESSAGE_ID
        every { it.content } returns TEST_CONTENT
    }

    companion object {
        private const val TEST_SENDER_USER_ID = "senderUserId"
        private const val TEST_SENDER_CLIENT_ID = "senderClientId"
        private const val TEST_MESSAGE_ID = "messageId"
        private val TEST_CONTENT = Content.Text("Servus")
        private val TEST_CONTACT_1 = Contact("contactId", "A Name", null)
        private val TEST_CONTACT_CLIENT_1 = ContactClient("clientId1")
        private val TEST_CONTACT_CLIENT_2 = ContactClient("clientId2")
        private val TEST_CONTACT_2 = Contact("contactId2", "Second Name", null)
        private val TEST_CONTACT_CLIENT_3 = ContactClient("clientId3")
        private val TEST_DETAILED_CONTACT_1 = DetailedContact(TEST_CONTACT_1, listOf(TEST_CONTACT_CLIENT_1, TEST_CONTACT_CLIENT_2))
        private val TEST_DETAILED_CONTACT_2 = DetailedContact(TEST_CONTACT_2, listOf(TEST_CONTACT_CLIENT_3))
        private val TEST_DETAILED_CONTACTS = listOf(TEST_DETAILED_CONTACT_1, TEST_DETAILED_CONTACT_2)
    }
}
