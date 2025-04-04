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
package com.wire.android.ui.home.conversations.details.updatechannelaccess

import androidx.lifecycle.SavedStateHandle
import com.wire.android.framework.TestUser
import com.wire.android.ui.destinations.ChannelAccessOnUpdateScreenDestination
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddUserPermissionType
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddUserPermissionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateChannelAccessViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given channel permission changed successfully, when updateChannelPermission is called, then update the state`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUpdateChannelAddUserPermissionUseCaseReturning(
                UpdateChannelAddUserPermissionUseCase.UpdateChannelAddUserPermissionUseCaseResult.Success
            ).arrange()

        viewModel.updateChannelAddUserPermission(ChannelAddUserPermissionType.EVERYONE)
        advanceUntilIdle()

        assertEquals(ChannelAddUserPermissionType.EVERYONE, viewModel.permissionType)
    }

    @Test
    fun `given channel permission failed to change, when updateChannelPermission is called, then do not update the state`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUpdateChannelAddUserPermissionUseCaseReturning(
                UpdateChannelAddUserPermissionUseCase.UpdateChannelAddUserPermissionUseCaseResult.Failure
            ).arrange()

        viewModel.updateChannelAddUserPermission(ChannelAddUserPermissionType.EVERYONE)
        advanceUntilIdle()

        assertEquals(ChannelAddUserPermissionType.ADMINS, viewModel.permissionType)
    }

    @Test
    fun `given channel access, when updateChannelAccess is called, then update the state`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        viewModel.updateChannelAccess(ChannelAccessType.PRIVATE)

        assertEquals(ChannelAccessType.PRIVATE, viewModel.accessType)
    }

    private class Arrangement {

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var updateChannelAddUserPermission: UpdateChannelAddUserPermissionUseCase

        private val viewModel by lazy {
            UpdateChannelAccessViewModel(
                updateChannelAddUserPermission = updateChannelAddUserPermission,
                savedStateHandle = savedStateHandle,
                qualifiedIdMapper = QualifiedIdMapperImpl(TestUser.SELF_USER_ID)
            )
        }

        init {
            val conversationId = "conversationId"
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkObject(ChannelAccessOnUpdateScreenDestination)
            every {
                ChannelAccessOnUpdateScreenDestination.argsFrom(any<SavedStateHandle>())
            } answers {
                UpdateChannelAccessArgs(conversationId)
            }

            every { savedStateHandle.navArgs<UpdateChannelAccessArgs>() } returns UpdateChannelAccessArgs(conversationId)
        }

        fun withUpdateChannelAddUserPermissionUseCaseReturning(
            result: UpdateChannelAddUserPermissionUseCase.UpdateChannelAddUserPermissionUseCaseResult
        ) = apply {
            coEvery { updateChannelAddUserPermission(any(), any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
