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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.DisplayNameState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeDisplayNameViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val updateDisplayName: UpdateDisplayNameUseCase,
) : ViewModel() {

    val textState: TextFieldState = TextFieldState()
    var displayNameState: DisplayNameState by mutableStateOf(DisplayNameState())
        private set

    init {
        viewModelScope.launch {
            getSelf()?.name.orEmpty().let { currentDisplayName ->
                textState.setTextAndPlaceCursorAtEnd(currentDisplayName)
                textState.textAsFlow().collectLatest {
                    displayNameState = displayNameState.copy(
                        saveEnabled = it.trim().isNotEmpty() && it.length <= NAME_MAX_COUNT && it.trim() != currentDisplayName,
                        error = when {
                            it.trim().isEmpty() -> DisplayNameState.NameError.TextFieldError.NameEmptyError
                            it.length > NAME_MAX_COUNT -> DisplayNameState.NameError.TextFieldError.NameExceedLimitError
                            else -> DisplayNameState.NameError.None
                        }
                    )
                }
            }
        }
    }

    fun saveDisplayName() {
        displayNameState = displayNameState.copy(loading = true)
        viewModelScope.launch {
            updateDisplayName(textState.text.toString().trim())
                .let {
                    displayNameState = displayNameState.copy(
                        loading = false,
                        completed = when (it) {
                            is DisplayNameUpdateResult.Failure -> DisplayNameState.Completed.Failure
                            is DisplayNameUpdateResult.Success -> DisplayNameState.Completed.Success
                        }
                    )
                }
        }
    }

    companion object {
        const val NAME_MAX_COUNT = 64
    }
}
