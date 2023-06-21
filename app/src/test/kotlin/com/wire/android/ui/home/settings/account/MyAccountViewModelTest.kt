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

package com.wire.android.ui.home.settings.account

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestTeam
import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase.Result.Success
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MyAccountViewModelTest {

    @Test
    fun `when trying to compute if the user requires password fails, then hasSAMLCred is false`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        assertFalse(arrangement.viewModel.hasSAMLCred)
    }

    @Test
    fun `when trying to compute if the user requires password return true, then hasSAMLCred is false`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        assertFalse(arrangement.viewModel.hasSAMLCred)
    }

    @Test
    fun `when trying to compute if the user requires password return false, then hasSAMLCred is true`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        assertTrue(arrangement.viewModel.hasSAMLCred)
    }

    @Test
    fun `when isAccountReadOnly return true, then managedByWire is false`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        assertFalse(arrangement.viewModel.managedByWire)
    }

    @Test
    fun `when isAccountReadOnly return false, then managedByWire is true`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(false)
            .arrange()

        assertTrue(arrangement.viewModel.managedByWire)
    }

    @Test
    fun `when user does not requires password, then should not load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        verify {
            arrangement.selfServerConfigUseCase wasNot Called
        }
    }

    @Test
    fun `when user requires a password, then should load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        coVerify(exactly = 1) { arrangement.selfServerConfigUseCase() }
    }

    @Test
    fun `when user is managed by Wire, then edit handle is allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(true)
            .arrange()

        assertFalse(viewModel.myAccountState.isEditHandleAllowed)
    }

    @Test
    fun `when user is not managed by Wire, then edit handle is allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(false)
            .arrange()

        assertTrue(viewModel.myAccountState.isEditHandleAllowed)
    }

    private class Arrangement {

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var getSelfTeamUseCase: GetSelfTeamUseCase

        @MockK
        lateinit var selfServerConfigUseCase: SelfServerConfigUseCase

        @MockK
        lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

        @MockK
        lateinit var isReadOnlyAccountUseCase: IsReadOnlyAccountUseCase

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        val viewModel by lazy {
            MyAccountViewModel(
                savedStateHandle,
                getSelfUserUseCase,
                getSelfTeamUseCase,
                selfServerConfigUseCase,
                isPasswordRequiredUseCase,
                isReadOnlyAccountUseCase,
                TestDispatcherProvider()
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelfUserUseCase() } returns flowOf(TestUser.SELF_USER.copy(teamId = TeamId(TestTeam.TEAM.id)))
            coEvery { getSelfTeamUseCase() } returns flowOf(TestTeam.TEAM)
            coEvery { selfServerConfigUseCase() } returns SelfServerConfigUseCase.Result.Success(newServerConfig(1))
        }

        fun withUserRequiresPasswordResult(result: IsPasswordRequiredUseCase.Result = Success(true)) = apply {
            coEvery { isPasswordRequiredUseCase() } returns result
        }

        fun withIsReadOnlyAccountResult(result: Boolean) = apply {
            coEvery { isReadOnlyAccountUseCase() } returns result
        }

        fun arrange() = this to viewModel
    }
}
