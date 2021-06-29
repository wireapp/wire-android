package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.Group
import com.wire.android.feature.conversation.OneToOne
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class RefineConversationNamesUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationListRepository: ConversationListRepository

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    private lateinit var refineConversationNamesUseCase: RefineConversationNamesUseCase

    @Before
    fun setUp() {
        refineConversationNamesUseCase = RefineConversationNamesUseCase(conversationListRepository, conversationRepository)
    }

    @Test
    fun `given run is called, when there are no conversations in conversationRepository, then directly propagates success`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(0)

        val result = runBlocking { refineConversationNamesUseCase.run(Unit) }

        result shouldSucceed {}
        coVerify(exactly = 1) { conversationRepository.numberOfConversations() }
        coVerify { conversationListRepository wasNot Called }
    }

    @Test
    fun `given run is called, when conversationRepository fails to check number of conversations, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationRepository.numberOfConversations() } returns Either.Left(failure)

        val result = runBlocking { refineConversationNamesUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationRepository.numberOfConversations() }
        coVerify { conversationListRepository wasNot Called }
    }

    @Test
    fun `given run is called, when there are conversations in conversationRepo, then queries conversationListRepo in batches of 20`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(50)
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returnsMany listOf(
            Either.Right(emptyList()), Either.Right(emptyList()), Either.Right(emptyList())
        )

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        coVerify(exactly = 3) { conversationListRepository.conversationListInBatch(any(), EXPECTED_BATCH_SIZE) }
    }

    @Test
    fun `given run is called, when a conversation is 1-1, then updates conversation name as member's name`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(1)
        val conversation = Conversation(id = "id", name = "corrupt name", type = OneToOne)
        val member = mockk<Contact>()
        every { member.name } returns TEST_MEMBER_NAME_1

        val conversationListItem = mockConversationListItem(conversation, member)
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returns Either.Right(listOf(conversationListItem))

        coEvery { conversationRepository.updateConversations(any()) } returns Either.Left(mockk())

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        val updatedConversationsSlot = slot<List<Conversation>>()
        coVerify(exactly = 1) { conversationRepository.updateConversations(capture(updatedConversationsSlot)) }
        updatedConversationsSlot.captured.first().name shouldBeEqualTo TEST_MEMBER_NAME_1
    }

    @Test
    fun `given run is called and the conversation is not 1-1, when conversation name is not empty, then does not update name`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(1)
        val conversation = Conversation(id = "id", name = "This conversation already has a name", type = Group)
        val conversationListItem = mockConversationListItem(conversation, mockk())
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returns Either.Right(listOf(conversationListItem))

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        coVerify(inverse = true) { conversationRepository.updateConversations(any()) }
    }

    @Test
    fun `given run is called and the conversation is not 1-1, when conversation name is empty, then updates conv name as members' names`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(1)
        val conversation = Conversation(id = "id", name = "", type = Group)
        val member1 = mockk<Contact>()
        every { member1.name } returns TEST_MEMBER_NAME_1
        val member2 = mockk<Contact>()
        every { member2.name } returns TEST_MEMBER_NAME_2

        val conversationListItem = mockConversationListItem(conversation, member1, member2)
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returns Either.Right(listOf(conversationListItem))

        coEvery { conversationRepository.updateConversations(any()) } returns Either.Left(mockk())

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        val updatedConversationsSlot = slot<List<Conversation>>()
        coVerify(exactly = 1) { conversationRepository.updateConversations(capture(updatedConversationsSlot)) }
        updatedConversationsSlot.captured.first().name shouldBeEqualTo "$TEST_MEMBER_NAME_1, $TEST_MEMBER_NAME_2"
    }

    @Test
    fun `given run is called, when a conversation has no members, then does not update name`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(1)
        val conversation = Conversation(id = "id", name = "Name", type = Group)
        val conversationListItem = mockConversationListItem(conversation)
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returns Either.Right(listOf(conversationListItem))

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        coVerify(inverse = true) { conversationRepository.updateConversations(any()) }
    }

    @Test
    fun `given run is called, when a conversationRepo fails to update conversation names, then propagates failure`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(1)
        val conversation = Conversation(id = "id", name = "corrupt name", type = OneToOne)
        val member = mockk<Contact>()
        every { member.name } returns TEST_MEMBER_NAME_1
        val conversationListItem = mockConversationListItem(conversation, member)
        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returns Either.Right(listOf(conversationListItem))

        val failure = mockk<Failure>()
        coEvery { conversationRepository.updateConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { refineConversationNamesUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationRepository.updateConversations(any()) }
    }

    @Test
    fun `given run is called, when a conversationRepo updates conversation names successfully, then proceeds to query next batch`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(EXPECTED_BATCH_SIZE * 2)

        val conversation = Conversation(id = "id", name = "corrupt name", type = OneToOne)
        val member = mockk<Contact>()
        every { member.name } returns TEST_MEMBER_NAME_1
        val conversationListItem = mockConversationListItem(conversation, member)

        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returnsMany listOf(
            Either.Right(listOf(conversationListItem)), Either.Left(mockk())
        )

        coEvery { conversationRepository.updateConversations(any()) } returns Either.Right(Unit)

        runBlocking { refineConversationNamesUseCase.run(Unit) }

        coVerify(exactly = 1) { conversationRepository.updateConversations(any()) }
        coVerify(exactly = 1) { conversationListRepository.conversationListInBatch(0, EXPECTED_BATCH_SIZE) }
        coVerify(exactly = 1) { conversationListRepository.conversationListInBatch(EXPECTED_BATCH_SIZE, EXPECTED_BATCH_SIZE) }
    }

    @Test
    fun `given run is called, when all batches are queried, then propagates success`() {
        coEvery { conversationRepository.numberOfConversations() } returns Either.Right(30)

        val conversation1 = Conversation(id = "id", name = "corrupt name", type = OneToOne)
        val member1 = mockk<Contact>()
        every { member1.name } returns TEST_MEMBER_NAME_1
        val conversationListItem1 = mockConversationListItem(conversation1, member1)

        val conversation2 = Conversation(id = "id_2", name = "", type = Group)
        val member2 = mockk<Contact>()
        every { member2.name } returns TEST_MEMBER_NAME_2
        val conversationListItem2 = mockConversationListItem(conversation2, member2)

        coEvery { conversationListRepository.conversationListInBatch(any(), any<Int>()) } returnsMany listOf(
            Either.Right(listOf(conversationListItem1)), Either.Right(listOf(conversationListItem2))
        )
        coEvery { conversationRepository.updateConversations(any()) } returnsMany listOf(
            Either.Right(Unit), Either.Right(Unit)
        )

        val result = runBlocking { refineConversationNamesUseCase.run(Unit) }

        result shouldSucceed { }
        coVerify(exactly = 2) { conversationRepository.updateConversations(any()) }
        coVerify(exactly = 2) { conversationListRepository.conversationListInBatch(any(), EXPECTED_BATCH_SIZE) }
    }

    companion object {
        private const val EXPECTED_BATCH_SIZE = 20
        private const val TEST_MEMBER_NAME_1 = "Alice Aaa"
        private const val TEST_MEMBER_NAME_2 = "Bob Bbb"

        fun mockConversationListItem(conversation: Conversation, vararg members: Contact): ConversationListItem =
            mockk<ConversationListItem>().also {
                every { it.conversation } returns conversation
                every { it.members } returns members.asList()
            }
    }
}
