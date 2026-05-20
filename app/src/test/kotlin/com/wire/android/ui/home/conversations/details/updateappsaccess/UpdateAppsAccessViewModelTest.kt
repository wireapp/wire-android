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

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `given apps are enabled for current conversation protocol and conversation has SERVICE role, then state should reflect this to allow`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val conversation = TestConversation.GROUP().copy(
            accessRole = listOf(Conversation.AccessRole.SERVICE),
            protocol = Conversation.ProtocolInfo.MLS(
                groupId = GroupID("group-id"),
                groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
                epoch = 1UL,
                keyingMaterialLastUpdate = Instant.parse("2022-04-04T16:11:28.388Z"),
                cipherSuite = CipherSuite.MLS_128_DHKEMP256_AES128GCM_SHA256_P256
            )
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
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
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
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
            .withAppsAllowedResult(AppsAllowedResult.Disabled)
            .arrange()

        advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
        assertEquals(false, viewModel.updateAppsAccessState.isAppAccessAllowed)
    }

    @Test
    fun `given mixed team falls back to Proteus, when conversation is Proteus with SERVICE role, then bots access is enabled`() = runTest {
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = listOf(Conversation.AccessRole.SERVICE),
                protocol = Conversation.ProtocolInfo.Proteus
            )
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
            .arrange()

        advanceUntilIdle()

        assertEquals(true, viewModel.updateAppsAccessState.isAppAccessAllowed)
        assertEquals(true, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `given mixed team falls back to Proteus, when conversation is MLS, then apps access is disabled`() = runTest {
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = listOf(Conversation.AccessRole.SERVICE),
                protocol = Conversation.ProtocolInfo.MLS(
                    groupId = GroupID("group-id"),
                    groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
                    epoch = 1UL,
                    keyingMaterialLastUpdate = Instant.parse("2022-04-04T16:11:28.388Z"),
                    cipherSuite = CipherSuite.MLS_128_DHKEMP256_AES128GCM_SHA256_P256
                )
            )
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
            .arrange()

        advanceUntilIdle()

        assertEquals(false, viewModel.updateAppsAccessState.isAppAccessAllowed)
        assertEquals(false, viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `when onAppsDialogDismiss is called, then dialog is hidden and state is reverted`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = listOf(Conversation.AccessRole.SERVICE, Conversation.AccessRole.TEAM_MEMBER),
                protocol = Conversation.ProtocolInfo.MLS(
                    groupId = GroupID("group-id"),
                    groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
                    epoch = 1UL,
                    keyingMaterialLastUpdate = Instant.parse("2022-04-04T16:11:28.388Z"),
                    cipherSuite = CipherSuite.MLS_128_DHKEMP256_AES128GCM_SHA256_P256
                )
            )
        )

        val (_, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
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
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var changeAccessForAppsInConversationUseCase: ChangeAccessForAppsInConversationUseCase

    @MockK
    lateinit var observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase

    @MockK
    lateinit var observeSelfUser: ObserveSelfUserUseCase

    val conversationId = ConversationId("some-dummy-value", "dummyDomain")

    private var navArgs = UpdateAppsAccessNavArgs(
        conversationId = conversationId,
        updateAppsAccessParams = UpdateAppsAccessParams(
            isGuestAllowed = true,
            isAppsAllowed = true,
            shouldUseNewAppsUi = true
        )
    )

    private val conversationDetailsFlow = MutableSharedFlow<ConversationDetails>(replay = Int.MAX_VALUE)

    private val observeParticipantsForConversationFlow =
        MutableSharedFlow<ConversationParticipantsData>(replay = Int.MAX_VALUE)

    private val viewModel by lazy {
        UpdateAppsAccessViewModel(
            updateAppsAccessNavArgs = navArgs,
            dispatcher = TestDispatcherProvider(),
            observeConversationDetails = observeConversationDetails,
            observeConversationMembers = observeParticipantsForConversationUseCase,
            changeAccessForAppsInConversation = changeAccessForAppsInConversationUseCase,
            observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
            selfUser = observeSelfUser,
        )
    }

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observeParticipantsForConversationUseCase(any()) } returns flowOf()
        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        withAppsAllowedResult(AppsAllowedResult.Disabled)
    }

    fun withGuestDisabledNavArgs() = apply {
        navArgs = UpdateAppsAccessNavArgs(
            conversationId = conversationId,
            updateAppsAccessParams = UpdateAppsAccessParams(
                isGuestAllowed = false,
                isAppsAllowed = true,
                shouldUseNewAppsUi = true
            )
        )
    }

    fun withAppsAllowedResult(result: AppsAllowedResult) = apply {
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
