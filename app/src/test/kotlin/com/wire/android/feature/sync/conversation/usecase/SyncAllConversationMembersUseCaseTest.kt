package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class SyncAllConversationMembersUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    @MockK
    private lateinit var contactRepository: ContactRepository

    private lateinit var syncAllConversationMembersUseCase: SyncAllConversationMembersUseCase

    @Before
    fun setUp() {
        syncAllConversationMembersUseCase = SyncAllConversationMembersUseCase(conversationRepository, contactRepository)
    }

    @Test
    fun `given convRepo fails to retrieve all member ids, when run is called, then directly propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationRepository.allConversationMemberIds() } returns Either.Left(failure)

        val result = runBlocking { syncAllConversationMembersUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationRepository.allConversationMemberIds() }
        verify { contactRepository wasNot Called }
    }

    @Test
    fun `given convRepo retrieves all member ids, when run is called, then calls contactRepo to fetch contact info`() {
        coEvery { conversationRepository.allConversationMemberIds() } returns Either.Right(TEST_MEMBER_IDS)
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Left(mockk())

        runBlocking { syncAllConversationMembersUseCase.run(Unit) }

        coVerify(exactly = 1) { conversationRepository.allConversationMemberIds() }
        val contactIdsSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.fetchContactsById(capture(contactIdsSlot)) }
        contactIdsSlot.captured shouldContainSame TEST_MEMBER_IDS
    }

    @Test
    fun `given contactRepo fails to fetch contacts and convRepo retrieved all member ids, when run is called , then propagates failure`() {
        coEvery { conversationRepository.allConversationMemberIds() } returns Either.Right(TEST_MEMBER_IDS)
        val failure = mockk<Failure>()
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Left(failure)

        val result = runBlocking { syncAllConversationMembersUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given contactRepo fetches contacts and convRepo retrieved all member ids, when run is called, then propagates success`() {
        coEvery { conversationRepository.allConversationMemberIds() } returns Either.Right(TEST_MEMBER_IDS)
        coEvery { contactRepository.fetchContactsById(any()) } returns Either.Right(mockk())

        val result = runBlocking { syncAllConversationMembersUseCase.run(Unit) }

        result shouldSucceed { }
    }

    companion object {
        private val TEST_MEMBER_IDS = listOf("contact-id-1", "contact-id-2")
    }
}
