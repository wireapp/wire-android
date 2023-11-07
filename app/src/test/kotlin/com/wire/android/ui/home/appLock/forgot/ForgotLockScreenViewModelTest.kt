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
 */
package com.wire.android.ui.home.appLock.forgot

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.isLeft
import com.wire.kalium.logic.functional.isRight
import io.mockk.Called
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ForgotLockScreenViewModelTest {
    private val dispatcher = TestDispatcherProvider()

    // password validation
    @Test
    fun `given password not required, when validating password, then return Success`() =
        runTest(dispatcher.default()) {
            val (arrangement, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(false))
                .arrange()
            val result = viewModel.validatePasswordIfNeeded("password")
            assert(result.isRight() && (result as Either.Right).value == ForgotLockScreenViewModel.Result.Success)
            verify { arrangement.validatePasswordUseCase(any()) wasNot Called }
        }
    @Test
    fun `given password required and valid, when validating password, then return Success`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withValidatePasswordResult(ValidatePasswordResult.Valid)
                .arrange()
            val result = viewModel.validatePasswordIfNeeded("password")
            assert(result.isRight() && (result as Either.Right).value == ForgotLockScreenViewModel.Result.Success)
        }
    @Test
    fun `given password required but invalid, when validating password, then return InvalidPassword`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withValidatePasswordResult(ValidatePasswordResult.Invalid())
                .arrange()
            val result = viewModel.validatePasswordIfNeeded("password")
            assert(result.isRight() && (result as Either.Right).value == ForgotLockScreenViewModel.Result.InvalidPassword)
        }
    @Test
    fun `given password required returns failure, when validating password, then return failure`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Failure(StorageFailure.DataNotFound))
                .arrange()
            val result = viewModel.validatePasswordIfNeeded("password")
            assert(result.isLeft())
        }

    // current client deletion
    private fun testSuccessfulClientDelete(deleteClientResult: DeleteClientResult, actionResult: ForgotLockScreenViewModel.Result) =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withDeleteClientResult(deleteClientResult)
                .arrange()
            val result = viewModel.deleteCurrentClient("password")
            assert(result.isRight() && (result as Either.Right).value == actionResult)
        }
    @Test
    fun `given deleting client returns success, when deleting current client, then return Success`() =
        testSuccessfulClientDelete(DeleteClientResult.Success, ForgotLockScreenViewModel.Result.Success)
    @Test
    fun `given deleting client returns invalid credentials, when deleting current client, then return InvalidPassword`() =
        testSuccessfulClientDelete(DeleteClientResult.Failure.InvalidCredentials, ForgotLockScreenViewModel.Result.InvalidPassword)
    @Test
    fun `given deleting client returns failure, when deleting current client, then return failure`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withDeleteClientResult(DeleteClientResult.Failure.Generic(StorageFailure.DataNotFound))
                .arrange()
            val result = viewModel.deleteCurrentClient("password")
            assert(result.isLeft())
        }

    // sessions hard logout
    private fun Arrangement.verifyHardLogoutActions(logoutCalled: Boolean) {
        coVerify { observeEstablishedCallsUseCase() }
        coVerify { endCallUseCase(any()) }
        coVerify { globalDataStore.clearAppLockPasscode() }
        coVerify { accountSwitchUseCase(SwitchAccountParam.Clear) }
        val (atLeast, atMost) = if (logoutCalled) 1 to 1 else 0 to 0
        coVerify(atLeast = atLeast, atMost = atMost) { logoutUseCase(any(), any()) }
        coVerify(atLeast = atLeast, atMost = atMost) { notificationManager.stopObservingOnLogout(any()) }
        coVerify(atLeast = atLeast, atMost = atMost) { notificationChannelsManager.deleteChannelGroup(any()) }
        coVerify(atLeast = atLeast, atMost = atMost) { userDataStore.clear() }
    }
    private fun testSuccessfulLoggingOut(getSessionsResult: GetAllSessionsResult, logoutCalled: Boolean) = runTest(dispatcher.default()) {
            val (arrangement, viewModel) = Arrangement()
                .withGetSessionsResult(getSessionsResult)
                .arrange()
            val result = viewModel.hardLogoutAllAccounts()
            advanceUntilIdle()
            assert(result.isRight() && (result as Either.Right).value == ForgotLockScreenViewModel.Result.Success)
            arrangement.verifyHardLogoutActions(logoutCalled = logoutCalled)
    }
    @Test
    fun `given no sessions, when logging out, then make all required actions other than logout and return success`() =
        testSuccessfulLoggingOut(getSessionsResult = GetAllSessionsResult.Failure.NoSessionFound, logoutCalled = false)
    @Test
    fun `given no valid sessions, when logging out, then make all required actions other than logout and return success`() =
        testSuccessfulLoggingOut(getSessionsResult = GetAllSessionsResult.Success(listOf(INVALID_SESSION)), logoutCalled = false)
    @Test
    fun `given valid sessions, when logging out, then make all required actions with logout and return success`() =
        testSuccessfulLoggingOut(getSessionsResult = GetAllSessionsResult.Success(listOf(VALID_SESSION)), logoutCalled = true)
    @Test
    fun `given sessions return failure, when hard-logging out sessions, then return failure`() =
        runTest(dispatcher.default()) {
            val (_, viewModel) = Arrangement()
                .withGetSessionsResult(GetAllSessionsResult.Failure.Generic(StorageFailure.DataNotFound))
                .arrange()
            val result = viewModel.hardLogoutAllAccounts()
            assert(result.isLeft())
        }

    class Arrangement {
        @MockK lateinit var coreLogic: CoreLogic
        @MockK lateinit var userSessionScope: UserSessionScope
        @MockK lateinit var logoutUseCase: LogoutUseCase
        @MockK lateinit var globalDataStore: GlobalDataStore
        @MockK lateinit var userDataStoreProvider: UserDataStoreProvider
        @MockK lateinit var userDataStore: UserDataStore
        @MockK lateinit var notificationChannelsManager: NotificationChannelsManager
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
                coreLogic, globalDataStore, userDataStoreProvider, notificationChannelsManager, notificationManager, getSelfUserUseCase,
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
