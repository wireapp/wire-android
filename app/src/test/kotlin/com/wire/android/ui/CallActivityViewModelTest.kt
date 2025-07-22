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
package com.wire.android.ui

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.ui.calling.CallActivityViewModel
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CallActivityViewModelTest {

    @Test
    fun `given no current, when checking screenshot censoring config, then return false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(false, result.await())
        }

    @Test
    fun `given screenshot censoring enabled, when checking screenshot censoring config, then return true`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withScreenshotCensoringConfigReturning(ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(true, result.await())
        }

    @Test
    fun `given screenshot censoring disabled, when checking screenshot censoring config, then return false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withScreenshotCensoringConfigReturning(ObserveScreenshotCensoringConfigResult.Disabled)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(false, result.await())
        }

    @Test
    fun `given no session available, when trying to switch account, then try to switchAccount usecase once`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
                .withAccountSwitch(SwitchAccountResult.Failure)
                .arrange()

            viewModel.switchAccountIfNeeded(userId, arrangement.switchAccountActions)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.accountSwitch(any()) }
        }

    @Test
    fun `given userId different from currentSession, when trying to switch account, then invoke switchAccount usecase once`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withAccountSwitch(SwitchAccountResult.SwitchedToAnotherAccount)
                .arrange()

            viewModel.switchAccountIfNeeded(UserId("anotherUserId", "domain"), arrangement.switchAccountActions)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.accountSwitch(any()) }
        }

    @Test
    fun `given userId same as currentSession, when trying to switch account, then do not invoke switchAccount usecase`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withAccountSwitch(SwitchAccountResult.SwitchedToAnotherAccount)
                .arrange()

            viewModel.switchAccountIfNeeded(userId, arrangement.switchAccountActions)
            advanceUntilIdle()

            coVerify(inverse = true) { arrangement.accountSwitch(any()) }
        }

    private fun testCallingSwitchAccountActions(
        switchAccountResult: SwitchAccountResult,
        switchedToAnotherAccountCalled: Boolean = false,
        noOtherAccountToSwitchCalled: Boolean = false,
    ) = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionReturning(CurrentSessionResult.Success(AccountInfo.Valid(UserId("user", "domain"))))
            .withAccountSwitch(switchAccountResult)
            .arrange()

        viewModel.switchAccountIfNeeded(UserId("anotherUser", "domain"), arrangement.switchAccountActions)
        advanceUntilIdle()

        coVerify(exactly = if (switchedToAnotherAccountCalled) 1 else 0) {
            arrangement.switchAccountActions.switchedToAnotherAccount()
        }
        coVerify(exactly = if (noOtherAccountToSwitchCalled) 1 else 0) {
            arrangement.switchAccountActions.noOtherAccountToSwitch()
        }
    }

    @Test
    fun `given no other account to switch, when switching, then call proper action`() = testCallingSwitchAccountActions(
        switchAccountResult = SwitchAccountResult.NoOtherAccountToSwitch,
        switchedToAnotherAccountCalled = false,
        noOtherAccountToSwitchCalled = true,
    )

    @Test
    fun `given account switched, when switching, then call proper action`() = testCallingSwitchAccountActions(
        switchAccountResult = SwitchAccountResult.SwitchedToAnotherAccount,
        switchedToAnotherAccountCalled = true,
        noOtherAccountToSwitchCalled = false,
    )

    @Test
    fun `given invalid account, when switching, then do not call any action`() = testCallingSwitchAccountActions(
        switchAccountResult = SwitchAccountResult.GivenAccountIsInvalid,
        switchedToAnotherAccountCalled = false,
        noOtherAccountToSwitchCalled = false,
    )

    @Test
    fun `given failure, when switching, then do not call any action`() = testCallingSwitchAccountActions(
        switchAccountResult = SwitchAccountResult.Failure,
        switchedToAnotherAccountCalled = false,
        noOtherAccountToSwitchCalled = false,
    )

    private class Arrangement {

        @MockK
        private lateinit var observeScreenshotCensoringConfigUseCaseProviderFactory:
                ObserveScreenshotCensoringConfigUseCaseProvider.Factory

        @MockK
        private lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var accountSwitch: AccountSwitchUseCase

        @MockK
        private lateinit var observeScreenshotCensoringConfig: ObserveScreenshotCensoringConfigUseCase

        @MockK
        lateinit var switchAccountActions: SwitchAccountActions

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { observeScreenshotCensoringConfigUseCaseProviderFactory.create(any()).observeScreenshotCensoringConfig } returns
                    observeScreenshotCensoringConfig
        }

        private val viewModel by lazy {
            CallActivityViewModel(
                dispatchers = TestDispatcherProvider(),
                currentSession = currentSession,
                observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
                accountSwitch = accountSwitch
            )
        }

        fun arrange() = this to viewModel

        suspend fun withCurrentSessionReturning(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        suspend fun withAccountSwitch(result: SwitchAccountResult) = apply {
            coEvery { accountSwitch(any()) } returns result
        }

        suspend fun withScreenshotCensoringConfigReturning(result: ObserveScreenshotCensoringConfigResult) =
            apply {
                coEvery { observeScreenshotCensoringConfig() } returns flowOf(result)
            }
    }

    companion object {
        val userId = UserId("userId", "domain")
        val accountInfo = AccountInfo.Valid(userId = userId)
    }
}
