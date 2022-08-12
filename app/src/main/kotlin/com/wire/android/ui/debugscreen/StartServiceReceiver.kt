package com.wire.android.ui.debugscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.di.NoSession
import com.wire.kalium.logic.feature.user.webSocketStatus.IsWebSocketEnabledUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StartServiceReceiver : BroadcastReceiver() {

    @Inject
    @NoSession
    lateinit var isWebSocketEnabledUseCase: IsWebSocketEnabledUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        val persistentWebSocketServiceIntent = Intent(context, PersistentWebSocketService::class.java)
        if (!isWebSocketEnabledUseCase()) {
            persistentWebSocketServiceIntent.action = PersistentWebSocketService.ACTION_STOP_FOREGROUND
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(persistentWebSocketServiceIntent)
        } else {
            context?.startService(persistentWebSocketServiceIntent)
        }
    }
}
