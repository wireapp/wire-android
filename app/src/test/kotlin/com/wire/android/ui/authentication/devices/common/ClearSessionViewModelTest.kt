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
package com.wire.android.ui.authentication.devices.common

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ClearSessionViewModelTest {

    @Test
    fun `given cancel login dialog not shown, when back button clicked, then show cancel login dialog`() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        viewModel.state.showCancelLoginDialog shouldBeEqualTo false
        // when
        viewModel.onBackButtonClicked()
        // then
        viewModel.state.showCancelLoginDialog shouldBeEqualTo true
    }

    @Test
    fun `given cancel login dialog shown, when proceed login clicked, then hide cancel login dialog`() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        viewModel.onBackButtonClicked() // to show dialog
        viewModel.state.showCancelLoginDialog shouldBeEqualTo true
        // when
        viewModel.onProceedLoginClicked()
        // then
        viewModel.state.showCancelLoginDialog shouldBeEqualTo false
    }

    @Test
    fun `given cancel login dialog shown, when cancel login clicked, then hide cancel login dialog`() = runTest {
        // given
        val currentSession = AccountInfo.Valid(UserId("userId", "domain"))
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Success(currentSession))
            .arrange()
        viewModel.onBackButtonClicked() // to show dialog
        viewModel.state.showCancelLoginDialog shouldBeEqualTo true
        // when
        viewModel.onCancelLoginClicked(arrangement.switchAccountActions)
        advanceUntilIdle()
        // then
        viewModel.state.showCancelLoginDialog shouldBeEqualTo false
    }

    @Test
    fun `given valid session, when cancel login clicked, then call proper actions`() = runTest {
        // given
        val currentSession = AccountInfo.Valid(UserId("userId", "domain"))
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Success(currentSession))
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            .withSwitchAccountReturning(SwitchAccountResult.SwitchedToAnotherAccount)
            .arrange()
        // when
        viewModel.onCancelLoginClicked(arrangement.switchAccountActions)
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) {
            arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(currentSession.userId)
            arrangement.switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)
            arrangement.switchAccountActions.switchedToAnotherAccount()
        }
    }

    @Test
    fun `given no valid session, when canceling, then do not logout or delete session but do switch account`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
            .withSwitchAccountReturning(SwitchAccountResult.SwitchedToAnotherAccount)
            .arrange()
        // when
        viewModel.onCancelLoginClicked(arrangement.switchAccountActions)
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) {
            arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(any())
        }
        coVerify(exactly = 1) {
            arrangement.switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)
            arrangement.switchAccountActions.switchedToAnotherAccount()
        }
    }

    @Test
    fun `given no valid session and delete session fails, when canceling, then still do switch account`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
            .withDeleteSessionReturning(DeleteSessionUseCase.Result.Failure(StorageFailure.DataNotFound))
            .withSwitchAccountReturning(SwitchAccountResult.SwitchedToAnotherAccount)
            .arrange()
        viewModel.onBackButtonClicked() // to show dialog
        viewModel.state.showCancelLoginDialog shouldBeEqualTo true
        // when
        viewModel.onCancelLoginClicked(arrangement.switchAccountActions)
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) {
            arrangement.logout(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(any())
        }
        coVerify(exactly = 1) {
            arrangement.switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)
            arrangement.switchAccountActions.switchedToAnotherAccount()
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var deleteSession: DeleteSessionUseCase

        @MockK
        lateinit var logout: LogoutUseCase

        @MockK
        lateinit var switchAccount: AccountSwitchUseCase

        @MockK
        lateinit var switchAccountActions: SwitchAccountActions

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(UserId("userId", "domain"))))
            withDeleteSessionReturning(DeleteSessionUseCase.Result.Success)
            withSwitchAccountReturning(SwitchAccountResult.SwitchedToAnotherAccount)
        }

        fun arrange() = this to ClearSessionViewModel(currentSession, deleteSession, switchAccount, logout)

        fun withCurrentSessionReturning(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withDeleteSessionReturning(result: DeleteSessionUseCase.Result) = apply {
            coEvery { deleteSession(any()) } returns result
        }

        fun withSwitchAccountReturning(result: SwitchAccountResult) = apply {
            coEvery { switchAccount(any()) } returns result
        }
    }
}
