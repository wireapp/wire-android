package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.core.exception.CacheFailure
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationResponse
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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
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
    fun `given fetchConversations is called, when remote data source cannot fetch next batch of conversations, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations() }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(any(), any()) }
        verify { conversationLocalDataSource wasNot Called }
        verify { conversationMapper wasNot Called }
    }

    @Test
    fun `given fetchConversations is called, when remote data source fetches next batch of conversations, then saves them locally`() {
        val response = mockk<ConversationsResponse>()
        val remoteConvList = mockk<List<ConversationResponse>>()
        every { response.conversations } returns remoteConvList
        every { response.hasMore } returns false

        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Right(response)
        val entities = mockk<List<ConversationEntity>>()
        every { conversationMapper.fromConversationResponseListToEntityList(remoteConvList) } returns entities
        coEvery { conversationLocalDataSource.saveConversations(entities) } returns Either.Left(DatabaseFailure())

        runBlocking { conversationDataSource.fetchConversations() }

        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(any(), any()) }
        verify(exactly = 1) { conversationMapper.fromConversationResponseListToEntityList(remoteConvList) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveConversations(entities) }
    }

    @Test
    fun `given fetchConversations is called, when local data source fails to save conversations, then propagates failure`() {
        val response = mockk<ConversationsResponse>()
        every { response.conversations } returns mockk()
        every { response.hasMore } returns false
        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Right(response)
        every { conversationMapper.fromConversationResponseListToEntityList(any()) } returns mockk()
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations() }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchConversations is called, when local data source saves conversations, then proceeds to save conversation members`() {
        val response = mockk<ConversationsResponse>()
        val remoteConvList = mockk<List<ConversationResponse>>()
        every { response.conversations } returns remoteConvList
        every { response.hasMore } returns false

        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Right(response)
        every { conversationMapper.fromConversationResponseListToEntityList(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)

        val conversationMemberEntities = mockk<List<ConversationMemberEntity>>()
        every { conversationMapper.fromConversationResponseListToConversationMembers(any()) } returns conversationMemberEntities
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) } returns Either.Left(mockk())

        runBlocking { conversationDataSource.fetchConversations() }

        verify(exactly = 1) { conversationMapper.fromConversationResponseListToConversationMembers(remoteConvList) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) }
    }

    @Test
    fun `given fetchConversations is called, when local data source fails to save members, then propagates the failure`() {
        val response = mockk<ConversationsResponse>()
        every { response.conversations } returns mockk()
        every { response.hasMore } returns false
        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Right(response)
        every { conversationMapper.fromConversationResponseListToEntityList(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)
        every { conversationMapper.fromConversationResponseListToConversationMembers(any()) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.fetchConversations() }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given fetchConversations is called and local data source saved members, when there aren't more pages, then propagates success`() {
        val response = mockk<ConversationsResponse>()
        every { response.conversations } returns mockk()
        every { response.hasMore } returns false
        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returns Either.Right(response)

        every { conversationMapper.fromConversationResponseListToEntityList(any()) } returns mockk()

        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)
        every { conversationMapper.fromConversationResponseListToConversationMembers(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(any()) } returns Either.Right(Unit)

        every { conversationMapper.fromEntity(any()) } returns mockk()

        val result = runBlocking { conversationDataSource.fetchConversations() }

        result shouldSucceed { it shouldBe Unit }
    }

    @Test
    fun `given fetchConversations is called and local data source saved members, when there are more pages, then proceeds to fetch them`() {
        val currentPageResponse = mockk<ConversationsResponse>()
        val lastConversation = mockk<ConversationResponse>()
        every { lastConversation.id } returns TEST_CONVERSATION_ID
        every { currentPageResponse.conversations } returns listOf(lastConversation)
        every { currentPageResponse.hasMore } returns true

        coEvery { conversationRemoteDataSource.conversationsByBatch(any(), any()) } returnsMany
            listOf(Either.Right(currentPageResponse), Either.Left(mockk()))

        every { conversationMapper.fromConversationResponseListToEntityList(any()) } returns mockk()

        coEvery { conversationLocalDataSource.saveConversations(any()) } returns Either.Right(Unit)
        every { conversationMapper.fromConversationResponseListToConversationMembers(any()) } returns mockk()
        coEvery { conversationLocalDataSource.saveMemberIdsForConversations(any()) } returns Either.Right(Unit)

        every { conversationMapper.fromEntity(any()) } returns mockk()

        runBlocking { conversationDataSource.fetchConversations() }

        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(null, any()) }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_CONVERSATION_ID, any()) }
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

    @Test
    fun `given allConversationMemberIds is called, when localDataSource returns member ids, then propagates result`() {
        val memberIds = mockk<List<String>>()
        coEvery { conversationLocalDataSource.allConversationMemberIds() } returns Either.Right(memberIds)

        val result = runBlocking { conversationDataSource.allConversationMemberIds() }

        result shouldSucceed { it shouldBeEqualTo memberIds }
    }

    @Test
    fun `given allConversationMemberIds is called, when localDataSource fails to return member ids, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.allConversationMemberIds() } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.allConversationMemberIds() }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given updateConversations is called, when localDataSource updates successfully, then propagates success`() {
        val conversations = mockk<List<Conversation>>()
        every { conversationMapper.toEntityList(conversations) } returns mockk()
        coEvery { conversationLocalDataSource.updateConversations(any()) } returns Either.Right(Unit)

        val result = runBlocking { conversationDataSource.updateConversations(conversations) }

        result shouldSucceed { }
    }

    @Test
    fun `given updateConversations is called, when localDataSource fails to update, then propagates failure`() {
        val conversations = mockk<List<Conversation>>()
        every { conversationMapper.toEntityList(conversations) } returns mockk()

        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.updateConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.updateConversations(conversations) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given numberOfConversations is called, when localDataSource returns the count, then propagates the count`() {
        val count = 250
        coEvery { conversationLocalDataSource.numberOfConversations() } returns Either.Right(count)

        val result = runBlocking { conversationDataSource.numberOfConversations() }

        result shouldSucceed { it shouldBeEqualTo count }
    }

    @Test
    fun `given numberOfConversations is called, when localDataSource fails to return the count, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.numberOfConversations() } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.numberOfConversations() }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given currentOpenedConversationId is called, when localDataSource is successful, then returns the current conversation id`() {
        coEvery { conversationLocalDataSource.currentOpenedConversationId() } returns Either.Right(TEST_CONVERSATION_ID)

        val result = runBlocking { conversationDataSource.currentOpenedConversationId() }

        result shouldSucceed { it shouldBeEqualTo TEST_CONVERSATION_ID }
    }

    @Test
    fun `given currentOpenedConversationId is called, when localDataSource returns failure, then returns failure`() {
        val failure = CacheFailure
        coEvery { conversationLocalDataSource.currentOpenedConversationId() } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.currentOpenedConversationId() }

        result shouldFail  { it shouldBeEqualTo failure }
    }

    @Test
    fun `given updateCurrentConversationId is called, when localDataSource updates successfully, then propagates success`() {
        coEvery { conversationLocalDataSource.updateConversations(any()) } returns Either.Right(Unit)

        val result = runBlocking { conversationDataSource.updateCurrentConversationId(TEST_CONVERSATION_ID) }

        result shouldSucceed { }
    }

    @Test
    fun `given updateCurrentConversationId is called, when localDataSource fails to update, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationLocalDataSource.updateConversations(any()) } returns Either.Left(failure)

        val result = runBlocking { conversationDataSource.updateCurrentConversationId(TEST_CONVERSATION_ID) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conv-id"
    }
}
