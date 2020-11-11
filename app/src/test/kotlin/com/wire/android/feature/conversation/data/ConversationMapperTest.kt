package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ConversationMapperTest : UnitTest() {

    private lateinit var conversationMapper: ConversationMapper

    @Before
    fun setUp() {
        conversationMapper = ConversationMapper()
    }

    @Test
    //TODO update this when we have a full mapper
    fun `given fromConversationsResponse is called with a response model, then returns list of conversations`() {
        val conversationResponse = mockk<ConversationResponse>()
        every { conversationResponse.id } returns TEST_CONVERSATION_ID
        every { conversationResponse.name } returns TEST_CONVERSATION_NAME

        val conversationResponses = listOf(conversationResponse)
        val conversationsResponse = ConversationsResponse(
            hasMore = true,
            conversationResponses = conversationResponses
        )

        conversationMapper.fromConversationsResponse(conversationsResponse).also {
            it.first().id shouldBeEqualTo TEST_CONVERSATION_ID
            it.first().name shouldBeEqualTo TEST_CONVERSATION_NAME
        }
    }

    @Test
    fun `given toEntityList is called with a conversation list, then returns list of entities`() {
        fun mockConversation(name: String): Conversation = mockk<Conversation>(relaxed = false).also {
            every { it.name } returns name
        }

        val name1 = "Conv1"
        val name2 = "Conv2"
        val conversation1 = mockConversation(name1)
        val conversation2 = mockConversation(name2)

        val conversationList = listOf(conversation1, conversation2)

        val entityList = conversationMapper.toEntityList(conversationList)

        entityList[0].name shouldBeEqualTo name1
        entityList[1].name shouldBeEqualTo name2
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "test-id-123"
        private const val TEST_CONVERSATION_NAME = "test-name"
    }
}
