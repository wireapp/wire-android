package com.wire.android.feature.auth.registration.personal.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.user.name.ValidateNameParams
import com.wire.android.shared.user.name.ValidateNameUseCase

class CreatePersonalAccountEmailNameViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val validateNameUseCase: ValidateNameUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _continueEnabled = SingleLiveEvent<Boolean>()
    val continueEnabled: LiveData<Boolean> = _continueEnabled

    fun validateName(name: String) = validateNameUseCase(viewModelScope, ValidateNameParams(name)) {
        _continueEnabled.value = it.isRight
    }
}
