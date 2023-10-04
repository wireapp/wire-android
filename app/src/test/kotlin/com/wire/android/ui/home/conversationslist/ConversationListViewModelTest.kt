/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversationslist

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.HomeSnackbarState
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
// TODO write more tests
class ConversationListViewModelTest {

    private lateinit var conversationListViewModel: ConversationListViewModel

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var observeConversationListDetailsUseCase: ObserveConversationListDetailsUseCase

    @MockK
    lateinit var leaveConversation: LeaveConversationUseCase

    @MockK
    lateinit var deleteTeamConversationUseCase: DeleteTeamConversationUseCase

    @MockK
    lateinit var joinCall: AnswerCallUseCase

    @MockK
    lateinit var blockUser: BlockUserUseCase

    @MockK
    lateinit var unblockUser: UnblockUserUseCase

    @MockK
    lateinit var clearConversationContent: ClearConversationContentUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

    @MockK
    private lateinit var refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase

    @MockK
    private lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

    @MockK(relaxed = true)
    private lateinit var onJoined: (ConversationId) -> Unit

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
        coEvery { observeConversationListDetailsUseCase.invoke(any()) } returns emptyFlow()

        mockUri()
        conversationListViewModel =
            ConversationListViewModel(
                dispatcher = TestDispatcherProvider(),
                updateConversationMutedStatus = updateConversationMutedStatus,
                answerCall = joinCall,
                observeConversationListDetails = observeConversationListDetailsUseCase,
                leaveConversation = leaveConversation,
                deleteTeamConversation = deleteTeamConversationUseCase,
                blockUserUseCase = blockUser,
                unblockUserUseCase = unblockUser,
                clearConversationContentUseCase = clearConversationContent,
                wireSessionImageLoader = wireSessionImageLoader,
                endCall = endCall,
                observeEstablishedCalls = observeEstablishedCalls,
                refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
                refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
                userTypeMapper = UserTypeMapper(),
                updateConversationArchivedStatus = updateConversationArchivedStatus
            )
    }

    @Test
    fun `given a valid conversation muting state, when calling muteConversation, then should call with call the UseCase`() = runTest {
        coEvery { updateConversationMutedStatus(any(), any(), any()) } returns ConversationUpdateStatusResult.Success
        conversationListViewModel.muteConversation(conversationId, MutedConversationStatus.AllMuted)

        coVerify(exactly = 1) { updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any()) }
    }

    @Test
    fun `given a conversation id, when joining an ongoing call, then verify that answer call usecase is called`() = runTest {
        coEvery { joinCall(any()) } returns Unit

        conversationListViewModel.joinOngoingCall(conversationId = conversationId, onJoined = onJoined)

        coVerify(exactly = 1) { joinCall(conversationId = conversationId) }
        verify(exactly = 1) { onJoined(conversationId) }
    }

    @Test
    fun `given a valid conversation muting state, when calling block user, then should call BlockUserUseCase`() = runTest {
        coEvery { blockUser(any()) } returns BlockUserResult.Success
        conversationListViewModel.blockUser(
            BlockUserDialogState(
                userName = "someName",
                userId = userId
            )
        )

        coVerify(exactly = 1) { blockUser(userId) }
    }

    @Test
    fun `given a valid conversation muting state, when calling unblock user, then should call BlockUserUseCase`() = runTest {
        coEvery { unblockUser(any()) } returns UnblockUserResult.Success
        conversationListViewModel.unblockUser(userId)

        coVerify(exactly = 1) { unblockUser(userId) }
    }

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() {
        conversationListViewModel.conversationListState = conversationListViewModel.conversationListState.copy(
            shouldShowJoinAnywayDialog = true
        )

        conversationListViewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, conversationListViewModel.conversationListState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() {
        conversationListViewModel.conversationListState = conversationListViewModel.conversationListState.copy(hasEstablishedCall = false)

        coEvery { joinCall(conversationId = any()) } returns Unit

        conversationListViewModel.joinOngoingCall(conversationId, onJoined)

        coVerify(exactly = 1) { joinCall(conversationId = any()) }
        coVerify(exactly = 1) { onJoined(any()) }
        assertEquals(false, conversationListViewModel.conversationListState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() {
        conversationListViewModel.conversationListState = conversationListViewModel.conversationListState.copy(hasEstablishedCall = true)

        conversationListViewModel.joinOngoingCall(conversationId, onJoined)

        assertEquals(true, conversationListViewModel.conversationListState.shouldShowJoinAnywayDialog)
        coVerify(inverse = true) { joinCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() {
        conversationListViewModel.conversationListState = conversationListViewModel.conversationListState.copy(hasEstablishedCall = true)
        conversationListViewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { endCall(any()) } returns Unit

        conversationListViewModel.joinAnyway(conversationId, onJoined)

        coVerify(exactly = 1) { endCall(any()) }
    }

    @Test
    fun `given permission dialog default state is false, when calling showPermissionDialog, then update the state to true`() = runTest {
        conversationListViewModel.conversationListState =
            conversationListViewModel.conversationListState.copy(shouldShowCallingPermissionDialog = false)

        conversationListViewModel.showCallingPermissionDialog()

        assertEquals(true, conversationListViewModel.conversationListState.shouldShowCallingPermissionDialog)
    }

    @Test
    fun `given default permission dialog state, when calling dismissPermissionDialog, then update the state to false`() = runTest {
        conversationListViewModel.conversationListState =
            conversationListViewModel.conversationListState.copy(shouldShowCallingPermissionDialog = true)

        conversationListViewModel.dismissCallingPermissionDialog()

        assertEquals(false, conversationListViewModel.conversationListState.shouldShowCallingPermissionDialog)
    }

    @Test
    fun `given a valid conversation state, when archiving it correctly, then the right success message is shown`() = runTest {
        val isArchiving = true
        val dialogState = DialogState(
            conversationItem.conversationId,
            conversationItem.conversationInfo.name,
            ConversationTypeDetail.Private(null, conversationItem.userId, BlockingState.NOT_BLOCKED),
            !isArchiving
        )
        val archivingTimestamp = 123456789L

        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns ArchiveStatusUpdateResult.Success

        conversationListViewModel.homeSnackBarState.test {
            conversationListViewModel.moveConversationToArchive(dialogState, archivingTimestamp)
            expectMostRecentItem() shouldBeEqualTo HomeSnackbarState.UpdateArchivingStatusSuccess(isArchiving = isArchiving)
        }
        coVerify(exactly = 1) {
            updateConversationArchivedStatus.invoke(
                dialogState.conversationId,
                !dialogState.isArchived,
                archivingTimestamp
            )
        }
    }

    @Test
    fun `given a valid conversation state, when un-archiving it with an error, then the right failure message is shown`() = runTest {
        val isArchiving = false
        val dialogState = DialogState(
            conversationItem.conversationId,
            conversationItem.conversationInfo.name,
            ConversationTypeDetail.Private(null, conversationItem.userId, BlockingState.NOT_BLOCKED),
            !isArchiving
        )
        val archivingTimestamp = 123456789L

        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns ArchiveStatusUpdateResult.Failure

        conversationListViewModel.homeSnackBarState.test {
            conversationListViewModel.moveConversationToArchive(dialogState, archivingTimestamp)
            expectMostRecentItem() shouldBeEqualTo HomeSnackbarState.UpdateArchivingStatusError(isArchiving = isArchiving)
        }
        coVerify(exactly = 1) {
            updateConversationArchivedStatus.invoke(
                dialogState.conversationId,
                !dialogState.isArchived,
                archivingTimestamp
            )
        }
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val userId: UserId = UserId("someUser", "some_domain")

        private val conversationItem = ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(),
            conversationInfo = ConversationInfo(
                name = "Some dummy name",
                membership = Membership.None
            ),
            conversationId = conversationId,
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = false,
            lastMessageContent = UILastMessageContent.None,
            badgeEventType = BadgeEventType.None,
            userId = userId,
            blockingState = BlockingState.CAN_NOT_BE_BLOCKED,
            teamId = null,
            isArchived = false
        )
    }
}
