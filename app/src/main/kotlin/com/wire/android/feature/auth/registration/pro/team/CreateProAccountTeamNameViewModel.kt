package com.wire.android.feature.auth.registration.pro.team

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameParams
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase

class CreateProAccountTeamNameViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getTeamNameUseCase: GetTeamNameUseCase,
    private val updateTeamNameUseCase: UpdateTeamNameUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _teamNameLiveData = MutableLiveData<String>()
    val teamNameLiveData: LiveData<String> = _teamNameLiveData

    private val _confirmationButtonEnabledLiveData = MutableLiveData<Boolean>()
    val confirmationButtonEnabledLiveData: LiveData<Boolean> = _confirmationButtonEnabledLiveData

    init {
        getTeamName()
    }

    private fun getTeamName() =
        getTeamNameUseCase(viewModelScope, Unit) {
            it.onSuccess { teamName -> handleSuccess(teamName) }
        }

    fun afterTeamNameChanged(teamName: String) =
        updateTeamNameUseCase(viewModelScope, UpdateTeamNameParams(teamName))

    private fun handleSuccess(teamName: String) {
        _teamNameLiveData.value = teamName
    }

    fun onTeamNameTextChanged(teamNameInput: String) {
        _confirmationButtonEnabledLiveData.value = teamNameInput.isNotEmpty()
    }
}
