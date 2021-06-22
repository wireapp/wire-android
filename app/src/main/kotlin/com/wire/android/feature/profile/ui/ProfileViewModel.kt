package com.wire.android.feature.profile.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.team.Team
import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import com.wire.android.shared.team.usecase.GetUserTeamUseCaseParams
import com.wire.android.shared.team.usecase.NotATeamUser
import com.wire.android.shared.user.User
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase

class ProfileViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserTeamUseCase: GetUserTeamUseCase,
) : ViewModel(),
    UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _errorLiveData = SingleLiveEvent<ErrorMessage>()
    val errorLiveData: LiveData<ErrorMessage> = _errorLiveData

    private val _currentUserLiveData = SingleLiveEvent<User>()
    val currentUserLiveData: LiveData<User> = _currentUserLiveData

    private val _teamLiveData = SingleLiveEvent<Team?>()
    val teamNameLiveData: LiveData<String?> = _teamLiveData.map { it?.name }

    fun fetchProfileInfo() = fetchCurrentUserInfo()

    private fun fetchCurrentUserInfo() =
        getCurrentUserUseCase(viewModelScope, Unit) {
            it.onFailure(::handleFailure)
                .onSuccess(::handleUserData)
        }

    private fun handleUserData(user: User) {
        _currentUserLiveData.value = user
        fetchUserTeamInfo(user)
    }

    private fun fetchUserTeamInfo(user: User) =
        getUserTeamUseCase(viewModelScope, GetUserTeamUseCaseParams(user)) {
            it.onFailure(::handleTeamFailure)
                .onSuccess { team -> updateTeam(team) }
        }

    private fun updateTeam(team: Team?) {
        _teamLiveData.value = team
    }

    private fun handleTeamFailure(failure: Failure) {
        if (failure is NotATeamUser) updateTeam(null)
        else handleFailure(failure)
    }

    private fun handleFailure(failure: Failure) {
        //TODO: more granular error messages
        _errorLiveData.value = GeneralErrorMessage
    }
}
