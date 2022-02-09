package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.mock.mockMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ConversationViewModel @Inject constructor(
    //TODO: here we can extract the ID provided to the screen and fetch the data for the conversation
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationViewState())

    val conversationViewState: StateFlow<ConversationViewState>
        get() = _state

    init {
        _state.value = ConversationViewState(
            conversationName = "Some test conversation",
            messages = mockMessages
        )
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }
}
