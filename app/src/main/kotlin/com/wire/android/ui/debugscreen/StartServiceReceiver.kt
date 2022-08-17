package com.wire.android.ui.debugscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.di.NoSession
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartServiceReceiver : BroadcastReceiver() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    @NoSession
    lateinit var observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        val persistentWebSocketServiceIntent = Intent(context, PersistentWebSocketService::class.java)
        scope.launch {
            observePersistentWebSocketConnectionStatus().collect {
                if (!it) {
                    persistentWebSocketServiceIntent.action = PersistentWebSocketService.ACTION_STOP_FOREGROUND
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(persistentWebSocketServiceIntent)
                } else {
                    context?.startService(persistentWebSocketServiceIntent)
                }
            }
        }


    }
}
