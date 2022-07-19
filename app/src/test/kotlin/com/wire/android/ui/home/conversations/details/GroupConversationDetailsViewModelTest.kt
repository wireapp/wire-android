package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.testUIParticipant
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.ProtocolInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GroupConversationDetailsViewModelTest {

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup.copy( conversation = testGroup.conversation.copy(name = "group name"))
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.groupName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details1 = testGroup.copy( conversation = testGroup.conversation.copy(name = "Group name 1"))
        val details2 = testGroup.copy( conversation = testGroup.conversation.copy(name = "Group name 2"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details1)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details1.conversation.name, viewModel.groupOptionsState.groupName)
        // When - Then
        arrangement.withConversationDetailUpdate(details2)
        assertEquals(details2.conversation.name, viewModel.groupOptionsState.groupName)
    }

    companion object {
        val testGroup = ConversationDetails.Group(
            Conversation(
                ConversationId("conv_id", "domain"),
                "Conv Name",
                Conversation.Type.ONE_ON_ONE,
                TeamId("team_id"),
                ProtocolInfo.Proteus,
                MutedConversationStatus.AllAllowed,
                null,
                null,
                access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
                accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST)
            ),
            legalHoldStatus = LegalHoldStatus.DISABLED
        )
    }
}

internal class GroupConversationDetailsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    private lateinit var updateConversationAccessUseCase: UpdateConversationAccessRoleUseCase

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)
    private val observeParticipantsForConversationChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            savedStateHandle,
            navigationManager,
            observeConversationDetails,
            observeParticipantsForConversationUseCase,
            updateConversationAccessUseCase,
            dispatcher = TestDispatcherProvider()
        )
    }

    init {
        // Tests setup
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails): GroupConversationDetailsViewModelArrangement {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
        conversationDetailsChannel.send(conversationDetails)
        return this
    }


    suspend fun withConversationMembersUpdate(conversationParticipantsData: ConversationParticipantsData): GroupConversationDetailsViewModelArrangement {
        coEvery { observeParticipantsForConversationUseCase(any()) } returns observeParticipantsForConversationChannel.consumeAsFlow()
        observeParticipantsForConversationChannel.send(conversationParticipantsData)
        return this
    }

    fun arrange() = this to viewModel
}
