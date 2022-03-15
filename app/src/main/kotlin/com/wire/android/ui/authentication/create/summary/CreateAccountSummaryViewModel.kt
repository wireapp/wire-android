package com.wire.android.ui.authentication.create.summary

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CREATE_ACCOUNT_FLOW_TYPE
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.CreateAccountFlowType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreateAccountSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {
    private val type: CreateAccountFlowType = checkNotNull(
        CreateAccountFlowType.fromRouteArg(savedStateHandle.getLiveData<String>(EXTRA_CREATE_ACCOUNT_FLOW_TYPE).value)
    ) { "Unknown CreateAccountFlowType" }
    var state: CreateAccountSummaryViewState by mutableStateOf(CreateAccountSummaryViewState(type))
        private set

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    fun navigateHome() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }
}
