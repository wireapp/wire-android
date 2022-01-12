package com.wire.android.feature.conversation.data

import android.annotation.SuppressLint
import com.wire.android.UnitTest
import com.wire.android.feature.conversation.*
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.data.remote.*
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class ConversationMapperTest : UnitTest() {

    @MockK
    private lateinit var conversationTypeMapper: ConversationTypeMapper

    private lateinit var conversationMapper: ConversationMapper

    @Before
    fun setUp() {
        conversationMapper = ConversationMapper(conversationTypeMapper)
    }

    @Test
    fun `given a list of ConversationResponse, when fromConversationResponseListToEntityList is called, then returns a list of entities`() {
        val id1 = ConversationIdResponse(
            value = "$TEST_CONVERSATION_ID-1",
            domain = TEST_CONVERSATION_DOMAIN
        )
        val name1 = "$TEST_CONVERSATION_NAME-1"
        val type1 = 1
        val conversationResponse1 = mockk<ConversationResponse>().also {
            every { it.id } returns id1
            every { it.name } returns name1
            every { it.type } returns type1
        }

        val id2 = ConversationIdResponse(
            value = "$TEST_CONVERSATION_ID-2",
            domain = TEST_CONVERSATION_DOMAIN
        )
        val type2 = 2
        val conversationResponse2 = mockk<ConversationResponse>().also {
            every { it.id } returns id2
            every { it.name } returns null
            every { it.type } returns type2
        }

        val conversationResponseList = listOf(conversationResponse1, conversationResponse2)

        val entityList =
            conversationMapper.fromConversationResponseListToEntityList(conversationResponseList)

        entityList shouldContainSame listOf(
            ConversationEntity(id = id1.value, domain = id1.domain, name = name1, type = type1),
            ConversationEntity(id = id2.value, domain = id2.domain, name = null, type = type2)
        )
    }

    @Test
    fun `given a list of ConvResponse, when mapping conversation response, then returns list of members including self user ID`() {
        val convIdResponse1 = ConversationIdResponse(value = "conv-1", domain = "domain-1")
        val convIdResponse2 = ConversationIdResponse(value = "conv-2", domain = "domain-1")
        val convIdResponse3 = ConversationIdResponse(value = "conv-3", domain = "domain-1")
        val convIdResponse4 = ConversationIdResponse(value = "conv-4", domain = "domain-1")

        val conversation1 =
            mockConversationResponseWithMembers(
                conversationIdResponse = convIdResponse1,
                "member-1-1", "member-1-2", "member-1-3"
            )
        val conversation2 = mockConversationResponseWithMembers(
            conversationIdResponse = convIdResponse2,
            "member-2-1", ""
        )
        val conversation3 =
            mockConversationResponseWithMembers(conversationIdResponse = convIdResponse3)
        val conversation4 = mockConversationResponseWithMembers(
            conversationIdResponse = convIdResponse4,
            "member-4-1"
        )

        val conversationResponseList =
            listOf(conversation1, conversation2, conversation3, conversation4)

        val conversationMemberEntities =
            conversationMapper.fromConversationResponseListToConversationMembers(
                conversationResponseList
            )

        conversationMemberEntities shouldContainSame listOf(
            ConversationMemberEntity("conv-1", "domain-1", "member-1-1"),
            ConversationMemberEntity("conv-1", "domain-1", "member-1-2"),
            ConversationMemberEntity("conv-1", "domain-1", "member-1-3"),
            ConversationMemberEntity("conv-2", "domain-1", "member-2-1"),
            ConversationMemberEntity("conv-4", "domain-1", "member-4-1"),
            ConversationMemberEntity("conv-1", "domain-1", SELF_USER_ID),
            ConversationMemberEntity("conv-2", "domain-1", SELF_USER_ID),
            ConversationMemberEntity("conv-3", "domain-1", SELF_USER_ID),
            ConversationMemberEntity("conv-4", "domain-1", SELF_USER_ID)
        )
    }

    @Test
    fun `given a conversation entity, when fromEntity is called, then returns a conversation`() {
        val conversationEntity =
            ConversationEntity(
                id = TEST_CONVERSATION_ID,
                domain = TEST_CONVERSATION_DOMAIN,
                name = TEST_CONVERSATION_NAME,
                type = 1
            )
        val type = mockk<ConversationType>()
        every { conversationTypeMapper.fromIntValue(any()) } returns type

        val conversation = conversationMapper.fromEntity(conversationEntity)
        val convID = ConversationID(value = TEST_CONVERSATION_ID, domain = TEST_CONVERSATION_DOMAIN)

        conversation shouldBeEqualTo Conversation(
            id = convID,
            name = TEST_CONVERSATION_NAME,
            type = type
        )
    }

    @Test
    fun `given a list of conversations, when ToEntityList is called, then returns a list of entities`() {
        val id1 =
            ConversationID(value = "$TEST_CONVERSATION_ID-1", domain = TEST_CONVERSATION_DOMAIN)
        val name1 = "$TEST_CONVERSATION_NAME-1"
        val groupTypeInt = 0
        val conversation1 = mockk<Conversation>().also {
            every { it.id } returns id1
            every { it.name } returns name1
            every { it.type } returns Group
        }
        every { conversationTypeMapper.toIntValue(Group) } returns groupTypeInt

        val id2 =
            ConversationID(value = "$TEST_CONVERSATION_ID-2", domain = TEST_CONVERSATION_DOMAIN)
        val oneToOneTypeInt = 2
        val conversation2 = mockk<Conversation>().also {
            every { it.id } returns id2
            every { it.name } returns null
            every { it.type } returns OneToOne
        }
        every { conversationTypeMapper.toIntValue(OneToOne) } returns oneToOneTypeInt

        val conversationList = listOf(conversation1, conversation2)

        val entityList = conversationMapper.toEntityList(conversationList)

        entityList shouldContainSame listOf(
            ConversationEntity(
                id = id1.value,
                domain = id1.domain,
                name = name1,
                type = groupTypeInt
            ),
            ConversationEntity(
                id = id2.value,
                domain = id2.domain,
                name = null,
                type = oneToOneTypeInt
            )
        )
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "test-id-123"
        private const val TEST_CONVERSATION_DOMAIN = "test-domain-123"
        private const val TEST_CONVERSATION_NAME = "test-name"
        private const val SELF_USER_ID = "self_user_id"

        private fun mockConversationResponseWithMembers(
            conversationIdResponse: ConversationIdResponse,
            vararg memberIds: String
        ): ConversationResponse =
            mockk<ConversationResponse>().also {
                every { it.id } returns conversationIdResponse


                val membersResponse = mockk<ConversationMembersResponse>()
                every { it.members } returns membersResponse

                val otherMembers = memberIds.map { memberId ->
                    mockk<ConversationOtherMembersResponse>().also { every { it.userId } returns memberId }
                }
                every { membersResponse.otherMembers } returns otherMembers

                val selfResponse = mockk<ConversationSelfMemberResponse>()
                every { selfResponse.userId } returns SELF_USER_ID

                every { membersResponse.self } returns selfResponse
            }
    }
}
