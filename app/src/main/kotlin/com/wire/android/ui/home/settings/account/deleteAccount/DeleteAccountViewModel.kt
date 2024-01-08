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
package com.wire.android.ui.home.settings.account.deleteAccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    var state by mutableStateOf(DeleteAccountState())
        private set

    fun onDeleteAccountClicked() {
        state = state.copy(startDeleteAccountFlow = true)
    }

    fun onDeleteAccountDialogDismissed() {
        state = state.copy(startDeleteAccountFlow = false)
    }

    fun onDeleteAccountDialogConfirmed() {
        viewModelScope.launch {
            deleteAccount(null)
            state = state.copy(startDeleteAccountFlow = false)
        }
    }
}
