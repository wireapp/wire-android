/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeDisplayNameViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val updateDisplayName: UpdateDisplayNameUseCase,
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

    fun saveDisplayName(
        onFailure: () -> Unit,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            when (updateDisplayName(displayNameState.displayName.text)) {
                is DisplayNameUpdateResult.Failure -> onFailure()
                is DisplayNameUpdateResult.Success -> onSuccess()
            }
        }
    }

    fun onNameErrorAnimated() {
        displayNameState = displayNameState.copy(animatedNameError = false)
    }

    companion object {
        private const val NAME_MAX_COUNT = 64
    }
}
