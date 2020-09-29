package com.wire.android.feature.launch.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.session.usecase.HasCurrentSessionUseCase

class LauncherViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val hasCurrentSessionUseCase: HasCurrentSessionUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _hasCurrentSessionLiveData = SingleLiveEvent<Boolean>()
    val hasCurrentSessionLiveData: LiveData<Boolean> = _hasCurrentSessionLiveData

    fun checkCurrentSessionExists() = hasCurrentSessionUseCase(viewModelScope, Unit) {
        _hasCurrentSessionLiveData.value = it.fold({ false }) { it }
    }
}
