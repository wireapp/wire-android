/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
@file:Suppress("MaxLineLength", "MaximumLineLength")

package com.wire.android.ui.home.conversationslist

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationDetails
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.util.orDefault
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
// TODO write more tests
class ConversationListViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given empty search query, when collecting, then update state with all conversations`() = runTest(dispatcherProvider.main()) {
        // Given
        val searchQueryText = ""
        val (arrangement, conversationListViewModel) = Arrangement().arrange()

        // When
        advanceUntilIdle()
        arrangement.searchQueryChanged(searchQueryText)
        advanceUntilIdle()

        // Then
        assertEquals(
            3,
            conversationListViewModel.conversationListState.foldersWithConversations[ConversationFolder.Predefined.Conversations]?.size,
        )
        assertEquals(searchQueryText, conversationListViewModel.conversationListState.searchQuery)
    }

    @Test
    fun `given non-empty search query, when collecting, then update state with filtered conversations`() = runTest(dispatcherProvider.main()) {
        // Given
        val searchQueryText = TestConversationDetails.CONVERSATION_ONE_ONE.conversation.name.orDefault("test")
        val (arrangement, conversationListViewModel) = Arrangement().arrange()

        // When
        advanceUntilIdle()
        arrangement.searchQueryChanged(searchQueryText)
        advanceUntilIdle()

        // Then
        assertEquals(
            1,
            conversationListViewModel.conversationListState.foldersWithConversations[ConversationFolder.Predefined.Conversations]?.size,
        )
        assertEquals(searchQueryText, conversationListViewModel.conversationListState.searchQuery)
    }

    @Test
    fun `given a valid conversation muting state, when calling muteConversation, then should call with call the UseCase`() = runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationListViewModel) = Arrangement()
            .updateConversationMutedStatusSuccess()
            .arrange()

        // When
        conversationListViewModel.muteConversation(conversationId, MutedConversationStatus.AllMuted)

        // Then
        coVerify(exactly = 1) { arrangement.updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any()) }
    }

    @Test
    fun `given a valid conversation muting state, when calling block user, then should call BlockUserUseCase`() = runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationListViewModel) = Arrangement()
            .blockUserSuccess()
            .arrange()

        // When
        conversationListViewModel.blockUser(BlockUserDialogState(userName = "someName", userId = userId)
        )

        // Then
        coVerify(exactly = 1) { arrangement.blockUser(userId) }
    }

    @Test
    fun `given a valid conversation muting state, when calling unblock user, then should call BlockUserUseCase`() = runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationListViewModel) = Arrangement()
            .unblockUserSuccess()
            .arrange()

        // When
        conversationListViewModel.unblockUser(userId)

        // Then
        coVerify(exactly = 1) { arrangement.unblockUser(userId) }
    }

    inner class Arrangement {
        @MockK
        lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

        @MockK
        lateinit var observeConversationListDetailsUseCase: ObserveConversationListDetailsUseCase

        @MockK
        lateinit var leaveConversation: LeaveConversationUseCase

        @MockK
        lateinit var deleteTeamConversationUseCase: DeleteTeamConversationUseCase

        @MockK
        lateinit var blockUser: BlockUserUseCase

        @MockK
        lateinit var unblockUser: UnblockUserUseCase

        @MockK
        lateinit var clearConversationContent: ClearConversationContentUseCase

        @MockK
        private lateinit var wireSessionImageLoader: WireSessionImageLoader

        @MockK
        private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

        @MockK
        private lateinit var refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase

        @MockK
        private lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

        private val searchQueryFlow = MutableStateFlow("")

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeConversationListDetailsUseCase.invoke(false) } returns flowOf(
                listOf(
                    TestConversationDetails.CONNECTION,
                    TestConversationDetails.CONVERSATION_ONE_ONE,
                    TestConversationDetails.GROUP
                )
            )
            mockUri()
        }

        fun searchQueryChanged(searchQuery: String) = apply {
            searchQueryFlow.value = searchQuery
        }

        fun updateConversationMutedStatusSuccess() = apply {
            coEvery {
                updateConversationMutedStatus(any(), any(), any())
            } returns ConversationUpdateStatusResult.Success
        }

        fun blockUserSuccess() = apply {
            coEvery { blockUser(any()) } returns BlockUserResult.Success
        }

        fun unblockUserSuccess() = apply {
            coEvery { unblockUser(any()) } returns UnblockUserResult.Success
        }

        fun arrange() = this to ConversationListViewModelImpl(
            conversationsSource = ConversationsSource.MAIN,
            dispatcher = dispatcherProvider,
            updateConversationMutedStatus = updateConversationMutedStatus,
            observeConversationListDetails = observeConversationListDetailsUseCase,
            leaveConversation = leaveConversation,
            deleteTeamConversation = deleteTeamConversationUseCase,
            blockUserUseCase = blockUser,
            unblockUserUseCase = unblockUser,
            clearConversationContentUseCase = clearConversationContent,
            wireSessionImageLoader = wireSessionImageLoader,
            refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
            refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
            userTypeMapper = UserTypeMapper(),
            updateConversationArchivedStatus = updateConversationArchivedStatus,
            searchQueryFlow = searchQueryFlow
        )
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val userId: UserId = UserId("someUser", "some_domain")
    }
}
