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

package com.wire.android.feature

import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSwitchUseCaseTest {

    private val testScope = TestScope()

    @Suppress("MaxLineLength")
    @Test
    fun givenCurrentSessionIsValid_whenSwitchingToAccountIsCalled_thenUpdateCurrentSessionAndReturnSuccessSwitchedToAnotherAccount() =
        testScope.runTest {
            val expectedResult = SwitchAccountResult.SwitchedToAnotherAccount

            val (arrangement, switchAccount) =
                Arrangement(testScope)
                    .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                    .withGetAllSessions(GetAllSessionsResult.Success(listOf(ACCOUNT_VALID_1, ACCOUNT_VALID_2)))
                    .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                    .arrange()

            val result = switchAccount(SwitchAccountParam.SwitchToAccount(ACCOUNT_VALID_2.userId))

            assertEquals(expectedResult, result)
            coVerify(exactly = 1) {
                arrangement.currentSession()
                arrangement.updateCurrentSession(any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun givenCurrentSessionIsValidAndNoOtherSessions_whenTryToSwitchToNextAccount_thenUpdateCurrentSessionAndReturnSuccessNoMoreAccounts() =
        testScope.runTest {
            val expectedResult = SwitchAccountResult.NoOtherAccountToSwitch
            val (arrangement, switchAccount) =
                Arrangement(testScope)
                    .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                    .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                    .withGetAllSessions(GetAllSessionsResult.Success(listOf(ACCOUNT_VALID_1)))
                    .arrange()

            val result = switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)

            assertEquals(expectedResult, result)
            coVerify(exactly = 1) {
                arrangement.currentSession()
                arrangement.updateCurrentSession(null)
            }
        }

    @Test
    fun givenCurrentSessionIsInvalid_whenSwitchingToAccount_thenUpdateCurrentSessionAndDeleteTheOldOne() = testScope.runTest {
        val currentAccount = ACCOUNT_INVALID_3
        val switchTo = ACCOUNT_VALID_2

        val expectedResult = SwitchAccountResult.SwitchedToAnotherAccount

        val (arrangement, switchAccount) =
            Arrangement(testScope)
                .withGetCurrentSession(CurrentSessionResult.Success(currentAccount))
                .withUpdateCurrentSession(UpdateCurrentSessionUseCase.Result.Success)
                .withGetAllSessions(GetAllSessionsResult.Success(listOf(currentAccount, switchTo)))
                .withDeleteSession(currentAccount.userId, DeleteSessionUseCase.Result.Success)
                .arrange()

        val result = switchAccount(SwitchAccountParam.SwitchToAccount(switchTo.userId))
        testScope.advanceUntilIdle()

        assertEquals(expectedResult, result)
        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.updateCurrentSession(switchTo.userId)
            arrangement.deleteSession(currentAccount.userId)
        }
    }

    @Test
    fun givenProvidedAccountIsNotFound_whenSwitchingToAccount_thenReturnGivenAccountIsInvalid() = testScope.runTest {
        val (arrangement, switchAccount) =
            Arrangement(testScope)
                .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                .withGetAllSessions(GetAllSessionsResult.Success(listOf(ACCOUNT_VALID_1)))
                .arrange()

        val result = switchAccount(SwitchAccountParam.SwitchToAccount(ACCOUNT_VALID_2.userId))

        assertEquals(SwitchAccountResult.GivenAccountIsInvalid, result)
        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.getSessions()
        }
    }

    @Test
    fun givenProvidedAccountIsNotValid_whenSwitchingToAccount_thenReturnGivenAccountIsInvalid() = testScope.runTest {
        val (arrangement, switchAccount) =
            Arrangement(testScope)
                .withGetCurrentSession(CurrentSessionResult.Success(ACCOUNT_VALID_1))
                .withGetAllSessions(GetAllSessionsResult.Success(listOf(ACCOUNT_VALID_1, ACCOUNT_INVALID_3)))
                .arrange()

        val result = switchAccount(SwitchAccountParam.SwitchToAccount(ACCOUNT_INVALID_3.userId))

        assertEquals(SwitchAccountResult.GivenAccountIsInvalid, result)
        coVerify(exactly = 1) {
            arrangement.currentSession()
            arrangement.getSessions()
        }
    }

    private companion object {
        val ACCOUNT_VALID_1 = AccountInfo.Valid(UserId("userId_valid_1", "domain_valid_1"))
        val ACCOUNT_VALID_2 = AccountInfo.Valid(UserId("userId_valid_2", "domain_valid_2"))
        val ACCOUNT_INVALID_3 =
            AccountInfo.Invalid(UserId("userId_invalid_3", "domain_invalid_3"), LogoutReason.SELF_SOFT_LOGOUT)
    }

    private class Arrangement(val testScope: TestScope) {

        @MockK
        lateinit var updateCurrentSession: UpdateCurrentSessionUseCase

        @MockK
        lateinit var getSessions: GetSessionsUseCase

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var deleteSession: DeleteSessionUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        var accountSwitchUseCase: AccountSwitchUseCase = AccountSwitchUseCase(
            updateCurrentSession,
            getSessions,
            currentSession,
            deleteSession,
            testScope
        )

        fun withGetAllSessions(result: GetAllSessionsResult) = apply {
            coEvery { getSessions() } returns result
        }

        fun withGetCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withUpdateCurrentSession(result: UpdateCurrentSessionUseCase.Result) = apply {
            coEvery { updateCurrentSession(any()) } returns result
        }

        fun withDeleteSession(userId: UserId, result: DeleteSessionUseCase.Result) = apply {
            coEvery { deleteSession(userId) } returns result
        }

        fun arrange() = this to accountSwitchUseCase
    }
}
