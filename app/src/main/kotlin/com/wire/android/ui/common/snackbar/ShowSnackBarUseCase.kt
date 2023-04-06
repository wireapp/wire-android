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

import com.wire.android.util.ui.UIText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

/**
 * Provides a way to control flow of messages for snackBar
 */
@Singleton
class ShowSnackBarUseCase internal constructor() {

    private val _snackBarStateFlow = MutableStateFlow(State())
    val observerSnackBarStatus: StateFlow<State> get() = _snackBarStateFlow.asStateFlow()

    suspend operator fun invoke(message: UIText) = _snackBarStateFlow.emit(State(message))

    data class State(val message: UIText? = null)
}
