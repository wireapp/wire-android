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

package com.wire.android.ui.home.settings.account

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestTeam
import com.wire.android.framework.TestUser
import com.wire.android.util.newServerConfig
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase.Result.Success
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
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
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertFalse(viewModel.hasSAMLCred)
    }

    @Test
    fun `when trying to compute if the user requires password return true, then hasSAMLCred is false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertFalse(viewModel.hasSAMLCred)
    }

    @Test
    fun `when trying to compute if the user requires password return false, then hasSAMLCred is true`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertTrue(viewModel.hasSAMLCred)
    }

    @Test
    fun `when isAccountReadOnly return true, then managedByWire is false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertFalse(viewModel.managedByWire)
    }

    @Test
    fun `when isAccountReadOnly return false, then managedByWire is true`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(false)
            .withE2EIEnabledResult(false)
            .arrange()

        assertTrue(viewModel.managedByWire)
    }

    @Test
    fun `when user does not requires password, then should not load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        coVerify(exactly = 0) {
            arrangement.selfServerConfigUseCase.invoke()
        }
    }

    @Test
    fun `when user requires a password, then should load forgot password url context`() = runTest {
        val (arrangement, _) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        coVerify(exactly = 1) { arrangement.selfServerConfigUseCase() }
    }

    @Test
    fun `when user IS managed by Wire, then edit handle is allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(false)
            .withE2EIEnabledResult(false)
            .arrange()

        assertTrue(viewModel.myAccountState.isEditHandleAllowed)
    }

    @Test
    fun `when user IS NOT managed by Wire, then edit handle is not allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(false))
            .withIsReadOnlyAccountResult(true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertFalse(viewModel.myAccountState.isEditHandleAllowed)
    }

    @Test
    fun `when the build does NOT accept email change, then edit email is not allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(false)
            .withEmailStatusByBuild(emailEditEnabled = false)
            .withE2EIEnabledResult(false)
            .arrange()

        assertFalse(viewModel.myAccountState.isEditEmailAllowed)
    }

    @Test
    fun `when the build does accept email change, then edit email IS allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(false)
            .withEmailStatusByBuild(emailEditEnabled = true)
            .withE2EIEnabledResult(false)
            .arrange()

        assertTrue(viewModel.myAccountState.isEditEmailAllowed)
    }

    @Test
    fun `given e2ei is enabled, then edit name is NOT allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(false)
            .withE2EIEnabledResult(true)
            .arrange()

        assertFalse(viewModel.myAccountState.isEditNameAllowed)
    }

    @Test
    fun `given e2ei is NOT enabled, then edit name IS allowed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withUserRequiresPasswordResult(Success(true))
            .withIsReadOnlyAccountResult(false)
            .withE2EIEnabledResult(false)
            .arrange()

        assertTrue(viewModel.myAccountState.isEditNameAllowed)
    }

    private class Arrangement {

        @MockK
        lateinit var observeSelfUserUseCase: ObserveSelfUserUseCase

        @MockK
        lateinit var getSelfTeamUseCase: GetUpdatedSelfTeamUseCase

        @MockK
        lateinit var selfServerConfigUseCase: SelfServerConfigUseCase

        @MockK
        lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

        @MockK
        lateinit var isReadOnlyAccountUseCase: IsReadOnlyAccountUseCase

        @MockK
        lateinit var isE2EIEnabledUseCase: IsE2EIEnabledUseCase

        @MockK
        lateinit var isSelfATeamMember: IsSelfATeamMemberUseCase

        private val viewModel by lazy {
            MyAccountViewModel(
                observeSelfUserUseCase,
                getSelfTeamUseCase,
                isSelfATeamMember,
                selfServerConfigUseCase,
                isPasswordRequiredUseCase,
                isReadOnlyAccountUseCase,
                TestDispatcherProvider(),
                isE2EIEnabledUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeSelfUserUseCase() } returns flowOf(TestUser.SELF_USER.copy(teamId = TeamId(TestTeam.TEAM.id)))
            coEvery { getSelfTeamUseCase() } returns Either.Right(TestTeam.TEAM)
            coEvery { selfServerConfigUseCase() } returns SelfServerConfigUseCase.Result.Success(newServerConfig(1))
            coEvery { isSelfATeamMember() } returns true
        }

        fun withUserRequiresPasswordResult(result: IsPasswordRequiredUseCase.Result = Success(true)) = apply {
            coEvery { isPasswordRequiredUseCase() } returns result
        }

        fun withEmailStatusByBuild(emailEditEnabled: Boolean = true) = apply {
            mockkObject(MyAccountViewModel.Companion)
            every { MyAccountViewModel.Companion.isChangeEmailEnabledByBuild() } returns emailEditEnabled
        }

        fun withIsReadOnlyAccountResult(result: Boolean) = apply {
            coEvery { isReadOnlyAccountUseCase() } returns result
        }

        fun withE2EIEnabledResult(result: Boolean) = apply {
            coEvery { isE2EIEnabledUseCase() } returns result
        }

        fun arrange() = this to viewModel
    }
}
