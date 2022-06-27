package com.wire.android.ui.home.conversations.details

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.mapper.testOtherUser
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun `given a group members, when solving the participants list, then right sizes are passed`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS + 1)) {
                add(MemberDetails.Other(testOtherUser(i), UserType.INTERNAL))
            }
        }
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        assert(viewModel.groupParticipantsState.participants.size <= GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS)
        assert(viewModel.groupParticipantsState.allParticipantsCount  == members.size)
    }
}
