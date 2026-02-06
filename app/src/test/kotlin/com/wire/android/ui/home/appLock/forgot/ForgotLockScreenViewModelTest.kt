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
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ForgotLockScreenViewModelTest {
    @Test
    fun `given sessions failure, when logout confirmed, then set error and do not clear app lock`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withGetSessionsResult(GetAllSessionsResult.Failure.Generic(StorageFailure.DataNotFound))
            .arrange()

        viewModel.onLogoutConfirmed(shouldWipeData = false)
        advanceUntilIdle()

        assertEquals(StorageFailure.DataNotFound, viewModel.state.error)
        assertEquals(false, viewModel.state.completed)
        assertEquals(false, viewModel.state.isLoggingOut)
        coVerify(exactly = 0) { arrangement.globalDataStore.clearAppLockPasscode() }
        coVerify(exactly = 0) { arrangement.accountSwitchUseCase(any()) }
    }

    @Test
    fun `given valid session and soft logout, when logout confirmed, then complete and keep user data`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withGetSessionsResult(GetAllSessionsResult.Success(listOf(VALID_SESSION)))
            .arrange()

        viewModel.onLogoutConfirmed(shouldWipeData = false)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.completed)
        assertEquals(null, viewModel.state.error)
        assertEquals(false, viewModel.state.isLoggingOut)
        coVerify { arrangement.logoutUseCase(LogoutReason.SELF_SOFT_LOGOUT, true) }
        coVerify { arrangement.notificationManager.stopObservingOnLogout(VALID_USER_ID) }
        coVerify { arrangement.globalDataStore.clearAppLockPasscode() }
        coVerify { arrangement.accountSwitchUseCase(SwitchAccountParam.Clear) }
        coVerify(exactly = 0) { arrangement.userDataStore.clear() }
    }

    @Test
    fun `given valid session and hard logout, when logout confirmed, then clear user data`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withGetSessionsResult(GetAllSessionsResult.Success(listOf(VALID_SESSION)))
            .arrange()

        viewModel.onLogoutConfirmed(shouldWipeData = true)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.completed)
        coVerify { arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true) }
        coVerify { arrangement.userDataStore.clear() }
    }

    @Test
    fun `given no sessions, when logout confirmed, then still complete and clear global app lock`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withGetSessionsResult(GetAllSessionsResult.Failure.NoSessionFound)
            .arrange()

        viewModel.onLogoutConfirmed(shouldWipeData = false)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.completed)
        coVerify { arrangement.globalDataStore.clearAppLockPasscode() }
        coVerify { arrangement.accountSwitchUseCase(SwitchAccountParam.Clear) }
        coVerify(exactly = 0) { arrangement.logoutUseCase(any(), any()) }
    }

    class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic
        @MockK
        lateinit var userSessionScope: UserSessionScope
        @MockK
        lateinit var logoutUseCase: LogoutUseCase
        @MockK
        lateinit var globalDataStore: GlobalDataStore
        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider
        @MockK
        lateinit var userDataStore: UserDataStore
        @MockK
        lateinit var notificationManager: WireNotificationManager
        @MockK
        lateinit var getSessionsUseCase: GetSessionsUseCase
        @MockK
        lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase
        @MockK
        lateinit var endCallUseCase: EndCallUseCase
        @MockK
        lateinit var accountSwitchUseCase: AccountSwitchUseCase

        private val viewModel: ForgotLockScreenViewModel by lazy {
            ForgotLockScreenViewModel(
                coreLogic = coreLogic,
                globalDataStore = globalDataStore,
                notificationManager = notificationManager,
                userDataStoreProvider = userDataStoreProvider,
                getSessions = getSessionsUseCase,
                observeEstablishedCalls = observeEstablishedCallsUseCase,
                endCall = endCallUseCase,
                accountSwitch = accountSwitchUseCase,
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every { userSessionScope.logout } returns logoutUseCase
            every { userDataStoreProvider.getOrCreate(any()) } returns userDataStore
            coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf(mockCall()))
            coEvery { accountSwitchUseCase(any()) } returns SwitchAccountResult.NoOtherAccountToSwitch
        }

        fun withGetSessionsResult(result: GetAllSessionsResult) =
            apply { coEvery { getSessionsUseCase() } returns result }

        fun arrange() = this to viewModel

        private fun mockCall(): Call {
            val call: Call = mockk()
            every { call.conversationId } returns CONVERSATION_ID
            return call
        }
    }

    companion object {
        private val VALID_USER_ID = UserId("id", "domain")
        private val VALID_SESSION = AccountInfo.Valid(VALID_USER_ID)
        private val CONVERSATION_ID = ConversationId("conversation-id", "domain")
    }
}
