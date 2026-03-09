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
package com.wire.android.notification.broadcastreceivers

import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.ui.WireActivity
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LogoutReceiver : CoroutineReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var currentSession: CurrentSessionUseCase

    @Inject
    lateinit var deleteSession: DeleteSessionUseCase

    @Inject
    lateinit var accountSwitch: AccountSwitchUseCase

    @Inject
    @ApplicationScope
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var nomadProfilesFeatureConfig: NomadProfilesFeatureConfig

    override suspend fun receive(context: Context, intent: Intent) {
        if (!nomadProfilesFeatureConfig.isEnabled()) return
        if (intent.action != ACTION_LOGOUT) return

        appLogger.i("$TAG Received logout broadcast")

        val appContext = context.applicationContext
        coroutineScope.launch {
            performLogout(appContext)
        }
    }

    private suspend fun performLogout(context: Context) {
        when (val session = currentSession()) {
            is CurrentSessionResult.Success -> {
                val userId = session.accountInfo.userId
                appLogger.i("$TAG Logging out user: ${userId.toLogString()}")
                coreLogic.getSessionScope(userId).logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
                deleteSession(userId)
                accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
                val wireActivityIntent = Intent(context, WireActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                withContext(Dispatchers.Main.immediate) {
                    context.startActivity(wireActivityIntent)
                }
            }

            is CurrentSessionResult.Failure.SessionNotFound ->
                appLogger.i("$TAG No active session found, nothing to logout")

            is CurrentSessionResult.Failure.Generic ->
                appLogger.e("$TAG Failed to get current session")
        }
    }

    companion object {
        const val ACTION_LOGOUT = "com.wire.ACTION_LOGOUT"
        private const val TAG = "LogoutReceiver"
    }
}
