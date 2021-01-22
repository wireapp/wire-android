package com.wire.android.feature.sync.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.work.WorkInfo
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler

class SyncViewModel(private val slowSyncWorkHandler: SlowSyncWorkHandler) : ViewModel() {

    private val _syncStatusLiveData = MutableLiveData<WorkInfo.State>()
    val syncStatusLiveData: LiveData<WorkInfo.State> = _syncStatusLiveData

    fun startSync() {
        //TODO start required sync type : slow/quick
        slowSync()
    }

    private fun slowSync() {
        slowSyncWorkHandler.enqueueWork().map { it.state }.observeForever {
            updateSyncStatus(it)
        }
    }

    private fun updateSyncStatus(status: WorkInfo.State) {
        _syncStatusLiveData.value = status
    }
}
