package com.wire.android.ui.authentication.devices.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClearSessionViewModel @Inject constructor(
    private val currentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val switchAccount: AccountSwitchUseCase
) : ViewModel() {
    var state: ClearSessionState by mutableStateOf(
        ClearSessionState(showCancelLoginDialog = false)
    )
        private set

    fun onBackButtonClicked() {
        state = state.copy(showCancelLoginDialog = true)
    }

    fun onProceedLoginClicked() {
        state = state.copy(showCancelLoginDialog = false)
    }

    fun onCancelLoginClicked() {
        state = state.copy(showCancelLoginDialog = false)
        viewModelScope.launch {
            currentSession().let {
                when (it) {
                    is CurrentSessionResult.Success -> {
                        deleteSession(it.accountInfo.userId)
                    }
                    is CurrentSessionResult.Failure.Generic -> {
                        appLogger.e("failed to delete session")
                    }
                    CurrentSessionResult.Failure.SessionNotFound -> {
                        appLogger.e("session not found")
                    }
                }
            }
        }.invokeOnCompletion {
            viewModelScope.launch {
                switchAccount(
                    SwitchAccountParam.SwitchToNextAccountOrWelcome
                )
            }
        }
    }
}
