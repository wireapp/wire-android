package com.wire.android.ui.userprofile.other

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private companion object {
        const val USER_ID = "user_id"
    }

    val state: OtherUserProfileScreenState by mutableStateOf(OtherUserProfileScreenState())

    init {
        savedStateHandle.get<String>(USER_ID)?.let { id ->
            Log.d("TEST", "user id $id")
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
