package com.wire.android.feature.conversation.list.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository
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

class GetMembersOfConversationsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var contactRepository: ContactRepository

    private lateinit var getMembersOfConversationsUseCase: GetMembersOfConversationsUseCase

    @Before
    fun setUp() {
        getMembersOfConversationsUseCase = GetMembersOfConversationsUseCase(conversationsRepository, contactRepository)
    }

    @Test
    fun `given conversations, when conversationRepository fails to provide members of a conv, then proceeds as if no members exist`() {
        val conversation1 = mockk<Conversation>()
        val conversation2 = mockk<Conversation>()
        val conversation3 = mockk<Conversation>()
        val failure = mockk<Failure>()

        coEvery { conversationsRepository.conversationMemberIds(any()) } returnsMany listOf(
            Either.Right(listOf("member-1")),
            Either.Left(failure),
            Either.Right(listOf("member-3"))
        )
        coEvery { contactRepository.contactsById(any()) } returns Either.Left(ServerError)

        val params = GetMembersOfConversationsParams(listOf(conversation1, conversation2, conversation3))
        runBlocking { getMembersOfConversationsUseCase.run(params) }

        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation1) }
        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation2) }
        coVerify(exactly = 1) { conversationsRepository.conversationMemberIds(conversation3) }

        val memberIdSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(memberIdSlot)) }
        memberIdSlot.captured shouldContainSame setOf("member-1", "member-3")
    }


    @Test
    fun `given a maxMemberCount, when convRepo is successful, then calls contactRepo with no more than maxMemberCount members per conv`() {
        val memberIdList1 = listOf("member-1-1", "member-1-2", "member-1-3", "member-1-4", "member-1-5")
        val memberIdList2 = listOf("member-2-1", "member-2-2", "member-2-3")
        val memberIdList3 = listOf("member-3-1", "member-3-2")

        coEvery { conversationsRepository.conversationMemberIds(any()) } returnsMany listOf(
            Either.Right(memberIdList1),
            Either.Right(memberIdList2),
            Either.Right(memberIdList3)
        )
        coEvery { contactRepository.contactsById(any()) } returns Either.Left(mockk())

        val params = GetMembersOfConversationsParams(
            conversations = listOf(mockk(), mockk(), mockk()),
            maxMemberCountPerConversation = 3
        )
        runBlocking { getMembersOfConversationsUseCase.run(params) }

        val memberIdSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(memberIdSlot)) }
        memberIdSlot.captured shouldContainSame setOf(
            "member-1-1", "member-1-2", "member-1-3",
            "member-2-1", "member-2-2", "member-2-3",
            "member-3-1", "member-3-2"
        )
    }

    @Test
    fun `given conversations, when conversationRepo is successful but contactRepo fails to fetch contact info, then propagates failure`() {
        coEvery { conversationsRepository.conversationMemberIds(any()) } returnsMany listOf(
            Either.Right(listOf("member-1")),
            Either.Right(listOf("member-2-1", "member-2-2")),
            Either.Right(listOf("member-3"))
        )
        val failure = mockk<Failure>()
        coEvery { contactRepository.contactsById(any()) } returns Either.Left(failure)

        val params = GetMembersOfConversationsParams(listOf(mockk(), mockk(), mockk()))
        val result = runBlocking { getMembersOfConversationsUseCase.run(params) }

        val memberIdSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(memberIdSlot)) }
        memberIdSlot.captured shouldContainSame setOf("member-1", "member-2-1", "member-2-2", "member-3")

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given conversations, when conversationRepo is successful but contactRepo provides partial info, then ignores missing info`() {
        val conversation1 = mockk<Conversation>()
        val conversation2 = mockk<Conversation>()
        val conversation3 = mockk<Conversation>()

        coEvery { conversationsRepository.conversationMemberIds(any()) } returnsMany listOf(
            Either.Right(listOf("member-1")),
            Either.Right(listOf("member-2-1", "member-2-2")),
            Either.Right(listOf("member-3"))
        )

        val contact2_1 = mockContactWithId("member-2-1")
        val contact2_2 = mockContactWithId("member-2-2")
        val contact3 = mockContactWithId("member-3")
        coEvery { contactRepository.contactsById(any()) } returns Either.Right(listOf(contact2_1, contact2_2, contact3))

        val params = GetMembersOfConversationsParams(listOf(conversation1, conversation2, conversation3))
        val result = runBlocking { getMembersOfConversationsUseCase.run(params) }

        val memberIdSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(memberIdSlot)) }
        memberIdSlot.captured shouldContainSame setOf("member-1", "member-2-1", "member-2-2", "member-3")

        result shouldSucceed {
            it[conversation1]?.isEmpty() ?: false shouldBeEqualTo true
            it[conversation2].orEmpty() shouldContainSame listOf(contact2_1, contact2_2)
            it[conversation3].orEmpty() shouldContainSame listOf(contact3)
        }
    }

    @Test
    fun `given conversations, when conversationRepo & contactRepo provides member info, then propagates conv-member mapping as success`() {
        val conversation1 = mockk<Conversation>()
        val conversation2 = mockk<Conversation>()
        val conversation3 = mockk<Conversation>()

        coEvery { conversationsRepository.conversationMemberIds(any()) } returnsMany listOf(
            Either.Right(listOf("member-1")),
            Either.Right(listOf("member-2-1", "member-2-2")),
            Either.Right(listOf("member-3"))
        )

        val contact1 = mockContactWithId("member-1")
        val contact2_1 = mockContactWithId("member-2-1")
        val contact2_2 = mockContactWithId("member-2-2")
        val contact3 = mockContactWithId("member-3")
        coEvery { contactRepository.contactsById(any()) } returns Either.Right(listOf(contact1, contact2_1, contact2_2, contact3))

        val params = GetMembersOfConversationsParams(listOf(conversation1, conversation2, conversation3))
        val result = runBlocking { getMembersOfConversationsUseCase.run(params) }

        val memberIdSlot = slot<Set<String>>()
        coVerify(exactly = 1) { contactRepository.contactsById(capture(memberIdSlot)) }
        memberIdSlot.captured shouldContainSame setOf("member-1", "member-2-1", "member-2-2", "member-3")

        result shouldSucceed {
            it[conversation1].orEmpty() shouldContainSame listOf(contact1)
            it[conversation2].orEmpty() shouldContainSame listOf(contact2_1, contact2_2)
            it[conversation3].orEmpty() shouldContainSame listOf(contact3)
        }
    }

    companion object {
        private fun mockContactWithId(id: String) = mockk<Contact>().also {
            every { it.id } returns id
        }
    }
}
