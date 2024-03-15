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
@file:Suppress("StringTemplate")

package com.wire.android.feature

import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.services.PersistentWebSocketService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartPersistentWebsocketIfNecessaryUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val shouldStartPersistentWebSocketService: ShouldStartPersistentWebSocketServiceUseCase
) {
    suspend operator fun invoke() {
        val persistentWebSocketServiceIntent = PersistentWebSocketService.newIntent(appContext)
        shouldStartPersistentWebSocketService().let {
            when (it) {
                is ShouldStartPersistentWebSocketServiceUseCase.Result.Failure -> {
                    appLogger.e("${TAG}: Failure while fetching persistent web socket status flow")
                }

                is ShouldStartPersistentWebSocketServiceUseCase.Result.Success -> {
                    if (it.shouldStartPersistentWebSocketService) {
                        startForegroundService(persistentWebSocketServiceIntent)
                    } else {
                        appLogger.i("${TAG}: Stopping PersistentWebsocketService, no user with persistent web socket enabled found")
                        appContext.stopService(persistentWebSocketServiceIntent)
                    }
                }
            }
        }
    }

    private fun startForegroundService(persistentWebSocketServiceIntent: Intent) {
        when {
            PersistentWebSocketService.isServiceStarted -> {
                appLogger.i("${TAG}: PersistentWebsocketService already started, not starting again")
            }

            else -> {
                appLogger.i("${TAG}: Starting PersistentWebsocketService")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(persistentWebSocketServiceIntent)
                } else {
                    appContext.startService(persistentWebSocketServiceIntent)
                }
            }
        }
    }

    companion object {
        const val TAG = "StartPersistentWebsocketIfNecessaryUseCase"
    }
}
