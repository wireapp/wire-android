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

package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.mapper.testUIParticipant
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsNavArgs
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateReceiptModeResult
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
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

        val details = testGroup.copy(conversation = testGroup.conversation.copy(name = "group name"))
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
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

        val details1 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val details2 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 2"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details1)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details1.conversation.name, viewModel.groupOptionsState.value.groupName)
        // When - Then
        arrangement.withConversationDetailUpdate(details2)
        assertEquals(details2.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given some conversation details, when archiving that conversation, then the use case is invoked`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val archivingEventTimestamp = 123456789L
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val conversationDetails = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        // When
        viewModel.onMoveConversationToArchive(
            conversationId = viewModel.conversationId,
            shouldArchive = true,
            timestamp = archivingEventTimestamp
        ) {}

        // Then
        coVerify(exactly = 1) {
            arrangement.updateConversationArchivedStatus(
                conversationId = viewModel.conversationId,
                shouldArchiveConversation = true,
                archivedStatusTimestamp = archivingEventTimestamp
            )
        }
    }

    @Test
    fun `given some conversation details, when un-archiving that conversation, then the use case is invoked`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val archivingEventTimestamp = 123456789L
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val conversationDetails = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        // When
        viewModel.onMoveConversationToArchive(viewModel.conversationId, false, archivingEventTimestamp) {}

        // Then
        coVerify(exactly = 1) {
            arrangement.updateConversationArchivedStatus(
                conversationId = viewModel.conversationId,
                shouldArchiveConversation = false,
                archivedStatusTimestamp = archivingEventTimestamp
            )
        }
    }

    @Test
    fun `given a group conversation, when solving the state, then the state is correct`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size,
            isSelfAnAdmin = true
        )

        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                name = "group name",
                teamId = TeamId("team_id"),
                accessRole = listOf(
                    Conversation.AccessRole.GUEST,
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.NON_TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE
                )
            )
        )
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(conversationParticipantsData.isSelfAnAdmin, viewModel.groupOptionsState.value.isUpdatingAllowed)
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(details.conversation.isTeamGroup(), viewModel.groupOptionsState.value.areAccessOptionsAvailable)
        assertEquals(
            (details.conversation.isGuestAllowed() || details.conversation.isNonTeamMemberAllowed()),
            viewModel.groupOptionsState.value.isGuestAllowed
        )
        assertEquals(
            conversationParticipantsData.isSelfAnAdmin && details.conversation.teamId?.value == selfTeam.id,
            viewModel.groupOptionsState.value.isUpdatingGuestAllowed
        )
    }

    @Test
    fun `when disabling Services , then the dialog must state must be updated`() = runTest {
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

        val details = testGroup

        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(false)
        assertEquals(true, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
    }

    @Test
    fun `when no guests and enabling services, use case is called with the correct values`() = runTest {
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

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(true)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                accessRoles = Conversation
                    .defaultGroupAccessRoles
                    .toMutableSet()
                    .apply {
                        add(Conversation.AccessRole.SERVICE)
                        remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    },
                access = Conversation.defaultGroupAccess
            )
        }
    }

    @Test
    fun `when no guests and disable service dialog confirmed, then use case is called with the correct values`() = runTest {
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

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServiceDialogConfirm()
        assertEquals(false, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                accessRoles = Conversation.defaultGroupAccessRoles.toMutableSet().apply { remove(Conversation.AccessRole.NON_TEAM_MEMBER) },
                access = Conversation.defaultGroupAccess
            )
        }
    }

    @Test
    fun `given a group conversation, when self is admin and in owner team, then should be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(true, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, when self is admin and not in owner team, then should not be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(false, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, then conversationSheetContent is valid`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        val expected = ConversationSheetContent(
            title = details.conversation.name.orEmpty(),
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, details.isSelfUserCreator),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup()
        )
        // When - Then
        assertEquals(expected, viewModel.conversationSheetContent)
    }

    @Test
    fun `given receipt mode value enabled, when updating receipt mode, then value is propagated to screen state`() = runTest {
        // given
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationReceiptModeReturningSuccess()
            .arrange()
        val receiptModeEnabled = true

        // when
        viewModel.onReadReceiptUpdate(enableReadReceipt = receiptModeEnabled)

        // then
        assertEquals(receiptModeEnabled, viewModel.groupOptionsState.value.isReadReceiptAllowed)
        coVerify(exactly = 1) {
            arrangement.updateConversationReceiptMode(
                conversationId = any(),
                receiptMode = Conversation.ReceiptMode.ENABLED
            )
        }
    }

    @Test
    fun `given receipt mode value disabled, when updating receipt mode, then value is propagated to screen state`() = runTest {
        // given
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationReceiptModeReturningSuccess()
            .arrange()
        val receiptModeEnabled = false

        // when
        viewModel.onReadReceiptUpdate(enableReadReceipt = receiptModeEnabled)

        // then
        assertEquals(receiptModeEnabled, viewModel.groupOptionsState.value.isReadReceiptAllowed)
        coVerify(exactly = 1) {
            arrangement.updateConversationReceiptMode(
                conversationId = any(),
                receiptMode = Conversation.ReceiptMode.DISABLED
            )
        }
    }

    @Test
    fun `given user has no teamId and conversation no teamId, when init group options, then read receipt toggle is disabled`() = runTest {
        // given
        // when
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = null))
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationReceiptModeReturningSuccess()
            .withConversationDetailUpdate(details)
            .withSelfTeamUseCaseReturns(result = null)
            .arrange()

        // then
        assertEquals(false, viewModel.groupOptionsState.value.isUpdatingReadReceiptAllowed)
    }

    @Test
    fun `given user has no teamId, is admin and conversation has teamId, when init group options, then read receipt toggle is enabled`() =
        runTest {
            // given
            val members = buildList {
                for (i in 1..5) {
                    add(testUIParticipant(i))
                }
            }
            val conversationParticipantsData = ConversationParticipantsData(
                participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
                allParticipantsCount = members.size,
                isSelfAnAdmin = true
            )
            val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

            // when
            val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
                .withUpdateConversationReceiptModeReturningSuccess()
                .withConversationDetailUpdate(details)
                .withConversationMembersUpdate(conversationParticipantsData)
                .withSelfTeamUseCaseReturns(result = null)
                .arrange()

            // then
            assertEquals(true, viewModel.groupOptionsState.value.isUpdatingReadReceiptAllowed)
        }

    @Test
    fun `given user has no teamId, not admin and conversation has teamId, when init group options, then read receipt toggle is enabled`() =
        runTest {
            // given
            val members = buildList {
                for (i in 1..5) {
                    add(testUIParticipant(i))
                }
            }
            val conversationParticipantsData = ConversationParticipantsData(
                participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
                allParticipantsCount = members.size,
                isSelfAnAdmin = true
            )
            val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

            // when
            val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
                .withUpdateConversationReceiptModeReturningSuccess()
                .withConversationDetailUpdate(details)
                .withConversationMembersUpdate(conversationParticipantsData)
                .withSelfTeamUseCaseReturns(result = null)
                .arrange()

            // then
            assertEquals(true, viewModel.groupOptionsState.value.isUpdatingReadReceiptAllowed)
        }

    @Test
    fun `given user has teamId, is admin and conversation teamId, when init group options, then read receipt toggle is enabled`() =
        runTest {
            // given
            val members = buildList {
                for (i in 1..5) {
                    add(testUIParticipant(i))
                }
            }
            val conversationParticipantsData = ConversationParticipantsData(
                participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
                allParticipantsCount = members.size,
                isSelfAnAdmin = true
            )
            val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
            val selfTeam = Team("team_id", "team_name", "icon")

            // when
            val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
                .withUpdateConversationReceiptModeReturningSuccess()
                .withConversationDetailUpdate(details)
                .withConversationMembersUpdate(conversationParticipantsData)
                .withSelfTeamUseCaseReturns(result = selfTeam)
                .arrange()

            // then
            assertEquals(true, viewModel.groupOptionsState.value.isUpdatingReadReceiptAllowed)
        }

    companion object {
        val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val testGroup = ConversationDetails.Group(
            Conversation(
                id = dummyConversationId,
                name = "Conv Name",
                type = Conversation.Type.ONE_ON_ONE,
                teamId = TeamId("team_id"),
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = MutedConversationStatus.AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply { add(Conversation.AccessRole.GUEST) },
                lastReadDate = "2022-04-04T16:11:28.388Z",
                creatorId = null,
                receiptMode = Conversation.ReceiptMode.ENABLED,
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null
            ),
            legalHoldStatus = LegalHoldStatus.DISABLED,
            hasOngoingCall = false,
            lastMessage = null,
            isSelfUserCreator = false,
            isSelfUserMember = true,
            unreadEventCount = emptyMap(),
            selfRole = Conversation.Member.Role.Member
        )
    }
}

internal class GroupConversationDetailsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var deleteTeamConversation: DeleteTeamConversationUseCase

    @MockK
    lateinit var removeMemberFromConversation: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observerSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase

    @MockK
    lateinit var getSelfTeamUseCase: GetSelfTeamUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var clearConversationContentUseCase: ClearConversationContentUseCase

    @MockK
    lateinit var isMLSEnabledUseCase: IsMLSEnabledUseCase

    @MockK
    lateinit var updateConversationReceiptMode: UpdateConversationReceiptModeUseCase

    @MockK
    lateinit var observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase

    @MockK
    lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    private val observeParticipantsForConversationChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)

    @MockK
    private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            dispatcher = TestDispatcherProvider(),
            observerSelfUser = observerSelfUser,
            observeConversationDetails = observeConversationDetails,
            deleteTeamConversation = deleteTeamConversation,
            removeMemberFromConversation = removeMemberFromConversation,
            observeConversationMembers = observeParticipantsForConversationUseCase,
            updateConversationAccessRole = updateConversationAccessRoleUseCase,
            getSelfTeam = getSelfTeamUseCase,
            savedStateHandle = savedStateHandle,
            updateConversationMutedStatus = updateConversationMutedStatus,
            clearConversationContent = clearConversationContentUseCase,
            updateConversationReceiptMode = updateConversationReceiptMode,
            isMLSEnabled = isMLSEnabledUseCase,
            observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
            refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
            updateConversationArchivedStatus = updateConversationArchivedStatus
        )
    }

    val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {

        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { savedStateHandle.navArgs<GroupConversationAllParticipantsNavArgs>() } returns GroupConversationAllParticipantsNavArgs(
            conversationId = conversationId
        )
        every { savedStateHandle.navArgs<GroupConversationDetailsNavArgs>() } returns GroupConversationDetailsNavArgs(
            conversationId = conversationId
        )

        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observerSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
        coEvery { getSelfTeamUseCase() } returns flowOf(null)
        coEvery { isMLSEnabledUseCase() } returns true
        coEvery { updateConversationMutedStatus(any(), any(), any()) } returns ConversationUpdateStatusResult.Success
        coEvery { observeSelfDeletionTimerSettingsForConversation(any(), any()) } returns flowOf(SelfDeletionTimer.Disabled)
        coEvery { updateConversationArchivedStatus(any(), any()) } returns ArchiveStatusUpdateResult.Success
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
            .map { ObserveConversationDetailsUseCase.Result.Success(it) }
        conversationDetailsChannel.send(conversationDetails)
    }

    suspend fun withConversationMembersUpdate(conversationParticipantsData: ConversationParticipantsData) = apply {
        coEvery { observeParticipantsForConversationUseCase(any()) } returns observeParticipantsForConversationChannel.consumeAsFlow()
        observeParticipantsForConversationChannel.send(conversationParticipantsData)
    }

    suspend fun withUpdateConversationAccessUseCaseReturns(result: UpdateConversationAccessRoleUseCase.Result) = apply {
        coEvery { updateConversationAccessRoleUseCase(any(), any(), any()) } returns result
    }

    suspend fun withSelfTeamUseCaseReturns(result: Team?) = apply {
        coEvery { getSelfTeamUseCase() } returns flowOf(result)
    }

    suspend fun withUpdateConversationReceiptModeReturningSuccess() = apply {
        coEvery { updateConversationReceiptMode(any(), any()) } returns ConversationUpdateReceiptModeResult.Success
    }

    suspend fun withUpdateArchivedStatus(result: ArchiveStatusUpdateResult) = apply {
        coEvery { updateConversationArchivedStatus(any(), any()) } returns result
        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns result
    }

    fun arrange() = this to viewModel
}
