package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
    }
}
