package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_SETTINGS_DISPLAY_NAME_CHANGED
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChangeDisplayNameViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val updateDisplayName: UpdateDisplayNameUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var displayNameState: DisplayNameState by mutableStateOf(DisplayNameState())
        private set

    init {
        viewModelScope.launch {
            getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1)).collect {
                displayNameState = displayNameState.copy(
                    originalDisplayName = it.name.orEmpty(),
                    displayName = TextFieldValue(it.name.orEmpty())
                )
            }
        }
    }

    fun onNameChange(newText: TextFieldValue) {
        displayNameState = validateNewNameChange(newText)
    }

    private fun validateNewNameChange(newText: TextFieldValue): DisplayNameState {
        val cleanText = newText.text.trim()
        return when {
            cleanText.isEmpty() -> {
                displayNameState.copy(
                    animatedNameError = true,
                    displayName = newText,
                    continueEnabled = false,
                    error = DisplayNameState.NameError.TextFieldError.NameEmptyError
                )
            }
            cleanText.count() > NAME_MAX_COUNT -> {
                displayNameState.copy(
                    animatedNameError = true,
                    displayName = newText,
                    continueEnabled = false,
                    error = DisplayNameState.NameError.TextFieldError.NameExceedLimitError
                )
            }
            cleanText == displayNameState.originalDisplayName -> {
                displayNameState.copy(
                    animatedNameError = false,
                    displayName = newText,
                    continueEnabled = false,
                    error = DisplayNameState.NameError.None
                )
            }
            else -> {
                displayNameState.copy(
                    animatedNameError = false,
                    displayName = newText,
                    continueEnabled = true,
                    error = DisplayNameState.NameError.None
                )
            }
        }
    }

    fun saveDisplayName() {
        viewModelScope.launch {
            when (updateDisplayName(displayNameState.displayName.text)) {
                is DisplayNameUpdateResult.Failure -> navigateBack(mapOf(EXTRA_SETTINGS_DISPLAY_NAME_CHANGED to false))
                is DisplayNameUpdateResult.Success -> navigateBack(mapOf(EXTRA_SETTINGS_DISPLAY_NAME_CHANGED to true))
            }
        }
    }

    fun onNameErrorAnimated() {
        displayNameState = displayNameState.copy(animatedNameError = false)
    }

    fun navigateBack(args: Map<String, Boolean> = mapOf()) {
        viewModelScope.launch { navigationManager.navigateBack(args) }
    }

    companion object {
        private const val NAME_MAX_COUNT = 64
    }
}
