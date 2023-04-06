/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

package com.wire.android.ui.common.snackbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.ui.UIText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SnackBarState(
    val showSnackBar: Boolean = false,
    val message: UIText? = null
)

@HiltViewModel
class SnackBarViewModel @Inject constructor(private val showSnackBar: ShowSnackBarUseCase) : ViewModel() {

    private val _snackBarMessage = MutableSharedFlow<SnackBarState>()
    val snackBarMessage = _snackBarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            showSnackBar.observerSnackBarStatus.collect { result ->
                result.message?.let {
                    _snackBarMessage.emit(SnackBarState(message = it))
                }
            }
        }
    }
}
