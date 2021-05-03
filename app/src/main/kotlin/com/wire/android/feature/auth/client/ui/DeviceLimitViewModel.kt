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
import com.wire.android.feature.auth.client.usecase.RegisterClientParams
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.shared.session.usecase.SetSessionCurrentUseCase
import com.wire.android.shared.session.usecase.SetSessionCurrentUseCaseParams

class DeviceLimitViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val setSessionCurrentUseCase: SetSessionCurrentUseCase,
    private val registerClientUseCase: RegisterClientUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _registerClientLiveData = SingleLiveEvent<Either<Failure, Unit>>()
    val registerClientLiveData: LiveData<Either<Failure, Unit>> = _registerClientLiveData

    fun registerClient(userId: String, password: String) {
        registerClientUseCase(viewModelScope, RegisterClientParams(password)) {
            it.onSuccess { setSessionCurrent(userId) }
                .onFailure { failure ->
                    _registerClientLiveData.failure(failure)
                }
        }
    }

    private fun setSessionCurrent(userId: String) {
        setSessionCurrentUseCase(viewModelScope, SetSessionCurrentUseCaseParams(userId)) {
            it.onSuccess { _registerClientLiveData.success() }
                .onFailure { failure ->
                    _registerClientLiveData.failure(failure)
                }
        }
    }
}
