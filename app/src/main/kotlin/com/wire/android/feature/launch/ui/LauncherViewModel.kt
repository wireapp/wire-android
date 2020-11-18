package com.wire.android.feature.launch.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.session.usecase.CheckCurrentSessionExistsUseCase

class LauncherViewModel(
    private val checkCurrentSessionExistsUseCase: CheckCurrentSessionExistsUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private val _currentSessionExistsLiveData = SingleLiveEvent<Boolean>()
    val currentSessionExistsLiveData: LiveData<Boolean> = _currentSessionExistsLiveData

    fun checkIfCurrentSessionExists() = checkCurrentSessionExistsUseCase(viewModelScope, Unit) {
        _currentSessionExistsLiveData.value = it.fold({ false }) { it }
    }
}
