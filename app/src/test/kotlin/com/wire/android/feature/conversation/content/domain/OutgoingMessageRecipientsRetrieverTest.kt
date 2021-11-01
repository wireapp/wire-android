package com.wire.android.feature.conversation.content.domain

import com.wire.android.UnitTest
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.ClientPreKeyInfo
import com.wire.android.shared.prekey.data.UserPreKeyInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class OutgoingMessageRecipientsRetrieverTest : UnitTest() {

    @MockK
    private lateinit var preKeyRepository: PreKeyRepository

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    @MockK
    private lateinit var messageRepository: MessageRepository

    private lateinit var recipientsRetriever: OutgoingMessageRecipientsRetriever

    @Before
    fun setUp() {
        recipientsRetriever = OutgoingMessageRecipientsRetriever(preKeyRepository, conversationRepository, messageRepository)
    }

    @Test
    fun `given conversationRepository fails, when preparing recipients, then the failure should be forwarded`() {
        val failure = mockk<Failure>()
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Left(failure)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Test
    fun `given a conversationId, when preparing recipients, then the conversationRepository should ge the correct id`() {
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Left(mockk())

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
        }

        coVerify(exactly = 1) { conversationRepository.detailedConversationMembers(TEST_CONVERSATION_ID) }
    }

    @Test
    fun `given detailed contacts, when preparing recipients, then the session existence should be verified each client`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(true)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
        }

        coVerifySequence {
            detailedContacts.forEach { detailedContact ->
                detailedContact.clients.forEach { client ->
                    messageRepository.doesCryptoSessionExists(any(), detailedContact.contact.id, client.id)
                }
            }
        }
    }

    @Test
    fun `given a failure happens during session verification, when preparing recipients, then failure is propagated`() {
        val failure = mockk<Failure>()
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Left(failure)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given all clients have sessions, when preparing recipients, then no preKeys should be fetched`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(true)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
        }

        coVerify(inverse = true) {
            preKeyRepository.preKeysOfClientsByUsers(any())
        }
    }

    @Test
    fun `given all clients have sessions, when preparing recipients, then return success`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(true)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)

                .shouldSucceed { }
        }
    }

    @Test
    fun `given clients do not have a session, when preparing recipients, then prekeys should be fetched for them`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(false)
        coEvery { preKeyRepository.preKeysOfClientsByUsers(any()) } returns Either.Right(listOf())
        coEvery { messageRepository.establishCryptoSession(any(), any(), any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
        }

        coVerify {
            preKeyRepository.preKeysOfClientsByUsers(
                mapOf(
                    TEST_DETAILED_CONTACT_1.contact.id to listOf(
                        TEST_CLIENT_1.id,
                        TEST_CLIENT_2.id
                    )
                )
            )
        }
    }

    @Test
    fun `given pre keys were fetched for clients, when preparing recipients, then a session should be established with each pre key`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        val client1 = ClientPreKeyInfo("client1", PreKey(1, "data1"))
        val client2 = ClientPreKeyInfo("client2", PreKey(2, "data2"))
        val fetchedPreKeyInfo1 = UserPreKeyInfo("userId", listOf(client1, client2))
        val client3 = ClientPreKeyInfo("client3", PreKey(3, "data3"))
        val fetchedPreKeyInfo2 = UserPreKeyInfo("userId2", listOf(client3))

        val fetchedUserPreKeyInfo = listOf(fetchedPreKeyInfo1, fetchedPreKeyInfo2)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(false)
        coEvery { preKeyRepository.preKeysOfClientsByUsers(any()) } returns Either.Right(fetchedUserPreKeyInfo)
        coEvery { messageRepository.establishCryptoSession(any(), any(), any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
        }

        coVerify {
            fetchedUserPreKeyInfo.forEach { userPreKeyInfo ->
                userPreKeyInfo.clientsInfo.forEach { clientPreKeyInfo ->
                    messageRepository.establishCryptoSession(
                        TEST_USER_ID,
                        userPreKeyInfo.userId,
                        clientPreKeyInfo.clientId,
                        clientPreKeyInfo.preKey
                    )
                }
            }
        }
    }

    @Test
    fun `given a failure happens when fetching pre keys, when preparing recipients, then failure is propagated`() {
        val failure = mockk<Failure>()
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(false)
        coEvery { preKeyRepository.preKeysOfClientsByUsers(any()) } returns Either.Left(failure)

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given sessions were established successfully, when preparing recipients, then should return propagate success`() {
        val detailedContacts = listOf(TEST_DETAILED_CONTACT_1)
        val client = ClientPreKeyInfo("client3", PreKey(3, "data3"))
        val fetchedPreKeyInfo = UserPreKeyInfo("userId2", listOf(client))

        val fetchedUserPreKeyInfo = listOf(fetchedPreKeyInfo, fetchedPreKeyInfo)
        coEvery { conversationRepository.detailedConversationMembers(any()) } returns Either.Right(detailedContacts)
        coEvery { messageRepository.doesCryptoSessionExists(any(), any(), any()) } returns Either.Right(false)
        coEvery { preKeyRepository.preKeysOfClientsByUsers(any()) } returns Either.Right(fetchedUserPreKeyInfo)
        coEvery { messageRepository.establishCryptoSession(any(), any(), any(), any()) } returns Either.Right(mockk())

        runBlockingTest {
            recipientsRetriever.prepareRecipientsForNewOutgoingMessage(TEST_USER_ID, TEST_CONVERSATION_ID)
                .shouldSucceed {}
        }
    }

    companion object {
        private const val TEST_USER_ID = "userId"
        private const val TEST_CONVERSATION_ID = "convId"
        private val TEST_CONTACT_1 = Contact("contact#1", "Number 1", null)
        private val TEST_CLIENT_1 = ContactClient("ABC")
        private val TEST_CLIENT_2 = ContactClient("DFG")
        private val TEST_DETAILED_CONTACT_1 = DetailedContact(TEST_CONTACT_1, listOf(TEST_CLIENT_1, TEST_CLIENT_2))
    }

}
