package com.wire.android.feature.auth.registration.pro.email

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor

class CreateProAccountTeamEmailViewModel(
    override val dispatcherProvider: DispatcherProvider
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _teamEmailLiveData = SingleLiveEvent<String>()
    val teamEmailLiveData: SingleLiveEvent<String> = _teamEmailLiveData

    private val _confirmationButtonEnabled = MutableLiveData<Boolean>()
    val confirmationButtonEnabled: LiveData<Boolean> = _confirmationButtonEnabled

    init {
        getTeamEmail()
    }

    private fun getTeamEmail() {
        //TODO call team email use-case and return email
    }

    fun afterTeamEmailChanged(teamEmail: String) {
        //TODO call update team email use-case
    }

    private fun handleSuccess(teamEmail: String) {
        _teamEmailLiveData.value = teamEmail
    }

    fun onTeamEmailTextChanged(teamEmailInput: String) {
        _confirmationButtonEnabled.value = teamEmailInput.isNotEmpty()
    }
}
