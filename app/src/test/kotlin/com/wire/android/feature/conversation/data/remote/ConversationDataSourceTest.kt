package com.wire.android.feature.conversation.data.remote

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
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
import org.junit.Before
import org.junit.Test

class ConversationDataSourceTest : UnitTest() {

    private lateinit var conversationsDataSource: ConversationsRepository

    @MockK
    private lateinit var conversationRemoteDataSource: ConversationRemoteDataSource

    @MockK
    private lateinit var conversationLocalDataSource: ConversationLocalDataSource

    @MockK
    private lateinit var conversationMapper: ConversationMapper

    @Before
    fun setup() {
        conversationsDataSource = ConversationDataSource(conversationMapper, conversationRemoteDataSource, conversationLocalDataSource)
    }

    @Test
    fun `given conversationsByBatch is requested, when remoteDataSource returns conversations, then saves them locally & returns`() {
        val conversationsResponse: ConversationsResponse = mockk(relaxed = true)
        val conversations = mockk<List<Conversation>>(relaxed = true)
        val conversationEntities = mockk<List<ConversationEntity>>()

        coEvery {
            conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS)
        } returns Either.Right(conversationsResponse)
        every { conversationMapper.fromConversationsResponse(conversationsResponse) } returns conversations
        every { conversationMapper.toEntityList(conversations) } returns conversationEntities
        coEvery { conversationLocalDataSource.saveConversations(conversationEntities) } returns Either.Right(Unit)

        val result = runBlocking { conversationsDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }

        result shouldSucceed { it shouldBe conversations }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }
        verify(exactly = 1) { conversationMapper.fromConversationsResponse(conversationsResponse) }
        verify(exactly = 1) { conversationMapper.toEntityList(conversations) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveConversations(conversationEntities) }
    }

    @Test
    fun `given conversationsByBatch is requested, when remoteSource returns success but localDataSource fails, then propagates failure`() {
        val conversationsResponse: ConversationsResponse = mockk(relaxed = true)
        val conversations = mockk<List<Conversation>>(relaxed = true)
        val conversationEntities = mockk<List<ConversationEntity>>()
        val failure = DatabaseFailure()

        coEvery {
            conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS)
        } returns Either.Right(conversationsResponse)
        every { conversationMapper.fromConversationsResponse(conversationsResponse) } returns conversations
        every { conversationMapper.toEntityList(conversations) } returns conversationEntities
        coEvery { conversationLocalDataSource.saveConversations(conversationEntities) } returns Either.Left(failure)

        val result = runBlocking { conversationsDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }
        verify(exactly = 1) { conversationMapper.fromConversationsResponse(conversationsResponse) }
        verify(exactly = 1) { conversationMapper.toEntityList(conversations) }
        coVerify(exactly = 1) { conversationLocalDataSource.saveConversations(conversationEntities) }    }

    @Test
    fun `given conversationsByBatch is requested, when remoteDataSource returns a failed response, then propagates error upwards`() {
        coEvery {
            conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS)
        } returns Either.Left(ServerError)

        val result = runBlocking { conversationsDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }

        result shouldFail { it shouldBe ServerError }
        coVerify(exactly = 1) { conversationRemoteDataSource.conversationsByBatch(TEST_START, TEST_SIZE, TEST_IDS) }
        verify { conversationMapper wasNot Called }
        verify { conversationLocalDataSource wasNot Called }
    }

    companion object {
        private const val TEST_START = "87dehhe883=jdgegge7730"
        private const val TEST_SIZE = 10
        private val TEST_IDS = emptyList<String>()
    }
}
