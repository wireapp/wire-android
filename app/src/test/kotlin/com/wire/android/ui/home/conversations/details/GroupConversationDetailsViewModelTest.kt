package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
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
        val details = mockConversationDetailsGroup("Group name")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.groupName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val details1 = mockConversationDetailsGroup("Group name 1")
        val details2 = mockConversationDetailsGroup("Group name 2")
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details1)
            .arrange()

        // When - Then
        assertEquals(details1.conversation.name, viewModel.groupOptionsState.groupName)
        // When - Then
        arrangement.withConversationDetailUpdate(details2)
        assertEquals(details2.conversation.name, viewModel.groupOptionsState.groupName)
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
    private lateinit var wireSessionImageLoader: WireSessionImageLoader
    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)
    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            savedStateHandle,
            navigationManager,
            observeConversationDetails,
            observeParticipantsForConversationUseCase,
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

    fun arrange() = this to viewModel
}
