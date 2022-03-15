package com.wire.android.ui.authentication.create.username

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CREATE_ACCOUNT_USERNAME_FLOW_TYPE
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountUsernameFlowType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreateAccountUsernameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
): ViewModel() {
    private val type: CreateAccountUsernameFlowType = checkNotNull(
        CreateAccountUsernameFlowType.fromRouteArg(savedStateHandle.getLiveData<String>(EXTRA_CREATE_ACCOUNT_USERNAME_FLOW_TYPE).value)
    ) { "Unknown CreateAccountUsernameFlowType" }
    var state: CreateAccountUsernameViewState by mutableStateOf(CreateAccountUsernameViewState(type))
        private set

    fun onUsernameChange(newText: TextFieldValue) {
        state = state.copy(
            username = newText,
            error = CreateAccountUsernameViewState.UsernameError.None,
            continueEnabled = newText.text.isNotEmpty() && !state.loading)
    }

    fun onContinue() {
        state = state.copy(loading = true)
        viewModelScope.launch {
            //TODO change username request
            state = state.copy(loading = false)
            navigationManager.navigate(
                when (type) {
                    CreateAccountUsernameFlowType.CreatePersonalAccount -> NavigationCommand(
                        NavigationItem.CreateSummary.getRouteWithArgs(listOf(CreateAccountFlowType.CreatePersonalAccount))
                    )
                    CreateAccountUsernameFlowType.CreateTeam -> NavigationCommand(
                        NavigationItem.CreateSummary.getRouteWithArgs(listOf(CreateAccountFlowType.CreateTeam))
                    )
                    CreateAccountUsernameFlowType.AppStart -> NavigationCommand(
                        NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE
                    )
                }
            )
        }
    }
}
