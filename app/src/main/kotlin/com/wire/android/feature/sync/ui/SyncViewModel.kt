package com.wire.android.feature.sync.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.events.usecase.ListenToEventsUseCase
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler
import com.wire.android.feature.sync.slow.usecase.CheckSlowSyncRequiredUseCase

class SyncViewModel(
    private val listenToEventsUseCase: ListenToEventsUseCase,
    private val checkSlowSyncRequiredUseCase: CheckSlowSyncRequiredUseCase,
    private val slowSyncWorkHandler: SlowSyncWorkHandler,
    override val dispatcherProvider: DispatcherProvider
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _syncStatusLiveData = MutableLiveData<WorkInfo.State>()
    val syncStatusLiveData: LiveData<WorkInfo.State> = _syncStatusLiveData

    fun startListeningToMessages() {
        listenToEventsUseCase(viewModelScope, Unit) { }
    }

    fun startSync() {
        checkSlowSyncRequiredUseCase(viewModelScope, Unit) {
            it.onSuccess { required ->
                if (required) slowSync()
            }
        }
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
