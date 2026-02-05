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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ForgotLockScreenViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val globalDataStore: GlobalDataStore,
    private val notificationManager: WireNotificationManager,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val getSessions: GetSessionsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
) : ViewModel() {

    var state: ForgotLockCodeViewState by mutableStateOf(ForgotLockCodeViewState())
        private set

    fun onErrorDismissed() {
        state = state.copy(error = null)
    }

    fun onLogoutConfirmed(shouldWipeData: Boolean) {
        viewModelScope.launch {
            state = state.copy(isLoggingOut = true, error = null)
            state = when (val result = logoutAllAccounts(shouldWipeData)) {
                is Result.Failure.Generic -> state.copy(error = result.cause, isLoggingOut = false)
                Result.Success -> {
                    state.copy(completed = true, isLoggingOut = false)
                }
            }
        }
    }

    private suspend fun logoutAllAccounts(shouldWipeData: Boolean): Result =
        when (val getAllSessionsResult = getSessions()) {
            is GetAllSessionsResult.Failure.Generic -> Result.Failure.Generic(getAllSessionsResult.genericFailure)
            is GetAllSessionsResult.Failure.NoSessionFound,
            is GetAllSessionsResult.Success -> {
                observeEstablishedCalls().firstOrNull()?.let { establishedCalls ->
                    establishedCalls.forEach { endCall(it.conversationId) }
                }
                val sessions = if (getAllSessionsResult is GetAllSessionsResult.Success) getAllSessionsResult.sessions else emptyList()
                val logoutReason = if (shouldWipeData) LogoutReason.SELF_HARD_LOGOUT else LogoutReason.SELF_SOFT_LOGOUT
                sessions.filterIsInstance<AccountInfo.Valid>().map { session ->
                    viewModelScope.launch {
                        logoutAccount(session.userId, logoutReason, shouldWipeData)
                    }
                }.joinAll() // wait until all accounts are logged out
                globalDataStore.clearAppLockPasscode()
                Result.Success
            }
        }

    private suspend fun logoutAccount(userId: UserId, logoutReason: LogoutReason, shouldWipeData: Boolean) {
        notificationManager.stopObservingOnLogout(userId)
        coreLogic.getSessionScope(userId).logout(reason = logoutReason, waitUntilCompletes = true)
        if (shouldWipeData) {
            userDataStoreProvider.getOrCreate(userId).clear()
        }
    }

    private sealed class Result {
        sealed class Failure : Result() {
            data class Generic(val cause: com.wire.kalium.common.error.CoreFailure) : Failure()
        }

        data object Success : Result()
    }

    companion object {
        const val TAG = "ForgotLockLogout"
    }
}
