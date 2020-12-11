package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class ConversationDataSourceTest : UnitTest() {

    @MockK
    private lateinit var conversationMapper: ConversationMapper

    @MockK
    private lateinit var conversationRemoteDataSource: ConversationsRemoteDataSource

    @MockK
    private lateinit var conversationLocalDataSource: ConversationLocalDataSource

    private lateinit var conversationDataSource: ConversationDataSource

    @Before
    fun setUp() {
        conversationDataSource = ConversationDataSource(conversationMapper, conversationRemoteDataSource, conversationLocalDataSource)
    }

    @Test
    fun `given fetchConversations is called, when start parameter is null, then calls remote data source with null start id`() {
        val failure = mockk<Failure>()
        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), TEST_BATCH_SIZE) } returns Either.Left(failure)

        runBlocking { conversationDataSource.fetchConversations(null, TEST_BATCH_SIZE) }

        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(null, TEST_BATCH_SIZE) }
    }

    @Test
    fun `given fetchConversations is called, when remote data source cannot fetch next batch of conversations, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }
        verify { conversationLocalDataSource wasNot Called }
        verify { conversationMapper wasNot Called }
    }

    @Test
    fun `given fetchConversations is called, when remote data source fetches next batch of conversations, then saves them locally`() {
        val response = mockk<ConversationsResponse>()
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Right(response)
        val entities = mockk<List<ConversationEntity>>()
        every { conversationMapper.fromConversationResponseToEntityList(response) } returns entities
        coEvery { conversationLocalDataSource.saveConversations(entities) } returns Either.Left(DatabaseFailure())

        runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }
        verify(exactly = 1) { conversationMapper.fromConversationResponseToEntityList(response) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveConversations(entities) }
    }

    @Test
    fun `given fetchConversations is called, when local data source fails to save conversations, then propagates failure`() {
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Right(mockk())
        every { conversationMapper.fromConversationResponseToEntityList(any()) } returns mockk()
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchConversations is called, when local data source saves conversations, then proceeds to save conversation members`() {
        val response = mockk<ConversationsResponse>()
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Right(response)
        every { conversationMapper.fromConversationResponseToEntityList(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)

        val conversationMemberEntities = mockk<List<ConversationMemberEntity>>()
        every { conversationMapper.fromConversationResponseToConversationMembers(response) } returns conversationMemberEntities
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) } returns
            Either.Left(DatabaseFailure())

        runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        verify(exactly = 1) { conversationMapper.fromConversationResponseToConversationMembers(response) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) }
    }

    @Test
    fun `given fetchConversations is called, when local data source fails to save members, then propagates the failure`() {
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Right(mockk())
        every { conversationMapper.fromConversationResponseToEntityList(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)
        every { conversationMapper.fromConversationResponseToConversationMembers(any()) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchConversations is called, when local data source saves members, then propagates conversations as success`() {
        coEvery { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) } returns Either.Right(mockk())

        val entity1 = mockk<ConversationEntity>()
        val entity2 = mockk<ConversationEntity>()
        every { conversationMapper.fromConversationResponseToEntityList(any()) } returns listOf(entity1, entity2)

        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)
        every { conversationMapper.fromConversationResponseToConversationMembers(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(any()) } returns Either.Right(Unit)

        val conversation1 = mockk<Conversation>()
        val conversation2 = mockk<Conversation>()
        every { conversationMapper.fromEntity(entity1) } returns conversation1
        every { conversationMapper.fromEntity(entity2) } returns conversation2

        val result = runBlocking { conversationDataSource.fetchConversations(TEST_CONVERSATION_ID, TEST_BATCH_SIZE) }

        result shouldSucceed  { it shouldContainSame listOf(conversation1, conversation2) }
    }

    @Test
    fun `given conversationMemberIds is called, when localDataSource returns success with member ids, then propagates result`() {
        val conversation = mockk<Conversation>()
        every { conversation.id } returns TEST_CONVERSATION_ID
        val memberIds = mockk<List<String>>()
        coEvery { conversationLocalDataSource.conversationMemberIds(TEST_CONVERSATION_ID) } returns Either.Right(memberIds)

        val result = runBlocking { conversationDataSource.conversationMemberIds(conversation) }

        result shouldSucceed { it shouldBeEqualTo memberIds }
    }

    @Test
    fun `given conversationMemberIds is called, when localDataSource fails to get member ids, then propagates failure`() {
        val conversation = mockk<Conversation>()
        every { conversation.id } returns TEST_CONVERSATION_ID
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.conversationMemberIds(TEST_CONVERSATION_ID) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.conversationMemberIds(conversation) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conv-id"
        private const val TEST_BATCH_SIZE = 10
    }
}
