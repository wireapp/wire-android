package com.wire.android.feature.conversation.list.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.list.usecase.GetConversationMembersParams.Companion.ALL_MEMBERS
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class GetConversationMembersUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var contactRepository: ContactRepository

    private lateinit var getConversationMembersUseCase: GetConversationMembersUseCase

    @Before
    fun setUp() {
        getConversationMembersUseCase = GetConversationMembersUseCase(conversationsRepository, contactRepository)
    }

    @Test
    fun `given run is called for a conversation, when conversationsRepository fails to provide member ids, then propagates failure`() {
        val conversation = mockk<Conversation>()
        val params = mockk<GetConversationMembersParams>()
        every { params.conversation } returns conversation

        val failure = mockk<Failure>()
        coEvery { conversationsRepository.conversationMemberIds(conversation) } returns Either.Left(failure)


        val result = runBlocking { getConversationMembersUseCase.run(params) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify { conversationsRepository.conversationMemberIds(conversation) }
    }

    @Test
    fun `given run is called for a conversation & all members, when member ids are provided, then calls contactRepository with all ids`() {
        val conversation = mockk<Conversation>()
        val params = GetConversationMembersParams(conversation, ALL_MEMBERS)

        coEvery { conversationsRepository.conversationMemberIds(conversation) } returns Either.Right(TEST_MEMBER_IDS)
        coEvery { contactRepository.contactsById(any()) } returns Either.Right(mockk())

        val result = runBlocking { getConversationMembersUseCase.run(params) }

        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation) }
        val contactIdsSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(contactIdsSlot)) }
        contactIdsSlot.captured shouldContainSame TEST_MEMBER_IDS
    }

    @Test
    fun `given run is called for a conv & some members, when member ids are provided, then calls contactRepo with given number of ids`() {
        val conversation = mockk<Conversation>()
        val memberCount = 2
        val params = GetConversationMembersParams(conversation, memberCount)

        coEvery { conversationsRepository.conversationMemberIds(conversation) } returns Either.Right(TEST_MEMBER_IDS)
        coEvery { contactRepository.contactsById(any()) } returns Either.Right(mockk())

        val result = runBlocking { getConversationMembersUseCase.run(params) }

        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation) }
        val contactIdsSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(contactIdsSlot)) }
        contactIdsSlot.captured shouldContainSame TEST_MEMBER_IDS.take(memberCount)
    }

    @Test
    fun `given run is called, when contactRepository provides a list of contacts, then propagates success with contacts`() {
        val conversation = mockk<Conversation>()
        val params = GetConversationMembersParams(conversation)

        coEvery { conversationsRepository.conversationMemberIds(conversation) } returns Either.Right(TEST_MEMBER_IDS)
        val contactList = mockk<List<Contact>>()
        coEvery { contactRepository.contactsById(any()) } returns Either.Right(contactList)

        val result = runBlocking { getConversationMembersUseCase.run(params) }

        result shouldSucceed { it shouldBeEqualTo contactList }
        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation) }
        coVerify(exactly = 1) { contactRepository.contactsById(any()) }
    }

    @Test
    fun `given run is called, when contactRepository fails to provide a list of contacts, then propagates the failure`() {
        val conversation = mockk<Conversation>()
        val params = GetConversationMembersParams(conversation)

        coEvery { conversationsRepository.conversationMemberIds(conversation) } returns Either.Right(TEST_MEMBER_IDS)
        val failure = mockk<Failure>()
        coEvery { contactRepository.contactsById(any()) } returns Either.Left(failure)

        val result = runBlocking { getConversationMembersUseCase.run(params) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation) }
        coVerify(exactly = 1) { contactRepository.contactsById(any()) }
    }

    companion object {
        private val TEST_MEMBER_IDS = listOf("id-1", "id-2", "id-3", "id-4")
    }
}
