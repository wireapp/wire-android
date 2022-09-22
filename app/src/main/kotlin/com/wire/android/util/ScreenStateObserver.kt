package com.wire.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.wire.android.appLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenStateObserver @Inject constructor(@ApplicationContext val context: Context): BroadcastReceiver() {

    private val _screenStateFlow = MutableStateFlow(true)
    val screenStateFlow = _screenStateFlow.asStateFlow()

    init {
//        val pm: PowerManager? = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
//        _screenStateFlow.value = pm?.isInteractive ?: true

//        context.registerReceiver(this, IntentFilter().apply {
//            addAction(Intent.ACTION_SCREEN_OFF)
//            addAction(Intent.ACTION_SCREEN_ON)
//        })
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        if(p1?.action == Intent.ACTION_SCREEN_OFF) {
            _screenStateFlow.value = false
        }
        if(p1?.action == Intent.ACTION_SCREEN_ON) {
            _screenStateFlow.value = true
        }
        appLogger.i("$TAG ${p1?.action}")
    }

    companion object {
        const val TAG = "ScreenStateObserver"
    }
}

