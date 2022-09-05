package com.wire.android.ui.debugscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.di.NoSession
import com.wire.android.services.PersistentWebSocketService
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartServiceReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    @Inject
    @NoSession
    lateinit var observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        val persistentWebSocketServiceIntent = PersistentWebSocketService.newIntent(context)
        scope.launch {
            if (!observePersistentWebSocketConnectionStatus().first()) {
                context?.stopService(persistentWebSocketServiceIntent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(persistentWebSocketServiceIntent)
                } else {
                    context?.startService(persistentWebSocketServiceIntent)
                }
            }
        }
    }
}
