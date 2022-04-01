package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_INTERNAL_STATUS
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    init {
        savedStateHandle.get<String>(EXTRA_USER_ID)?.let { id ->
            appLogger.d("user id $id")
            //TODO: here we need to get the user from the Kalium
        }

        //TODO: internal is here untill we can get the ConnectionStatus from the user
        // for now it is just to be able to proceed forward
        val internalStatus = savedStateHandle.get<String>(EXTRA_INTERNAL_STATUS)

        val booleanStatus = internalStatus == "true"

        state = state.copy(
            isAvatarLoading = false,
            fullName = "Kim",
            userName = "Dawson",
            teamName = "AWESOME TEAM NAME",
            email = "kim.dawson@gmail.com",
            phone = "+49 123 456 000",
            connectionStatus = if (booleanStatus) ConnectionStatus.Connected else ConnectionStatus.NotConnected(false)
        )
    }

    fun openConversation() {
        //TODO: fire a use case
    }

    fun sendConnectionRequest() {
        //TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(true))
    }

    fun cancelConnectionRequest() {
        //TODO: fire a use case
        state = state.copy(connectionStatus = ConnectionStatus.NotConnected(false))
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
