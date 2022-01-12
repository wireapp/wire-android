package com.wire.android.feature.sync.slow

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager

class SlowSyncWorkHandler(private val application: Application) {

    fun enqueueWork(): LiveData<WorkInfo> =
        WorkManager.getInstance(application).run {
            val workInfo = SlowSyncWorker.buildWork()
            beginUniqueWork(SlowSyncWorker.WORK_NAME, ExistingWorkPolicy.KEEP, workInfo).enqueue()
            getWorkInfoByIdLiveData(workInfo.id)
        }
}
