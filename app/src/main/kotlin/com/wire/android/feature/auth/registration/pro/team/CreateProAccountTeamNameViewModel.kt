package com.wire.android.feature.auth.registration.pro.team

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.accessibility.AccessibilityManagerWrapper
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameParams
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase

class CreateProAccountTeamNameViewModel(
        private val getTeamNameUseCase: GetTeamNameUseCase,
        private val updateTeamNameUseCase: UpdateTeamNameUseCase,
        private val accessibilityManagerWrapper: AccessibilityManagerWrapper
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private var _urlLiveData = MutableLiveData<String>()
    val urlLiveData: LiveData<String> = _urlLiveData

    private var _teamNameLiveData = MutableLiveData<String>()
    val teamNameLiveData: LiveData<String> = _teamNameLiveData

    private var _confirmationButtonEnabled = MutableLiveData<Boolean>()
    val confirmationButtonEnabled: LiveData<Boolean> = _confirmationButtonEnabled

    private var _textInputFocusedLiveData = MutableLiveData<Unit>()
    val textInputFocusedLiveData: LiveData<Unit> = _textInputFocusedLiveData

    init {
        requestFocusForInput()
        getTeamName()
    }

    private fun requestFocusForInput() {
        if (!accessibilityManagerWrapper.isTalkbackEnabled()) {
            _textInputFocusedLiveData.value = Unit
        }
    }

    private fun getTeamName() =
        getTeamNameUseCase(viewModelScope, Unit) {
            it.onSuccess { teamName -> handleSuccess(teamName) }
        }

    fun onAboutButtonClicked() {
        _urlLiveData.value = "$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX"
    }

    fun afterTeamNameChanged(teamName: String) = updateTeamNameUseCase(viewModelScope, UpdateTeamNameParams(teamName))

    private fun handleSuccess(teamName: String) {
        _teamNameLiveData.value = teamName
    }

    fun onTeamNameTextChanged(teamNameInput: String) {
        _confirmationButtonEnabled.value = teamNameInput.isNotEmpty()
    }

    companion object {
        //TODO need to get the url prefix from Config (default.json)
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
