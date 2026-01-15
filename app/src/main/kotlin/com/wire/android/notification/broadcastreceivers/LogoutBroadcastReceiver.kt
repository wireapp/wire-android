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

package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LogoutBroadcastReceiver : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    @ApplicationScope
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var userDataStoreProvider: UserDataStoreProvider

    @Inject
    lateinit var notificationManager: WireNotificationManager

    @Inject
    lateinit var globalDataStore: GlobalDataStore

    @Inject
    lateinit var accountSwitch: AccountSwitchUseCase

    @Inject
    lateinit var getSessions: GetSessionsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOGOUT) {
            appLogger.w("$TAG: onReceive, unexpected action: ${intent.action}")
            return
        }

        appLogger.i("$TAG: Received logout broadcast, initiating logout for all accounts")

        coroutineScope.launch(Dispatchers.Default) {
            try {
                hardLogoutAllAccounts()
                context.moveAppToBackground()
                appLogger.i("$TAG: Logout completed, app moved to background")
            } catch (e: Exception) {
                appLogger.e("$TAG: Error during logout: ${e.message}", e)
            }
        }
    }

    private suspend fun hardLogoutAllAccounts() {
        when (val getAllSessionsResult = getSessions()) {
            is GetAllSessionsResult.Failure.Generic -> {
                appLogger.e("$TAG: Failed to get all sessions: ${getAllSessionsResult.genericFailure}")
            }
            is GetAllSessionsResult.Failure.NoSessionFound,
            is GetAllSessionsResult.Success -> {
                val sessions = if (getAllSessionsResult is GetAllSessionsResult.Success) {
                    getAllSessionsResult.sessions
                } else {
                    emptyList()
                }

                val validSessions = sessions.filterIsInstance<AccountInfo.Valid>()
                appLogger.i("$TAG: Logging out ${validSessions.size} accounts")

                validSessions.map { session ->
                    coroutineScope.launch {
                        try {
                            hardLogoutAccount(session.userId)
                            appLogger.i("$TAG: Successfully logged out account: ${session.userId}")
                        } catch (e: Exception) {
                            appLogger.e("$TAG: Failed to logout account ${session.userId}: ${e.message}", e)
                        }
                    }
                }.joinAll()

                globalDataStore.clearAppLockPasscode()
                accountSwitch(SwitchAccountParam.Clear)
                appLogger.i("$TAG: Global state cleared")
            }
        }
    }

    private suspend fun hardLogoutAccount(userId: UserId) {
        notificationManager.stopObservingOnLogout(userId)
        coreLogic.getSessionScope(userId).logout(
            reason = LogoutReason.SELF_HARD_LOGOUT,
            waitUntilCompletes = true
        )
        userDataStoreProvider.getOrCreate(userId).clear()
    }

    private fun Context.moveAppToBackground() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    companion object {
        const val ACTION_LOGOUT = "com.wire.ACTION_LOGOUT"
        private const val TAG = "LogoutBroadcastReceiver"
    }
}
