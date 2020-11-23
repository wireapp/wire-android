package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class ConversationMapperTest : UnitTest() {

    private lateinit var conversationMapper: ConversationMapper

    @Before
    fun setUp() {
        conversationMapper = ConversationMapper()
    }

    @Test
    fun `given a ConversationsResponse, when fromConversationResponseToEntityList is called, then returns a list of entities`() {
        val conversationsResponse = mockk<ConversationsResponse>()

        val id1 = "$TEST_CONVERSATION_ID-1"
        val name1 = "$TEST_CONVERSATION_NAME-1"
        val conversationResponse1 = mockk<ConversationResponse>().also {
            every { it.id } returns id1
            every { it.name } returns name1
        }

        val id2 = "$TEST_CONVERSATION_ID-2"
        val conversationResponse2 = mockk<ConversationResponse>().also {
            every { it.id } returns id2
            every { it.name } returns null
        }

        every { conversationsResponse.conversations } returns listOf(conversationResponse1, conversationResponse2)

        val entityList = conversationMapper.fromConversationResponseToEntityList(conversationsResponse)

        entityList shouldContainSame listOf(
            ConversationEntity(id = id1, name = name1),
            ConversationEntity(id = id2, name = String.EMPTY)
        )
    }

    @Test
    fun `given a conversation entity, when fromEntity is called, then returns a conversation`() {
        val conversationEntity = ConversationEntity(id = TEST_CONVERSATION_ID, name = TEST_CONVERSATION_NAME)

        val conversation = conversationMapper.fromEntity(conversationEntity)

        conversation shouldBeEqualTo Conversation(id = TEST_CONVERSATION_ID, name = TEST_CONVERSATION_NAME)
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "test-id-123"
        private const val TEST_CONVERSATION_NAME = "test-name"
    }
}
