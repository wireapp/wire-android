package com.wire.android.ui.home.conversations.details.participants.usecase

import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.testOtherUser
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveParticipantsForConversationUseCaseTest {

    @Test
    fun `given a group members, when solving the participants list with limit, then limited sizes are passed`() = runTest {
        // Given
        val limit = 4
        val members = buildList {
            for (i in 1..(limit + 1)) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserType.INTERNAL), Member.Role.Member))
            }
        }
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        useCase(ConversationId("", ""), limit).test {
            val data = awaitItem()
            assert(data.participants.size == limit)
            assert(data.allParticipantsCount == members.size)
        }
    }

    @Test
    fun `given a group members, when solving the participants list without limit, then all lists are passed`() = runTest {
        // Given
        val members: List<MemberDetails> = buildList {
            for (i in 1..20) {
                add(MemberDetails(testOtherUser(i).copy(userType = UserType.INTERNAL), Member.Role.Member))
            }
        }
        val (_, useCase) = ObserveParticipantsForConversationUseCaseArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        useCase(ConversationId("", "")).test {
            val data = awaitItem()
            assert(data.participants.size == members.size)
            assert(data.allParticipantsCount == members.size)
        }
    }
}

internal class ObserveParticipantsForConversationUseCaseArrangement {

    @MockK
    lateinit var observeConversationMembersUseCase: ObserveConversationMembersUseCase
    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader
    private val uIParticipantMapper by lazy { UIParticipantMapper(UserTypeMapper(), wireSessionImageLoader) }
    private val conversationMembersChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)
    private val useCase by lazy {
        ObserveParticipantsForConversationUseCase(
            observeConversationMembersUseCase,
            uIParticipantMapper,
            dispatchers = TestDispatcherProvider()
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        // Default empty values
        coEvery { observeConversationMembersUseCase(any()) } returns flowOf()
    }

    suspend fun withConversationParticipantsUpdate(members: List<MemberDetails>): ObserveParticipantsForConversationUseCaseArrangement {
        coEvery { observeConversationMembersUseCase(any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(members)
        return this
    }

    fun arrange() = this to useCase
}
