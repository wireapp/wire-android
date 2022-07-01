package com.wire.android.ui.home.conversations.details.participants

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.mapper.testUIParticipant
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GroupConversationParticipantsViewModelTest {

    private fun testSize(givenParticipantsSize: Int, expectedParticipantsSize: Int) = runTest {
        // Given
        val members = buildList {
            for (i in 1..givenParticipantsSize) {
                add(testUIParticipant(i))
            }
        }
        val (_, viewModel) = GroupConversationParticipantsViewModelArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        assert(viewModel.groupParticipantsState.data.participants.size == expectedParticipantsSize)
        assert(viewModel.groupParticipantsState.data.allParticipantsCount == members.size)
    }

    @Test
    fun `given a group members, when solving the participants list, then right sizes are passed`() {
        val maxNumber = GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS
        testSize(givenParticipantsSize = maxNumber + 1, expectedParticipantsSize = maxNumber)
        testSize(givenParticipantsSize = maxNumber - 1, expectedParticipantsSize = maxNumber - 1)
    }
}

internal class GroupConversationParticipantsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    @MockK
    lateinit var navigationManager: NavigationManager
    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase
    private val conversationMembersChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)
    private val viewModel by lazy {
        GroupConversationParticipantsViewModel(savedStateHandle, navigationManager, observeParticipantsForConversationUseCase)
    }

    init {
        // Tests setup
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
        // Default empty values
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
    }

    suspend fun withConversationParticipantsUpdate(participants: List<UIParticipant>): GroupConversationParticipantsViewModelArrangement {
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(
            ConversationParticipantsData(
                participants = participants.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
                allParticipantsCount = participants.size
            )
        )
        return this
    }

    fun arrange() = this to viewModel
}
