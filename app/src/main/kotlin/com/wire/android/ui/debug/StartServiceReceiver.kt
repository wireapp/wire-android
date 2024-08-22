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

package com.wire.android.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This BroadcastReceiver will restart the persistentWebSocket Service after restarting the device.
 */
@AndroidEntryPoint
class StartServiceReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var startPersistentWebSocketService: StartPersistentWebsocketIfNecessaryUseCase

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        appLogger.i("$TAG: onReceive called with action ${intent?.action}")
        scope.launch { startPersistentWebSocketService() }
    }

    companion object {
        const val TAG = "StartServiceReceiver"
    }
}
