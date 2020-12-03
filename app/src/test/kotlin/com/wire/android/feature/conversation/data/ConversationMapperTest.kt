package com.wire.android.feature.conversation.data

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.remote.ConversationMembersResponse
import com.wire.android.feature.conversation.data.remote.ConversationOtherMembersResponse
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsResponse
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
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
    fun `given a ConvResponse, when fromConversationResponseToConversationMembers is called, then returns list of entities & member ids`() {

        val conversation1 = mockConversationResponseWithMembers("conv-1", "member-1-1", "member-1-2", "member-1-3")
        val conversation2 = mockConversationResponseWithMembers("conv-2", "member-2-1", "")
        val conversation3 = mockConversationResponseWithMembers("conv-3")
        val conversation4 = mockConversationResponseWithMembers("conv-4", "member-4-1")

        val conversationsResponse = mockk<ConversationsResponse>().also {
            every { it.conversations } returns listOf(conversation1, conversation2, conversation3, conversation4)
        }

        val conversationMemberEntities =
            conversationMapper.fromConversationResponseToConversationMembers(conversationsResponse)

        conversationMemberEntities shouldContainSame listOf(
            ConversationMemberEntity("conv-1", "member-1-1"),
            ConversationMemberEntity("conv-1", "member-1-2"),
            ConversationMemberEntity("conv-1", "member-1-3"),
            ConversationMemberEntity("conv-2", "member-2-1"),
            ConversationMemberEntity("conv-4", "member-4-1")
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

        private fun mockConversationResponseWithMembers(conversationId: String, vararg memberIds: String): ConversationResponse =
            mockk<ConversationResponse>().also {
                every { it.id } returns conversationId

                val membersResponse = mockk<ConversationMembersResponse>()
                every { it.members } returns membersResponse

                val otherMembers = memberIds.map { memberId ->
                    mockk<ConversationOtherMembersResponse>().also { every { it.userId } returns memberId }
                }
                every { membersResponse.otherMembers } returns otherMembers
            }
    }
}
