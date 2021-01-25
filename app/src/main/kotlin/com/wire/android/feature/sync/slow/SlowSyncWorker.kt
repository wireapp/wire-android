package com.wire.android.feature.sync.slow

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.feature.sync.conversation.usecase.SyncConversationsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

//https://wearezeta.atlassian.net/wiki/spaces/ENGINEERIN/pages/15566946/Full+state+synchronization
class SlowSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncConversationsUseCase by inject<SyncConversationsUseCase>()

    private val dispatcherProvider by inject<DispatcherProvider>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        syncConversationsUseCase.run(Unit).fold({ Result.failure() }) { Result.success() }!!
    }

    companion object {
        const val WORK_NAME = "SlowSyncWork"

        fun buildWork(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SlowSyncWorker>()
                .setConstraints(constraints)
                .build()
        }
    }
}
