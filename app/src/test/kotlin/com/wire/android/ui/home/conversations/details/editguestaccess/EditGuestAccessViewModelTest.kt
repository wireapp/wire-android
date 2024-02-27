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

@file:Suppress("MaximumLineLength")

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversationDetails
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class EditGuestAccessViewModelTest {

    @Test
    fun `given updateConversationAccessRole use case runs successfully, when trying to enable guest access, then enable guest access`() =
        runTest {
            // given
            val (arrangement, editGuestAccessViewModel) = Arrangement()
                .withUpdateConversationAccessRoleResult(UpdateConversationAccessRoleUseCase.Result.Success)
                .arrange()
            advanceUntilIdle()

            // when
            editGuestAccessViewModel.updateGuestAccess(true)

            // then
            coVerify(exactly = 1) { arrangement.updateConversationAccessRole(any(), any(), any()) }
            assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
        }

    @Test
    fun `given a failure when running updateConversationAccessRole, when trying to enable guest access, then do not enable guest access`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withUpdateConversationAccessRoleResult(
                UpdateConversationAccessRoleUseCase.Result.Failure(CoreFailure.MissingClientRegistration)
            ).arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.updateGuestAccess(true)

        // then
        coVerify(exactly = 1) { arrangement.updateConversationAccessRole(any(), any(), any()) }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
    }

    @Test
    fun `given guest access is activated, when trying to disable guest access, then display dialog before disabling guest access`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withUpdateConversationAccessRoleResult(UpdateConversationAccessRoleUseCase.Result.Success)
            .arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.updateGuestAccess(false)

        // then
        coVerify(inverse = true) { arrangement.updateConversationAccessRoleUseCase(any(), any(), any()) }
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.shouldShowGuestAccessChangeConfirmationDialog)
    }

    @Test
    fun `given useCase runs with success, when_generating guest link, then invoke it once`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withGenerateGuestRoomResult(GenerateGuestRoomLinkResult.Success)
            .arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.onRequestGuestRoomLink()

        // then
        coVerify(exactly = 1) { arrangement.generateGuestRoomLink.invoke(any(), null) }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGeneratingGuestRoomLink)
    }

    @Test
    fun `given useCase runs with failure, when generating guest link, then show dialog error`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withGenerateGuestRoomResult(
                GenerateGuestRoomLinkResult.Failure(NetworkFailure.NoNetworkConnection(RuntimeException("no network")))
            ).arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.onRequestGuestRoomLink()

        // then
        coVerify(exactly = 1) { arrangement.generateGuestRoomLink.invoke(any(), null) }
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToGenerateGuestRoomLink)
    }

    @Test
    fun `given useCase runs with success, when revoking guest link, then invoke it once`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withRevokeGuestRoomLinkResult(RevokeGuestRoomLinkResult.Success)
            .arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.removeGuestLink()

        // then
        coVerify(exactly = 1) { arrangement.revokeGuestRoomLink(any()) }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
    }

    @Test
    fun `given useCase runs with failure when revoking guest link then show dialog error`() = runTest {
        // given
        val (arrangement, editGuestAccessViewModel) = Arrangement()
            .withRevokeGuestRoomLinkResult(RevokeGuestRoomLinkResult.Failure(CoreFailure.MissingClientRegistration))
            .arrange()
        advanceUntilIdle()

        // when
        editGuestAccessViewModel.removeGuestLink()

        // then
        coVerify(exactly = 1) { arrangement.revokeGuestRoomLink(any()) }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToRevokeGuestRoomLink)
    }

    @Test
    fun `given updateConversationAccessRole use case runs successfully, when trying to disable guest access, then disable guest access`() =
        runTest {
            // given
            val (arrangement, editGuestAccessViewModel) = Arrangement()
                .withUpdateConversationAccessRoleResult(UpdateConversationAccessRoleUseCase.Result.Success)
                .arrange()
            advanceUntilIdle()

            // when
            editGuestAccessViewModel.onGuestDialogConfirm()

            // then
            coVerify(exactly = 1) { arrangement.updateConversationAccessRole(any(), any(), any()) }
            assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
        }

    @Test
    fun `given a failure running updateConversationAccessRole, when trying to disable guest access, then do not disable guest access`() =
        runTest {
            // given
            val (arrangement, editGuestAccessViewModel) = Arrangement()
                .withUpdateConversationAccessRoleResult(
                    UpdateConversationAccessRoleUseCase.Result.Failure(CoreFailure.MissingClientRegistration)
                )
                .arrange()
            advanceUntilIdle()

            // when
            editGuestAccessViewModel.onGuestDialogConfirm()

            // then
            coVerify(exactly = 1) { arrangement.updateConversationAccessRole(any(), any(), any()) }
            assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
        }

    @Test
    fun `given conversation guest is enabled, when init, then sync conversation code`() = runTest {

        val conversationDetailsResult = flowOf(
            TestConversationDetails.GROUP.let {
                it.copy(conversation = it.conversation.copy(accessRole = listOf(Conversation.AccessRole.GUEST, Conversation.AccessRole.NON_TEAM_MEMBER), name = "test")).let {
                        ObserveConversationDetailsUseCase.Result.Success(it)
                    }
            }
        )

        val conversationMember = flow {
            emit(ConversationParticipantsData(isSelfAnAdmin = true))
        }
        // given
        val (arrangement, _) = Arrangement()
            .withConversationDetails(conversationDetailsResult)
            .withConversationMembers(conversationMember)
            .withSyncConversationCodeSuccess()
            .arrange()

        advanceUntilIdle()

        // then
        coVerify(exactly = 1) { arrangement.syncConversationCodeUseCase(any()) }
    }

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var observeConversationMembers: ObserveParticipantsForConversationUseCase

        @MockK
        lateinit var updateConversationAccessRole: UpdateConversationAccessRoleUseCase

        @MockK
        lateinit var generateGuestRoomLink: GenerateGuestRoomLinkUseCase

        @MockK
        lateinit var observeGuestRoomLink: ObserveGuestRoomLinkUseCase

        @MockK
        lateinit var revokeGuestRoomLink: RevokeGuestRoomLinkUseCase

        @MockK
        lateinit var observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase

        @MockK
        lateinit var canCreatePasswordProtectedLinks: CanCreatePasswordProtectedLinksUseCase

        @MockK
        lateinit var syncConversationCodeUseCase: SyncConversationCodeUseCase

        val editGuestAccessViewModel: EditGuestAccessViewModel by lazy {
            EditGuestAccessViewModel(
                savedStateHandle = savedStateHandle,
                observeConversationDetails = observeConversationDetails,
                observeConversationMembers = observeConversationMembers,
                updateConversationAccessRole = updateConversationAccessRole,
                generateGuestRoomLink = generateGuestRoomLink,
                observeGuestRoomLink = observeGuestRoomLink,
                revokeGuestRoomLink = revokeGuestRoomLink,
                observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag,
                canCreatePasswordProtectedLinks = canCreatePasswordProtectedLinks,
                dispatcher = TestDispatcherProvider(),
                syncConversationCode = syncConversationCodeUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<EditGuestAccessNavArgs>() } returns EditGuestAccessNavArgs(
                conversationId = OtherUserProfileScreenViewModelTest.CONVERSATION_ID,
                editGuessAccessParams = EditGuestAccessParams(
                    isGuestAccessAllowed = true,
                    isServicesAllowed = true,
                    isUpdatingGuestAccessAllowed = true
                )
            )
            coEvery { observeConversationDetails(any()) } returns flowOf()
            coEvery { observeConversationMembers(any()) } returns flowOf()
            coEvery { observeGuestRoomLink(any()) } returns flowOf()
            coEvery { observeGuestRoomLinkFeatureFlag() } returns flowOf()
            coEvery { canCreatePasswordProtectedLinks() } returns true
        }

        fun withSyncConversationCodeSuccess() = apply {
            coEvery { syncConversationCodeUseCase.invoke(any()) }
        }

        fun withConversationMembers(result: Flow<ConversationParticipantsData>) = apply {
            coEvery { observeConversationMembers(any()) } returns result
        }

        fun withConversationDetails(result: Flow<ObserveConversationDetailsUseCase.Result>) = apply {
            coEvery { observeConversationDetails(any()) } returns result
        }

        fun withRevokeGuestRoomLinkResult(result: RevokeGuestRoomLinkResult) = apply {
            coEvery { revokeGuestRoomLink(any()) } returns result
        }

        fun withGenerateGuestRoomResult(result: GenerateGuestRoomLinkResult) = apply {
            coEvery { generateGuestRoomLink.invoke(any(), any()) } returns result
        }

        fun withUpdateConversationAccessRoleResult(result: UpdateConversationAccessRoleUseCase.Result) = apply {
            editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)
            coEvery { updateConversationAccessRole(any(), any(), any()) } returns result
        }

        fun arrange() = this to editGuestAccessViewModel
    }
}
