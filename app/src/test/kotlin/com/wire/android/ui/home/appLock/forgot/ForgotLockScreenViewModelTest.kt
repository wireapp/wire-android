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
package com.wire.android.ui.home.appLock.forgot

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ForgotLockScreenViewModelTest {
    private val dispatcher = TestDispatcherProvider()

    // password validation
    @Test
    fun `given password not required, when validating password, then return Success with empty password`() =
        runTest(dispatcher.default()) {
            val (arrangement, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(false))
                .arrange()
            val (result, resultPassword) = viewModel.validatePasswordIfNeeded("password")
            assert(result is ForgotLockScreenViewModel.Result.Success)
            assertEquals("", resultPassword)
            verify(exactly = 0) { arrangement.validatePasswordUseCase(any()) }
        }
    @Test
    fun `given password required and valid, when validating password, then return Success with given password`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withValidatePasswordResult(ValidatePasswordResult.Valid)
                .arrange()
            val (result, resultPassword) = viewModel.validatePasswordIfNeeded("password")
            assert(result is ForgotLockScreenViewModel.Result.Success)
            assertEquals("password", resultPassword)
        }
    @Test
    fun `given password required but invalid, when validating password, then return InvalidPassword`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withValidatePasswordResult(ValidatePasswordResult.Invalid())
                .arrange()
            val (result, _) = viewModel.validatePasswordIfNeeded("password")
            assert(result is ForgotLockScreenViewModel.Result.Failure.InvalidPassword)
        }
    @Test
    fun `given password required but not provided, when validating password, then return PasswordRequired`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withValidatePasswordResult(ValidatePasswordResult.Invalid())
                .arrange()
            val (result, _) = viewModel.validatePasswordIfNeeded("")
            assert(result is ForgotLockScreenViewModel.Result.Failure.PasswordRequired)
        }
    @Test
    fun `given password required returns failure, when validating password, then return failure`() =
        runTest(dispatcher.default()) {
            val (arrangement, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound))
                .arrange()
            val (result, _) = viewModel.validatePasswordIfNeeded("password")
            assert(result is ForgotLockScreenViewModel.Result.Failure)
            verify(exactly = 0) { arrangement.validatePasswordUseCase(any()) }
        }

    // current client deletion
    private fun testClientDelete(deleteClientResult: DeleteClientResult, expected: ForgotLockScreenViewModel.Result) =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withDeleteClientResult(deleteClientResult)
                .arrange()
            val result = viewModel.deleteCurrentClient("password")
            assertEquals(expected, result)
        }
    @Test
    fun `given deleting client returns success, when deleting current client, then return Success`() =
        testClientDelete(
            deleteClientResult = DeleteClientResult.Success,
            expected = ForgotLockScreenViewModel.Result.Success
        )
    @Test
    fun `given deleting client returns invalid credentials, when deleting current client, then return InvalidPassword`() =
        testClientDelete(
            deleteClientResult = DeleteClientResult.Failure.InvalidCredentials,
            expected = ForgotLockScreenViewModel.Result.Failure.InvalidPassword
        )
    @Test
    fun `given deleting client returns failure, when deleting current client, then return failure`() =
        testClientDelete(
            deleteClientResult = DeleteClientResult.Failure.Generic(StorageFailure.DataNotFound),
            expected = ForgotLockScreenViewModel.Result.Failure.Generic(StorageFailure.DataNotFound)
        )

    // sessions hard logout
    private fun Arrangement.verifyHardLogoutActions(successActionsCalled: Boolean, userLogoutActionsCalled: Boolean) {
        val successActionsCalledExactly = if (successActionsCalled) 1 else 0
        coVerify(exactly = successActionsCalledExactly) { observeEstablishedCallsUseCase() }
        coVerify(exactly = successActionsCalledExactly) { endCallUseCase(any()) }
        coVerify(exactly = successActionsCalledExactly) { globalDataStore.clearAppLockPasscode() }
        coVerify(exactly = successActionsCalledExactly) { accountSwitchUseCase(SwitchAccountParam.Clear) }
        val logoutActionsCalledExactly = if (userLogoutActionsCalled) 1 else 0
        coVerify(exactly = logoutActionsCalledExactly) { logoutUseCase(any(), any()) }
        coVerify(exactly = logoutActionsCalledExactly) { notificationManager.stopObservingOnLogout(any()) }
        coVerify(exactly = logoutActionsCalledExactly) { userDataStore.clear() }
    }
    private fun testLoggingOut(
        getSessionsResult: GetAllSessionsResult,
        expected: ForgotLockScreenViewModel.Result,
        successActionsCalled: Boolean,
        userLogoutActionsCalled: Boolean
    ) =
        runTest(dispatcher.default()) {
            val (arrangement, viewModel) = Arrangement()
                .withGetSessionsResult(getSessionsResult)
                .arrange()
            val result = viewModel.hardLogoutAllAccounts()
            advanceUntilIdle()
            assertEquals(expected, result)
            arrangement.verifyHardLogoutActions(successActionsCalled, userLogoutActionsCalled)
    }
    @Test
    fun `given no sessions, when logging out, then make all required actions other than logout and return success`() =
        testLoggingOut(
            getSessionsResult = GetAllSessionsResult.Failure.NoSessionFound,
            expected = ForgotLockScreenViewModel.Result.Success,
            successActionsCalled = true,
            userLogoutActionsCalled = false
        )
    @Test
    fun `given no valid sessions, when logging out, then make all required actions other than logout and return success`() =
        testLoggingOut(
            getSessionsResult = GetAllSessionsResult.Success(listOf(INVALID_SESSION)),
            expected = ForgotLockScreenViewModel.Result.Success,
            successActionsCalled = true,
            userLogoutActionsCalled = false
        )
    @Test
    fun `given valid sessions, when logging out, then make all required actions with logout and return success`() =
        testLoggingOut(
            getSessionsResult = GetAllSessionsResult.Success(listOf(VALID_SESSION)),
            expected = ForgotLockScreenViewModel.Result.Success,
            successActionsCalled = true,
            userLogoutActionsCalled = true
        )
    @Test
    fun `given sessions return failure, when hard-logging out sessions, then return failure`() =
        testLoggingOut(
            getSessionsResult = GetAllSessionsResult.Failure.Generic(StorageFailure.DataNotFound),
            expected = ForgotLockScreenViewModel.Result.Failure.Generic(StorageFailure.DataNotFound),
            successActionsCalled = false,
            userLogoutActionsCalled = false
        )

    class Arrangement {
        @MockK lateinit var coreLogic: CoreLogic
        @MockK lateinit var userSessionScope: UserSessionScope
        @MockK lateinit var logoutUseCase: LogoutUseCase
        @MockK lateinit var globalDataStore: GlobalDataStore
        @MockK lateinit var userDataStoreProvider: UserDataStoreProvider
        @MockK lateinit var userDataStore: UserDataStore
        @MockK lateinit var notificationManager: WireNotificationManager
        @MockK lateinit var getSelfUserUseCase: GetSelfUserUseCase
        @MockK lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase
        @MockK lateinit var validatePasswordUseCase: ValidatePasswordUseCase
        @MockK lateinit var observeCurrentClientIdUseCase: ObserveCurrentClientIdUseCase
        @MockK lateinit var deleteClientUseCase: DeleteClientUseCase
        @MockK lateinit var getSessionsUseCase: GetSessionsUseCase
        @MockK lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase
        @MockK lateinit var endCallUseCase: EndCallUseCase
        @MockK lateinit var accountSwitchUseCase: AccountSwitchUseCase

        private val viewModel: ForgotLockScreenViewModel by lazy {
            ForgotLockScreenViewModel(
                coreLogic, globalDataStore, userDataStoreProvider, notificationManager, getSelfUserUseCase,
                isPasswordRequiredUseCase, validatePasswordUseCase, observeCurrentClientIdUseCase, deleteClientUseCase, getSessionsUseCase,
                observeEstablishedCallsUseCase, endCallUseCase, accountSwitchUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every { userSessionScope.logout } returns logoutUseCase
            every { userDataStoreProvider.getOrCreate(any()) } returns userDataStore
            coEvery { observeCurrentClientIdUseCase() } returns flowOf(ClientId("currentClientId"))
            val call: Call = mockk()
            coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf(call))
            every { call.conversationId } returns ConversationId("conversationId", "domain")
            coEvery { accountSwitchUseCase(any()) } returns SwitchAccountResult.NoOtherAccountToSwitch
        }

        fun withIsPasswordRequiredResult(result: IsPasswordRequiredUseCase.Result) =
            apply { coEvery { isPasswordRequiredUseCase() } returns result }
        fun withValidatePasswordResult(result: ValidatePasswordResult) =
            apply { coEvery { validatePasswordUseCase(any()) } returns result }
        fun withDeleteClientResult(result: DeleteClientResult) =
            apply { coEvery { deleteClientUseCase(any()) } returns result }
        fun withGetSessionsResult(result: GetAllSessionsResult) =
            apply { coEvery { getSessionsUseCase() } returns result }
        fun arrange() = this to viewModel
    }

    companion object {
        val INVALID_SESSION = AccountInfo.Invalid(UserId("id", "domain"), LogoutReason.SELF_HARD_LOGOUT)
        val VALID_SESSION = AccountInfo.Valid(UserId("id", "domain"))
    }
}
