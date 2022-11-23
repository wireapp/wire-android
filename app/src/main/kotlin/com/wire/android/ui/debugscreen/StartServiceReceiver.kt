package com.wire.android.ui.debugscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.services.PersistentWebSocketService
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartServiceReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val persistentWebSocketServiceIntent = PersistentWebSocketService.newIntent(context)
        appLogger.e("persistent web socket receiver")
        scope.launch {
            coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().collect {
                if (it.map { it.isPersistentWebSocketEnabled }.contains(true)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context?.startForegroundService(persistentWebSocketServiceIntent)
                    } else {
                        context?.startService(persistentWebSocketServiceIntent)
                    }
                } else {
                    context?.stopService(persistentWebSocketServiceIntent)
                }
            }
        }
    }
}
