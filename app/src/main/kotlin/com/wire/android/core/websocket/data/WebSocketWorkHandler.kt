package com.wire.android.core.websocket.data

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

class WebSocketWorkHandler(private val application: Application) {
    fun run() {
        WorkManager.getInstance(application).run {
            val workInfo = WebSocketWorker.buildWork()
            beginUniqueWork(WebSocketWorker.WORK_NAME, ExistingWorkPolicy.KEEP, workInfo).enqueue()
        }
    }
}
