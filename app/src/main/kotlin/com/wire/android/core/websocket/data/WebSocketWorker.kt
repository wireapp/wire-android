package com.wire.android.core.websocket.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wire.android.WireApplication
import com.wire.android.core.storage.cache.CacheGateway
import org.koin.core.KoinComponent
import org.koin.core.inject

class WebSocketWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params), KoinComponent {

    private val cacheGateway by inject<CacheGateway>()
    private val webSocketConnection by inject<WebSocketConnection>()

    override fun doWork(): Result {
        val isAppInBackground = cacheGateway.load(WireApplication.IS_IN_BACKGROUND) as Boolean
        if (isAppInBackground)
            return Result.failure()

        if (!webSocketConnection.isConnected)
            webSocketConnection.connect()

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "WebSocketSyncWork"

        fun buildWork(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<WebSocketWorker>()
                .setConstraints(constraints)
                .build()
        }
    }
}
