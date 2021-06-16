package com.wire.android.feature.sync.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.core.websocket.WebSocketService
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler
import com.wire.android.feature.sync.slow.usecase.CheckSlowSyncRequiredUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SyncViewModel(
    private val checkSlowSyncRequiredUseCase: CheckSlowSyncRequiredUseCase,
    private val slowSyncWorkHandler: SlowSyncWorkHandler,
    private val webSocketService: WebSocketService,
    override val dispatcherProvider: DispatcherProvider,
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _syncStatusLiveData = MutableLiveData<WorkInfo.State>()
    val syncStatusLiveData: LiveData<WorkInfo.State> = _syncStatusLiveData

    //TODO usage example, it will be called in ListenWebSocketUseCase
    fun establishConnection() {
        viewModelScope.launch {
            webSocketService.observeEvent().collect {
                Log.d("TAG", "establishConnection: $it")
            }
        }
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
