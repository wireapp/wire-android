package com.wire.android.feature.auth.client.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.session.usecase.SetCurrentSessionToDormantUseCase

class DeviceLimitViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val setCurrentSessionToDormantUseCase: SetCurrentSessionToDormantUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _isCurrentSessionDormantLiveData = MutableLiveData(false)
    val isCurrentSessionDormantLiveData: LiveData<Boolean> = _isCurrentSessionDormantLiveData

    fun clearSession() {
        setCurrentSessionToDormantUseCase(viewModelScope, Unit) {
            it.onSuccess { _isCurrentSessionDormantLiveData.value = true }
            it.onFailure { _isCurrentSessionDormantLiveData.value = false }
        }
    }
}
