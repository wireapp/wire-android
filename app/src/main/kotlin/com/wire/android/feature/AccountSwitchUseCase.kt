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

package com.wire.android.feature

import com.wire.android.di.ApplicationScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class AccountSwitchUseCase @Inject constructor(
    private val updateCurrentSession: UpdateCurrentSessionUseCase,
    private val navigationManager: NavigationManager,
    private val getSessions: GetSessionsUseCase,
    private val getCurrentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val serverConfigForAccountUseCase: ServerConfigForAccountUseCase,
    @ApplicationScope private val coroutineScope: CoroutineScope
) {

    val currentAccount
        get() = when (val result = getCurrentSession()) {
            is CurrentSessionResult.Failure.Generic -> null
            CurrentSessionResult.Failure.SessionNotFound -> null
            is CurrentSessionResult.Success -> result.accountInfo
        }

    suspend operator fun invoke(params: SwitchAccountParam) {
        val current = currentAccount
        when (params) {
            is SwitchAccountParam.SwitchToAccount -> switch(params.userId, current)
            SwitchAccountParam.SwitchToNextAccountOrWelcome -> switchToNextAccountOrWelcome(current)
        }
    }

    private suspend fun switchToNextAccountOrWelcome(current: AccountInfo?) {

        val nextSessionId: UserId? = getSessions().let {
            when (it) {
                is GetAllSessionsResult.Failure.Generic -> null
                GetAllSessionsResult.Failure.NoSessionFound -> null
                is GetAllSessionsResult.Success ->
                    it.sessions.firstOrNull { accountInfo ->
                        (accountInfo is AccountInfo.Valid) && (accountInfo.userId != current?.userId)
                    }?.userId
            }
        }
        switch(nextSessionId, current)
    }

    private suspend fun switch(userId: UserId?, current: AccountInfo?) {
        val navigationDistention = (userId?.let { NavigationItem.Home } ?: run {
            // if there are no more accounts, we need to change the auth server config to the one of the current user
            current?.let { updateAuthServer(it.userId) }
            NavigationItem.Welcome
        }).getRouteWithArgs()

        when (updateCurrentSession(userId)) {
            is UpdateCurrentSessionUseCase.Result.Success -> {
                navigationManager.navigate(
                    NavigationCommand(
                        navigationDistention,
                        BackStackMode.CLEAR_WHOLE
                    )
                )
            }
            is UpdateCurrentSessionUseCase.Result.Failure -> {
                return
            }
        }
        current?.also {
            handleOldSession(it)
        }
    }

    private suspend fun updateAuthServer(current: UserId) {
        serverConfigForAccountUseCase(current).let {
            when (it) {
                is ServerConfigForAccountUseCase.Result.Success -> authServerConfigProvider.updateAuthServer(it.config)
                is ServerConfigForAccountUseCase.Result.Failure -> return
            }
        }

    }

    private fun handleOldSession(oldSession: AccountInfo) {
        when (oldSession) {
            is AccountInfo.Valid -> {
                // do nothing
            }
            is AccountInfo.Invalid -> coroutineScope.launch {
                withTimeout(DELETE_USER_SESSION_TIMEOUT) {
                    handleInvalidSession(oldSession)
                }
            }
        }
    }

    private suspend fun handleInvalidSession(invalidAccount: AccountInfo.Invalid) {
        when (invalidAccount.logoutReason) {
            LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                deleteSession(invalidAccount.userId)
            }
            LogoutReason.DELETED_ACCOUNT,
            LogoutReason.REMOVED_CLIENT,
            LogoutReason.SESSION_EXPIRED -> deleteSession(invalidAccount.userId)
        }
    }

    private companion object {
        const val DELETE_USER_SESSION_TIMEOUT = 3000L
    }
}

sealed class SwitchAccountParam {
    object SwitchToNextAccountOrWelcome : SwitchAccountParam()
    data class SwitchToAccount(val userId: UserId) : SwitchAccountParam()
}
