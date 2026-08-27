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

package com.wire.android.ui.authentication.devices.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

class ClearSessionViewModel @AssistedInject constructor(
    private val currentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val switchAccount: AccountSwitchUseCase,
    private val logout: LogoutUseCase,
    @Assisted private val cancelUserId: UserId? = null,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(cancelUserId: UserId?): ClearSessionViewModel
    }

    var state: ClearSessionState by mutableStateOf(
        ClearSessionState(showCancelLoginDialog = false)
    )
        private set

    fun onBackButtonClicked() {
        state = state.copy(showCancelLoginDialog = true)
    }

    fun onProceedLoginClicked() {
        state = state.copy(showCancelLoginDialog = false)
    }

    fun onCancelLoginClicked(switchAccountActions: SwitchAccountActions) {
        state = state.copy(showCancelLoginDialog = false)
        viewModelScope.launch {
            cancelLogin(switchAccountActions)
        }
    }

    internal suspend fun cancelLogin(switchAccountActions: SwitchAccountActions) {
        val userId = cancelUserId ?: currentSessionUserId()
        // Select the account to return to while the unfinished login is still the current
        // session. If logout runs first, current-session observation briefly becomes empty and
        // Navigation 3 can dispose this route before the account switch is attempted.
        try {
            val switchResult = switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)
            // Remove the session-backed authentication route before deleting the account that owns
            // its Metro graph. Navigation 3 can keep an outgoing entry composed for its exit
            // transition; deleting first would make that entry recreate a graph for a session that
            // no longer exists.
            switchResult.callAction(switchAccountActions)
        } finally {
            withContext(NonCancellable) {
                if (userId != null) {
                    // logout to cancel all session-related actions, remove all sensitive data and free up resources
                    logout(reason = LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
                    // delete the session to make it seem like the session was never logged in
                    deleteSession(userId)
                }
            }
        }
    }

    private suspend fun currentSessionUserId(): UserId? =
        when (val result = currentSession()) {
            is CurrentSessionResult.Success -> result.accountInfo.userId
            is CurrentSessionResult.Failure.Generic -> {
                appLogger.e("$TAG: failed to get current session")
                null
            }
            CurrentSessionResult.Failure.SessionNotFound -> {
                appLogger.e("$TAG: session not found")
                null
            }
        }

    companion object {
        private const val TAG = "ClearSessionViewModel"
    }
}
