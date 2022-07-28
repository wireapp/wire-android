package com.wire.android.ui.authentication.create.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AssistedViewModel
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountSummaryViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel(), AssistedViewModel<CreateAccountFlowType> {

    val type: CreateAccountFlowType get() = param

    var summaryState: CreateAccountSummaryViewState by mutableStateOf(CreateAccountSummaryViewState(type))
        private set

    fun onSummaryContinue() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(VoyagerNavigationItem.CreateUsername, BackStackMode.CLEAR_WHOLE)
            )
        }
    }
}
