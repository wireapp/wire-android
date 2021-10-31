package com.wire.android.feature.conversation.content.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.content.SendMessageFailure
import com.wire.android.framework.functional.shouldFail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageSendFailureHandlerTest : UnitTest() {

    @MockK
    private lateinit var contactRepository: ContactRepository

    private lateinit var messageSendFailureHandler: MessageSendFailureHandler

    @Before
    fun setUp() {
        messageSendFailureHandler = MessageSendFailureHandler(contactRepository)
    }

    @Test
    fun `given missing clients, when handling a clientsHaveChanged failure, contacts that control these clients should be fetched`() {
        val userOne = "userId" to listOf("clientId", "secondClientId")
        val userTwo = "userId2" to listOf("clientId2", "secondClientId2")
        val failureData = SendMessageFailure.ClientsHaveChanged(missingClientsOfUsers = mapOf(userOne, userTwo), mapOf(), mapOf())
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Right(Unit)
        coEvery { contactRepository.addNewClientsToContact(any(), any()) } returns Either.Right(Unit)

        runBlockingTest {
            messageSendFailureHandler.handleClientsHaveChangedFailure(failureData)
        }

        coVerify(exactly = 1) { contactRepository.fetchContactsById(failureData.missingClientsOfUsers.keys) }
    }

    @Test
    fun `given missing contacts and clients, when handling a clientsHaveChanged failure, clients should be added to contacts`() {
        val userOne = "userId" to listOf("clientId", "secondClientId")
        val userTwo = "userId2" to listOf("clientId2", "secondClientId2")
        val missingClientsOfUsers = mapOf(userOne, userTwo)
        val failureData = SendMessageFailure.ClientsHaveChanged(missingClientsOfUsers = missingClientsOfUsers, mapOf(), mapOf())
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Right(Unit)
        coEvery { contactRepository.addNewClientsToContact(any(), any()) } returns Either.Right(Unit)

        runBlockingTest {
            messageSendFailureHandler.handleClientsHaveChangedFailure(failureData)
        }

        coVerify(exactly = 1) {
            contactRepository.addNewClientsToContact(userOne.first, userOne.second.map(::ContactClient))
            contactRepository.addNewClientsToContact(userTwo.first, userTwo.second.map(::ContactClient))
        }
    }

    @Test
    fun `given repository fails to fetch contacts, when handling a clientsHaveChanged failure, failure should be propagated`() {
        val failure = mockk<Failure>()
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Left(failure)
        coEvery { contactRepository.addNewClientsToContact(any(), any()) } returns Either.Right(Unit)
        val failureData = SendMessageFailure.ClientsHaveChanged(mapOf(), mapOf(), mapOf())

        runBlockingTest {
            messageSendFailureHandler.handleClientsHaveChangedFailure(failureData)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }

    @Test
    fun `given repository fails to add clients to contacts, when handling a clientsHaveChanged failure, failure should be propagated`() {
        val userOne = "userId" to listOf("clientId", "secondClientId")
        val failure = mockk<Failure>()
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Right(Unit)
        coEvery { contactRepository.addNewClientsToContact(any(), any()) } returns Either.Left(failure)
        val failureData = SendMessageFailure.ClientsHaveChanged(mapOf(userOne), mapOf(), mapOf())

        runBlockingTest {
            messageSendFailureHandler.handleClientsHaveChangedFailure(failureData)
                .shouldFail { it shouldBeEqualTo failure }
        }
    }
}
