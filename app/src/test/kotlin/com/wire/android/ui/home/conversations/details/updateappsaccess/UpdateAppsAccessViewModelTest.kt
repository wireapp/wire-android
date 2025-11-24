/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.updateappsaccess

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class UpdateAppsAccessViewModelTest {

    @Test
    fun `when disabling apps access, then the dialog state must be updated`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            )
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        // When
        viewModel.onAppsAccessUpdate(false)

        // Then
        assertEquals(true, viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
    }

    @Test
    fun `when no guests allowed and enabling apps access, use case is called with the correct values`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    remove(Conversation.AccessRole.GUEST)
                },
                access = listOf()
            )
        )

        val (arrangement, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withGuestDisabledNavArgs()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            )
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        // When
        viewModel.onAppsAccessUpdate(true)

        // Then
        coVerify(exactly = 1) {
            arrangement.changeAccessForAppsInConversationUseCase(
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
    fun `when no guests allowed and disable apps access dialog confirmed, then use case is called with the correct values`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    remove(Conversation.AccessRole.GUEST)
                },
                access = listOf()
            )
        )

        val (arrangement, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withGuestDisabledNavArgs()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            )
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        // When
        viewModel.onServiceDialogConfirm()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        coVerify(exactly = 1) {
            arrangement.changeAccessForAppsInConversationUseCase(
                conversationId = details.conversation.id,
                accessRoles = Conversation.defaultGroupAccessRoles.toMutableSet().apply {
                    remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                },
                access = Conversation.defaultGroupAccess
            )
        }
    }

    @Test
    fun `given user is admin, when observing conversation, then apps access update is allowed`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `given user is not admin, when observing conversation, then apps access update is not allowed`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = false)
        val details = testGroup

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `given isAppsAllowed for team and conversation has SERVICE role, then state should reflect this to allow`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val conversation = TestConversation.GROUP().copy(
            accessRole = listOf(Conversation.AccessRole.SERVICE)
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withAppsAllowedResult(true)
            .withConversationDetailUpdate(TestConversationDetails.GROUP.copy(conversation = conversation))
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(true, viewModel.updateAppsAccessState.isAppAccessAllowed)
    }

    @Test
    fun `given isAppsAllowed for team but no SERVICE role for conversation, then state should reflect this to not allow`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val conversation = TestConversation.GROUP().copy(
            accessRole = listOf(Conversation.AccessRole.GUEST)
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withAppsAllowedResult(true)
            .withConversationDetailUpdate(TestConversationDetails.GROUP.copy(conversation = conversation))
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.isAppAccessAllowed)
    }

    @Test
    fun `given team does not allow apps, when observing conversation, then apps access update is not allowed`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(false)
            .arrange()

        advanceUntilIdle()

        // Then
        // WPB-21835: even if team does not allow apps, we still allow enabling/disabling apps based on protocol, later this will change
        assertEquals(true, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
        assertEquals(true, viewModel.updateAppsAccessState.isAppAccessAllowed)
    }

    @Test
    fun `when onAppsDialogDismiss is called, then dialog is hidden and state is reverted`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = listOf(Conversation.AccessRole.SERVICE, Conversation.AccessRole.TEAM_MEMBER)
            )
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(true)
            .arrange()

        advanceUntilIdle()
        val initialServicesAllowed = viewModel.updateAppsAccessState.isAppAccessAllowed

        viewModel.onAppsAccessUpdate(false) // Trigger disable, which shows dialog

        // When
        viewModel.onAppsDialogDismiss()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        assertEquals(initialServicesAllowed, viewModel.updateAppsAccessState.isAppAccessAllowed)
        assertEquals(false, viewModel.updateAppsAccessState.isLoadingAppsOption)
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "dummyDomain")
        val testGroup = ConversationDetails.Group.Regular(
            Conversation(
                id = conversationId,
                name = "Conv Name",
                type = Conversation.Type.OneOnOne,
                teamId = TeamId("team_id"),
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = MutedConversationStatus.AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    add(Conversation.AccessRole.GUEST)
                    add(Conversation.AccessRole.SERVICE)
                },
                lastReadDate = Instant.parse("2022-04-04T16:11:28.388Z"),
                creatorId = null,
                receiptMode = Conversation.ReceiptMode.ENABLED,
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null,
                mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                legalHoldStatus = Conversation.LegalHoldStatus.ENABLED
            ),
            hasOngoingCall = false,
            isSelfUserMember = true,
            selfRole = Conversation.Member.Role.Member,
            wireCell = null,
        )
    }
}

internal class UpdateAppsAccessViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var changeAccessForAppsInConversationUseCase: ChangeAccessForAppsInConversationUseCase

    @MockK
    lateinit var observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase

    @MockK
    lateinit var observeSelfUser: ObserveSelfUserUseCase

    private val conversationDetailsFlow = MutableSharedFlow<ConversationDetails>(replay = Int.MAX_VALUE)

    private val observeParticipantsForConversationFlow =
        MutableSharedFlow<ConversationParticipantsData>(replay = Int.MAX_VALUE)

    private val viewModel by lazy {
        UpdateAppsAccessViewModel(
            dispatcher = TestDispatcherProvider(),
            observeConversationDetails = observeConversationDetails,
            observeConversationMembers = observeParticipantsForConversationUseCase,
            changeAccessForAppsInConversation = changeAccessForAppsInConversationUseCase,
            observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
            selfUser = observeSelfUser,
            savedStateHandle = savedStateHandle
        )
    }

    val conversationId = ConversationId("some-dummy-value", "dummyDomain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { savedStateHandle.navArgs<UpdateAppsAccessNavArgs>() } returns UpdateAppsAccessNavArgs(
            conversationId = conversationId,
            updateAppsAccessParams = UpdateAppsAccessParams(
                isGuestAllowed = true,
                isAppsAllowed = true
            )
        )

        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observeParticipantsForConversationUseCase(any()) } returns flowOf()
        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        withAppsAllowedResult(false)
    }

    fun withGuestDisabledNavArgs() = apply {
        every { savedStateHandle.navArgs<UpdateAppsAccessNavArgs>() } returns UpdateAppsAccessNavArgs(
            conversationId = conversationId,
            updateAppsAccessParams = UpdateAppsAccessParams(
                isGuestAllowed = false,
                isAppsAllowed = true
            )
        )
    }

    fun withAppsAllowedResult(result: Boolean) = apply {
        coEvery { observeIsAppsAllowedForUsage() } returns flowOf(result)
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsFlow
            .map { ObserveConversationDetailsUseCase.Result.Success(it) }
        conversationDetailsFlow.emit(conversationDetails)
    }

    suspend fun withConversationMembersUpdate(conversationParticipantsData: ConversationParticipantsData) = apply {
        coEvery { observeParticipantsForConversationUseCase(any()) } returns observeParticipantsForConversationFlow
        observeParticipantsForConversationFlow.emit(conversationParticipantsData)
    }

    suspend fun withUpdateConversationAccessUseCaseReturns(result: UpdateConversationAccessRoleUseCase.Result) = apply {
        coEvery { changeAccessForAppsInConversationUseCase(any(), any(), any()) } returns result
    }

    fun arrange() = this to viewModel
}
