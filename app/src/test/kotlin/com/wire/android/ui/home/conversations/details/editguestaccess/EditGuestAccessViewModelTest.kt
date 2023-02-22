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

@file:Suppress("MaximumLineLength")

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class EditGuestAccessViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

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

    private lateinit var editGuestAccessViewModel: EditGuestAccessViewModel

    @Before
    fun setUp() {
        editGuestAccessViewModel = EditGuestAccessViewModel(
            navigationManager = navigationManager,
            dispatcher = TestDispatcherProvider(),
            observeConversationDetails = observeConversationDetails,
            observeConversationMembers = observeConversationMembers,
            updateConversationAccessRole = updateConversationAccessRole,
            generateGuestRoomLink = generateGuestRoomLink,
            revokeGuestRoomLink = revokeGuestRoomLink,
            observeGuestRoomLink = observeGuestRoomLink,
            savedStateHandle = savedStateHandle,
            qualifiedIdMapper = qualifiedIdMapper,
            observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag
        )
    }

    @Test
    fun `given updateConversationAccessRole use case runs successfully, when trying to enable guest access, then enable guest access`() =
        runTest {
            editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = false)
            coEvery {
                updateConversationAccessRoleUseCase(any(), any(), any(), any())
            } returns UpdateConversationAccessRoleUseCase.Result.Success

            editGuestAccessViewModel.updateGuestAccess(true)

            coVerify(exactly = 1) {
                updateConversationAccessRoleUseCase(any(), any(), any(), any())
            }
            assertEquals(
                true,
                editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
            )
        }

    @Test
    fun `given a failure when running updateConversationAccessRole, when trying to enable guest access, then do not enable guest access`() {
        editGuestAccessViewModel.editGuestAccessState =
            editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = false)
        coEvery {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        } returns UpdateConversationAccessRoleUseCase.Result.Failure(
            CoreFailure.MissingClientRegistration
        )

        editGuestAccessViewModel.updateGuestAccess(true)

        coVerify(exactly = 1) {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        }
        assertEquals(
            false,
            editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
        )
    }

    @Test
    fun `given guest access is activated, when trying to enable guest access, then display dialog before disabling guest access`() {
        editGuestAccessViewModel.editGuestAccessState =
            editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)

        editGuestAccessViewModel.updateGuestAccess(false)

        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.shouldShowGuestAccessChangeConfirmationDialog)
        coVerify(inverse = true) {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        }
    }

    @Test
    fun given_useCase_runs_with_success_When_generating_guest_link_Then_Invoke_it_once() = runTest {
        coEvery {
            generateGuestRoomLink(any())
        } returns GenerateGuestRoomLinkResult.Success

        editGuestAccessViewModel.onGuestDialogConfirm()

        coVerify(exactly = 1) {
            generateGuestRoomLink(any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGeneratingGuestRoomLink)
    }

    @Test
    fun `given_useCase_runs_with_failureWhen_generating_guest_link_Then_show_dialog_error`() = runTest {
        coEvery {
            generateGuestRoomLink(any())
        } returns GenerateGuestRoomLinkResult.Failure(CoreFailure.MissingClientRegistration)

        editGuestAccessViewModel.onGuestDialogConfirm()

        coVerify(exactly = 1) {
            generateGuestRoomLink(any())
        }
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToGenerateGuestRoomLink)
    }

    @Test
    fun `given_useCase_runs_with_success_When_revoking_guest_link_Then_Invoke_it_once`() = runTest {
        coEvery {
            revokeGuestRoomLink(any())
        } returns RevokeGuestRoomLinkResult.Success

        editGuestAccessViewModel.onRevokeDialogConfirm()

        coVerify(exactly = 1) {
            revokeGuestRoomLink(any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
    }

    @Test
    fun `given_useCase_runs_with_failure_When_revoking_guest_link_Then_show_dialog_error`() = runTest {
        coEvery {
            revokeGuestRoomLink(any())
        } returns RevokeGuestRoomLinkResult.Failure(CoreFailure.MissingClientRegistration)

        editGuestAccessViewModel.onRevokeDialogConfirm()

        coVerify(exactly = 1) {
            revokeGuestRoomLink(any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToRevokeGuestRoomLink)
    }
}
