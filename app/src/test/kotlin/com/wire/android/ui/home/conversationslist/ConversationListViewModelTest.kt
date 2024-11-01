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

import androidx.paging.PagingData
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestConversationItem
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
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
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
// TODO write more tests
class ConversationListViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Disabled
    @Test
    fun `given initial empty search query, when collecting conversations, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN).arrange()

            // When
//            conversationListViewModel.conversationListState.foldersWithConversations.test {
//                // Then
//                coVerify(exactly = 1) {
//                    arrangement.getConversationsPaginated("", false, true, false)
//                }
//                cancelAndIgnoreRemainingEvents()
//            }
        }

    @Disabled
    @Test
    fun `given updated non-empty search query, when collecting conversations, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
//            val searchQueryText = "search"
//            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN).arrange()

            // When
//        conversationListViewModel.conversationListState.foldersWithConversations.test {
//            conversationListViewModel.searchQueryChanged(searchQueryText)
//            advanceUntilIdle()
//
//            // Then
//            coVerify(exactly = 1) {
//                arrangement.getConversationsPaginated(searchQueryText, false, true, false)
//            }
//            cancelAndIgnoreRemainingEvents()
//        }
        }

    @Test
    @Disabled
    fun `given updated non-empty search query, when collecting archived, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
//        val searchQueryText = "search"
//        val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.ARCHIVE).arrange()

            // When
//        conversationListViewModel.conversationListState.foldersWithConversations.test {
//            conversationListViewModel.searchQueryChanged(searchQueryText)
//            advanceUntilIdle()
//
//            // Then
//            coVerify(exactly = 1) {
//                arrangement.getConversationsPaginated(searchQueryText, true, false, false)
//            }
//            cancelAndIgnoreRemainingEvents()
//        }
        }

    @Test
    fun `given a valid conversation muting state, when calling muteConversation, then should call with call the UseCase`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, conversationListViewModel) = Arrangement()
                .updateConversationMutedStatusSuccess()
                .arrange()

            // When
            conversationListViewModel.muteConversation(conversationId, MutedConversationStatus.AllMuted)

            // Then
            coVerify(exactly = 1) {
                arrangement.updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any())
            }
        }

    @Test
    fun `given a valid conversation muting state, when calling block user, then should call BlockUserUseCase`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, conversationListViewModel) = Arrangement()
                .blockUserSuccess()
                .arrange()

            // When
            conversationListViewModel.blockUser(BlockUserDialogState(userName = "someName", userId = userId))

            // Then
            coVerify(exactly = 1) { arrangement.blockUser(userId) }
        }

    @Test
    fun `given a valid conversation muting state, when calling unblock user, then should call BlockUserUseCase`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, conversationListViewModel) = Arrangement()
                .unblockUserSuccess()
                .arrange()

            // When
            conversationListViewModel.unblockUser(userId)

            // Then
            coVerify(exactly = 1) { arrangement.unblockUser(userId) }
        }

    inner class Arrangement(val conversationsSource: ConversationsSource = ConversationsSource.MAIN) {
        @MockK
        lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

        @MockK
        lateinit var getConversationsPaginated: GetConversationsFromSearchUseCase

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
        private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

        @MockK
        private lateinit var refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase

        @MockK
        private lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

        @MockK
        private lateinit var observeConversationListDetailsWithEventsUseCase:
                ObserveConversationListDetailsWithEventsUseCase

        @MockK
        private lateinit var wireSessionImageLoader: WireSessionImageLoader

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery {
                getConversationsPaginated.invoke(any(), any(), any(), any())
            } returns flowOf(
                PagingData.from(listOf(TestConversationItem.CONNECTION, TestConversationItem.PRIVATE, TestConversationItem.GROUP))
            )
            coEvery { observeConversationListDetailsWithEventsUseCase.invoke(false) } returns flowOf(
                listOf(
                    TestConversationDetails.CONNECTION,
                    TestConversationDetails.CONVERSATION_ONE_ONE,
                    TestConversationDetails.GROUP
                ).map {
                    ConversationDetailsWithEvents(
                        conversationDetails = it
                    )
                }
            )
            mockUri()
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
            conversationsSource = conversationsSource,
            dispatcher = dispatcherProvider,
            updateConversationMutedStatus = updateConversationMutedStatus,
            getConversationsPaginated = getConversationsPaginated,
            leaveConversation = leaveConversation,
            deleteTeamConversation = deleteTeamConversationUseCase,
            blockUserUseCase = blockUser,
            unblockUserUseCase = unblockUser,
            clearConversationContentUseCase = clearConversationContent,
            refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
            refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
            updateConversationArchivedStatus = updateConversationArchivedStatus,
            observeConversationListDetailsWithEvents = observeConversationListDetailsWithEventsUseCase,
            userTypeMapper = UserTypeMapper(),
            wireSessionImageLoader = wireSessionImageLoader
        )
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val userId: UserId = UserId("someUser", "some_domain")
    }
}
