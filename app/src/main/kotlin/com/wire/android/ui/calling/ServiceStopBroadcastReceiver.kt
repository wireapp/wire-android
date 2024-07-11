package com.wire.android.ui.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun coIO(runner: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch { runner.invoke((this)) }

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        coIO {
            if (intent.action == "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror") {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
        }
    }
}
