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

package com.wire.android.ui.home.conversations.details.edit_guest_access

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
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

    private lateinit var editGuestAccessViewModel: EditGuestAccessViewModel

    @Before
    fun setUp() {
        editGuestAccessViewModel = EditGuestAccessViewModel(
            navigationManager = navigationManager,
            dispatcher = TestDispatcherProvider(),
            updateConversationAccessRole = updateConversationAccessRoleUseCase,
            savedStateHandle = savedStateHandle,
            qualifiedIdMapper = qualifiedIdMapper
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
            assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
        }

    @Test
    fun `given a failure when running updateConversationAccessRole use case, when trying to enable guest access, then do not enable guest access`() {
        editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = false)
        coEvery {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        } returns UpdateConversationAccessRoleUseCase.Result.Failure(CoreFailure.MissingClientRegistration)

        editGuestAccessViewModel.updateGuestAccess(true)

        coVerify(exactly = 1) {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed)
    }

    @Test
    fun `given guest access is activated, when trying to enable guest access, then display dialog before disabling guest access`() {
        editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)

        editGuestAccessViewModel.updateGuestAccess(false)

        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.changeGuestOptionConfirmationRequired)
        coVerify(inverse = true) {
            updateConversationAccessRoleUseCase(any(), any(), any(), any())
        }
    }
}
