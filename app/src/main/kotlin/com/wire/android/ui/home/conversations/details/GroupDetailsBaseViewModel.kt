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
 *
 *
 */

package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class GroupDetailsBaseViewModel(savedStateHandle: SavedStateHandle) : SavedStateViewModel(savedStateHandle) {

    private val _snackBarMessenger = MutableSharedFlow<UIText>()
    val snackBarMessage = _snackBarMessenger.asSharedFlow()

    suspend fun showSnackBarMessage(message: UIText) {
        _snackBarMessenger.emit(message)
    }

}
