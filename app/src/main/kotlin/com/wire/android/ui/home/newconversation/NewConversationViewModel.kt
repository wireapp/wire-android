package com.wire.android.ui.home.newconversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewConversationViewModel
@Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

}
