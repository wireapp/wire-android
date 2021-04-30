package com.wire.android.feature.auth.client.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.client.usecase.DevicesLimitReached
import com.wire.android.feature.auth.client.usecase.RegisterClientParams
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.shared.session.usecase.SetDormantSessionToCurrentUseCase
import com.wire.android.shared.session.usecase.SetDormantSessionToCurrentUseCaseParams

class DeviceLimitViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val setDormantSessionToCurrentUseCase: SetDormantSessionToCurrentUseCase,
    private val registerClientUseCase: RegisterClientUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _isDormantSessionCurrentLiveData = SingleLiveEvent<Either<Failure, Unit>>()
    val isDormantSessionCurrentLiveData: SingleLiveEvent<Either<Failure, Unit>> = _isDormantSessionCurrentLiveData

    private val _registerClientLiveData = SingleLiveEvent<Either<Unit, Unit>>()
    val registerClientLiveData: LiveData<Either<Unit, Unit>> = _registerClientLiveData

    fun registerClient(password: String) {
        registerClientUseCase(viewModelScope, RegisterClientParams(password)) {
            it.onSuccess {
                _registerClientLiveData.success()
            }
            it.onFailure { failure ->
                if (failure is DevicesLimitReached) _registerClientLiveData.failure(Unit)
            }
        }
    }

    fun setDormantSessionToCurrent(userId: String) {
        setDormantSessionToCurrentUseCase(viewModelScope, SetDormantSessionToCurrentUseCaseParams(userId)) {
            it.onSuccess { _isDormantSessionCurrentLiveData.success() }
            it.onFailure { failure -> _isDormantSessionCurrentLiveData.failure(failure) }
        }
    }
}
